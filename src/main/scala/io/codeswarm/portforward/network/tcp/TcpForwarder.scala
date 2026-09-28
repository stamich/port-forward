package io.codeswarm.portforward.network.tcp

import akka.Done
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Tcp}
import io.codeswarm.portforward.domain.ForwardingStatus._
import io.codeswarm.portforward.domain.{ForwardingConfig, ForwardingStatus}
import io.codeswarm.portforward.network.Forwarder

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Akka Streams implementation of a single-rule TCP forwarder.
 *
 * The class owns only the listener lifecycle. Per-connection byte forwarding
 * is delegated to [[TcpConnectionFlowFactory]].
 *
 * @param flowFactory factory creating streams for target connections.
 * @param actorSystem Akka actor system used by the networking layer.
 * @param materializer Akka Streams materializer.
 * @param executionContext execution context used for asynchronous callbacks.
 */
final class TcpForwarder(
    flowFactory: TcpConnectionFlowFactory
)(
    implicit actorSystem: ActorSystem,
    materializer: Materializer,
    executionContext: ExecutionContext
) extends Forwarder {

  private val logger: LoggingAdapter = actorSystem.log
  private val state = new AtomicReference[ForwardingStatus](Stopped)
  private val binding = new AtomicReference[Option[Tcp.ServerBinding]](None)

  /**
   * Starts the TCP listener.
   *
   * Calling this method when the forwarder is already active fails fast.
   *
   * @param config validated forwarding rule.
   * @return future completed after successful socket binding.
   */
  override def start(config: ForwardingConfig): Future[Done] = synchronized {
    state.get() match {
      case Stopped =>
        state.set(Starting)
        logger.info("Starting TCP forwarding from {} to {}", config.listen.toString, config.target.toString)

        val bindingFuture =
          Tcp()
            .bind(config.listen.host, config.listen.port)
            .to(Sink.foreach { connection =>
              logger.info(
                "Accepted connection from {} for target {}",
                connection.remoteAddress.toString,
                config.target.toString
              )

              connection.handleWith(flowFactory.create(config.target))
            })
            .run()

        bindingFuture.transformWith {
          case Success(serverBinding) =>
            binding.set(Some(serverBinding))
            state.set(Running)
            logger.info("TCP listener bound to {}", serverBinding.localAddress.toString)
            Future.successful(Done)

          case Failure(ex) =>
            state.set(Stopped)
            logger.error(ex, "Unable to start TCP forwarder")
            Future.failed(ex)
        }

      case current =>
        Future.failed(new IllegalStateException(s"Cannot start forwarder while state is $current"))
    }
  }

  /**
   * Unbinds the TCP listener.
   *
   * Calling this method when the forwarder is already stopped is idempotent.
   *
   * @return future completed once the listener is no longer accepting clients.
   */
  override def stop(): Future[Done] = synchronized {
    binding.get() match {
      case Some(serverBinding) =>
        state.set(Stopping)
        logger.info("Stopping TCP listener {}", serverBinding.localAddress.toString)

        serverBinding
          .unbind()
          .map { _ =>
            binding.set(None)
            state.set(Stopped)
            logger.info("TCP forwarder stopped")
            Done
          }
          .recoverWith { case ex =>
            state.set(Stopped)
            binding.set(None)
            Future.failed(ex)
          }

      case None =>
        state.set(Stopped)
        Future.successful(Done)
    }
  }

  /**
   * Returns the current lifecycle state.
   *
   * @return forwarding state.
   */
  override def status: ForwardingStatus = state.get()
}
