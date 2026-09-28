package io.codeswarm.portforward.runtime

import io.codeswarm.portforward.network.tcp.{TcpConnectionFlowFactory, TcpForwarder}
import io.codeswarm.portforward.service.ForwardingService
import org.apache.pekko.Done
import org.apache.pekko.actor.{ActorSystem, CoordinatedShutdown}
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Owns Apache Pekko infrastructure shared by CLI and GUI front ends.
 *
 * The runtime registers forwarding cleanup with `CoordinatedShutdown`, so
 * SIGTERM/JVM shutdown and explicit application shutdown follow the same
 * lifecycle path.
 *
 * @param actorSystem Pekko ActorSystem owned by this runtime.
 * @param forwardingService application forwarding service.
 */
final class ApplicationRuntime private (
    val actorSystem: ActorSystem,
    val forwardingService: ForwardingService
) {

  private val logger: Logger =
    LoggerFactory.getLogger(classOf[ApplicationRuntime])

  private implicit val executionContext: ExecutionContext =
    actorSystem.dispatcher

  /**
   * Runs Pekko coordinated shutdown.
   *
   * @return future completed after all registered shutdown phases finish.
   */
  def shutdown(): Future[Done] = {
    logger.info("Starting coordinated application shutdown")
    CoordinatedShutdown(actorSystem)
      .run(CoordinatedShutdown.UnknownReason)
  }
}

/**
 * Production runtime factory.
 */
object ApplicationRuntime {

  /**
   * Creates and wires the production runtime.
   *
   * @return initialized runtime; no listener is started yet.
   */
  def create(): ApplicationRuntime = {
    implicit val actorSystem: ActorSystem =
      ActorSystem("port-forward-system")

    implicit val materializer: Materializer =
      SystemMaterializer(actorSystem).materializer

    implicit val executionContext: ExecutionContext =
      actorSystem.dispatcher

    val flowFactory =
      new TcpConnectionFlowFactory()

    val forwarder =
      new TcpForwarder(flowFactory)

    val forwardingService =
      new ForwardingService(forwarder)

    val runtime =
      new ApplicationRuntime(
        actorSystem,
        forwardingService
      )

    registerShutdown(forwardingService)

    runtime
  }

  /**
   * Registers listener cleanup in Pekko's service-unbind shutdown phase.
   *
   * @param forwardingService service whose listener should be stopped.
   * @param actorSystem runtime actor system.
   * @param executionContext runtime execution context.
   */
  private def registerShutdown(
      forwardingService: ForwardingService
  )(
      implicit actorSystem: ActorSystem,
      executionContext: ExecutionContext
  ): Unit = {
    CoordinatedShutdown(actorSystem).addTask(
      CoordinatedShutdown.PhaseServiceUnbind,
      "port-forward-unbind"
    ) { () =>
      forwardingService.stop().recover { case _ => Done }
    }
  }
}
