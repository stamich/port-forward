package io.codeswarm.portforward.streaming

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Flow
import io.codeswarm.portforward.model.ForwardingConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Await, ExecutionContextExecutor}

class ConnectionHandlerSpec extends AnyFlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  override def afterAll(): Unit = {
    system.whenTerminated.foreach(_ => ())
    Thread.sleep(1000)
  }

  "ConnectionHandler" should "create a valid flow" in {
    val config = ForwardingConfig(
      localHost = "localhost",
      localPort = 8080,
      remoteHost = "example.com",
      remotePort = 80
    )

    val handler = new ConnectionHandler(config)
    val flow = handler.createForwardingFlow()

    flow shouldBe a[Flow[_, _, _]]
  }
}
