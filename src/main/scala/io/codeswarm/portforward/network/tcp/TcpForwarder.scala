package io.codeswarm.portforward.network.tcp

import io.codeswarm.portforward.domain.{ForwardingConfig, ForwardingStatus}
import io.codeswarm.portforward.domain.ForwardingStatus.{Running, Starting, Stopped, Stopping}
import io.codeswarm.portforward.error.{BindFailedException, ForwarderStateException}
import io.codeswarm.portforward.network.Forwarder
import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Tcp}
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Apache Pekko Streams implementation of a single-rule TCP forwarder.
 *
 * The complete mutable lifecycle is represented by one private state value,
 * preventing inconsistent combinations such as `Running` without a binding.
 *
 * @param flowFactory factory creating one target flow per accepted client.
 * @param actorSystem Pekko actor system used by the TCP extension.
 * @param materializer stream materializer.
 * @param executionContext execution context for asynchronous callbacks.
 */
final class TcpForwarder(
    flowFactory: TcpConnectionFlowFactory
)(
    implicit actorSystem: ActorSystem,
    materializer: Materializer,
    executionContext: ExecutionContext
) extends Forwarder {

  private val logger: Logger =
    LoggerFactory.getLogger(classOf[TcpForwarder])

  private val connectionSequence = new AtomicLong(0L)

  private sealed trait State {
    def publicStatus: ForwardingStatus
  }

  private case object Idle extends State {
    override val publicStatus: ForwardingStatus = Stopped
  }

  private final case class Binding(config: ForwardingConfig) extends State {
    override val publicStatus: ForwardingStatus = Starting
  }

  private final case class Bound(
      config: ForwardingConfig,
      serverBinding: Tcp.ServerBinding
  ) extends State {
    override val publicStatus: ForwardingStatus = Running
  }

  private final case class Unbinding(config: ForwardingConfig) extends State {
    override val publicStatus: ForwardingStatus = Stopping
  }

  @volatile private var currentState: State = Idle

  /**
   * Starts the local TCP listener.
   *
   * A second start request while the forwarder is not stopped fails fast.
   * Bind failures restore the state to `Stopped`.
   *
   * @param config validated forwarding rule.
   * @return future completed after the listener is bound.
   */
  override def start(config: ForwardingConfig): Future[Done] = synchronized {
    currentState match {
      case Idle =>
        currentState = Binding(config)
        logger.info(
          "Starting TCP forwarding from {} to {}",
          config.listen,
          config.target
        )

        val bindingFuture =
          Tcp()
            .bind(config.listen.host, config.listen.port)
            .to(
              Sink.foreach { connection =>
                val connectionId =
                  connectionSequence.incrementAndGet()

                logger.info(
                  "connection={} event=accepted client={} target={}",
                  Long.box(connectionId),
                  connection.remoteAddress,
                  config.target
                )

                connection.handleWith(
                  flowFactory.create(config.target)
                )
              }
            )
            .run()

        bindingFuture.transformWith {
          case Success(serverBinding) =>
            synchronized {
              currentState match {
                case Binding(`config`) =>
                  currentState = Bound(config, serverBinding)
                  logger.info(
                    "TCP listener bound to {}",
                    serverBinding.localAddress
                  )
                  Future.successful(Done)

                case other =>
                  serverBinding.unbind()
                  Future.failed(
                    ForwarderStateException(
                      "complete start",
                      other.publicStatus
                    )
                  )
              }
            }

          case Failure(ex) =>
            synchronized {
              currentState = Idle
            }
            logger.error(
              s"Unable to bind TCP listener to ${config.listen}",
              ex
            )
            Future.failed(BindFailedException(config.listen, ex))
        }

      case other =>
        Future.failed(
          ForwarderStateException("start", other.publicStatus)
        )
    }
  }

  /**
   * Stops accepting new connections by unbinding the listener.
   *
   * Existing per-connection streams are allowed to complete naturally.
   * Stopping an already stopped forwarder succeeds immediately.
   *
   * @return future completed after the listener is unbound.
   */
  override def stop(): Future[Done] = synchronized {
    currentState match {
      case Idle =>
        Future.successful(Done)

      case Bound(config, serverBinding) =>
        currentState = Unbinding(config)

        logger.info(
          "Stopping TCP listener {}",
          serverBinding.localAddress
        )

        serverBinding.unbind().transformWith {
          case Success(_) =>
            synchronized {
              currentState = Idle
            }
            logger.info("TCP forwarder stopped")
            Future.successful(Done)

          case Failure(ex) =>
            synchronized {
              currentState = Idle
            }
            logger.error("Failed to unbind TCP listener", ex)
            Future.failed(ex)
        }

      case other =>
        Future.failed(
          ForwarderStateException("stop", other.publicStatus)
        )
    }
  }

  /**
   * Returns the public lifecycle state derived from the single internal state
   * object.
   *
   * @return current forwarding state.
   */
  override def status: ForwardingStatus =
    currentState.publicStatus
}
