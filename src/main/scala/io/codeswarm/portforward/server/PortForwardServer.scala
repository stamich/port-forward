package io.codeswarm.portforward.server

import akka.{Done, actor}
import akka.actor.typed.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Tcp}
import io.codeswarm.portforward.model.ForwardingConfig
import io.codeswarm.portforward.streaming.ConnectionHandler

import scala.concurrent.{ExecutionContext, Future}

class PortForwardServer(config: ForwardingConfig)(implicit system: ActorSystem[_]) {

  implicit val classicSystem: actor.ActorSystem = system.classicSystem
  implicit val materializer: Materializer = Materializer(system)

  private var bindingFuture: Option[Future[Tcp.ServerBinding]] = None

  def start()(implicit ec: ExecutionContext): Future[Done] = {
    println(s"Starting server on ${config.localHost}:${config.localPort}")

    val connectionHandler = new ConnectionHandler(config)

    val serverFlow = Tcp().bind(config.localHost, config.localPort)
      .to(Sink.foreach { connection =>
        println(s"New connection from: ${connection.remoteAddress}")

        val flow = connectionHandler.createForwardingFlow()
        connection.handleWith(flow)
      })

    val binding = serverFlow.run()
    bindingFuture = Some(binding)

    binding.map(_ => Done)
  }

  def stop()(implicit ec: ExecutionContext): Future[Done] = {
    println("Stopping server...")
    bindingFuture.map(_.flatMap(binding => binding.unbind().map(_ => Done)))
      .getOrElse(Future.successful(Done))
  }
}
