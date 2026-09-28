package io.codeswarm.portforward.network.tcp

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString
import io.codeswarm.portforward.domain.Endpoint

/**
 * Creates the stream used for one accepted client connection.
 *
 * Akka Streams propagates backpressure in both directions, avoiding the need
 * for an unbounded user-space buffer.
 *
 * @param actorSystem Akka actor system used by the TCP extension.
 */
final class TcpConnectionFlowFactory(implicit actorSystem: ActorSystem) {

  /**
   * Creates a flow connecting client bytes with the configured target.
   *
   * @param target destination endpoint.
   * @return flow that forwards bytes to and from the target.
   */
  def create(target: Endpoint): Flow[ByteString, ByteString, NotUsed] =
    Tcp()
      .outgoingConnection(target.host, target.port)
      .mapMaterializedValue(_ => NotUsed)
}
