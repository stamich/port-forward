package io.codeswarm.portforward.runtime

import akka.actor.ActorSystem
import akka.stream.{Materializer, SystemMaterializer}
import io.codeswarm.portforward.network.tcp.{TcpConnectionFlowFactory, TcpForwarder}
import io.codeswarm.portforward.service.ForwardingService

import scala.concurrent.{ExecutionContext, Future}

/**
 * Owns infrastructure resources shared by CLI and GUI front ends.
 *
 * Centralizing ActorSystem creation avoids duplicated lifecycle code and keeps
 * presentation classes focused on user interaction.
 *
 * @param actorSystem Akka actor system owned by this runtime.
 */
final class ApplicationRuntime private (
    val actorSystem: ActorSystem,
    val forwardingService: ForwardingService
) {

  private implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  /**
   * Stops forwarding and then terminates the actor system.
   *
   * @return future completed when runtime resources are terminated.
   */
  def shutdown(): Future[Unit] =
    forwardingService
      .stop()
      .recover { case _ => akka.Done }
      .flatMap(_ => actorSystem.terminate())
      .map(_ => ())
}

/**
 * Factory for fully wired application runtimes.
 */
object ApplicationRuntime {

  /**
   * Creates the production runtime used by CLI and GUI applications.
   *
   * @return initialized runtime; the TCP listener is not started yet.
   */
  def create(): ApplicationRuntime = {
    implicit val actorSystem: ActorSystem = ActorSystem("port-forward-system")
    implicit val materializer: Materializer = SystemMaterializer(actorSystem).materializer
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    val flowFactory = new TcpConnectionFlowFactory()
    val forwarder = new TcpForwarder(flowFactory)
    val service = new ForwardingService(forwarder)

    new ApplicationRuntime(actorSystem, service)
  }
}
