package io.codeswarm.portforward.streaming

import akka.actor
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{BidiFlow, Flow, Source, Tcp}
import akka.util.ByteString
import io.codeswarm.portforward.model.ForwardingConfig

import scala.concurrent.ExecutionContextExecutor

class ConnectionHandler(config: ForwardingConfig)(implicit system: ActorSystem[_]) {

  implicit val classicSystem: actor.ActorSystem = system.classicSystem
  implicit val ec: ExecutionContextExecutor = system.executionContext

  def createForwardingFlow(): Flow[ByteString, ByteString, _] = {
    Flow.fromGraph {
      BidiFlow.fromFlows(
        Flow[ByteString].via(createOutboundConnection()),
        Flow[ByteString]
      ).join(Flow[ByteString].map(identity))
    }
  }

  private def createOutboundConnection(): Flow[ByteString, ByteString, _] = {
    Tcp().outgoingConnection(config.remoteHost, config.remotePort)
      .recoverWithRetries(1, {
        case ex =>
          system.log.error(s"Connection to ${config.remoteHost}:${config.remotePort} failed: ${ex.getMessage}")
          Source.empty.via(Flow[ByteString])
      })
  }
}
