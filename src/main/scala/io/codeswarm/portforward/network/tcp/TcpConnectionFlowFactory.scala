package io.codeswarm.portforward.network.tcp

import io.codeswarm.portforward.domain.Endpoint
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Flow, Tcp}
import org.apache.pekko.util.ByteString

/**
 * Creates one target-side Pekko Streams flow for each accepted TCP client.
 *
 * Pekko Streams propagates backpressure between both sockets, which avoids an
 * unbounded application-level byte buffer.
 *
 * @param actorSystem Pekko actor system used by the TCP extension.
 */
final class TcpConnectionFlowFactory(implicit actorSystem: ActorSystem) {

  /**
   * Creates a byte flow connected to the configured target endpoint.
   *
   * @param target remote destination.
   * @return flow forwarding bytes to and from the target.
   */
  def create(target: Endpoint): Flow[ByteString, ByteString, NotUsed] =
    Tcp()
      .outgoingConnection(target.host, target.port)
      .mapMaterializedValue(_ => NotUsed)
}
