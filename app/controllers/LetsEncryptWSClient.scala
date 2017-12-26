package controllers

import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import play.api.Environment
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.ahc.AhcWSClient
import play.api.libs.ws.{WSClient, WSRequest}
import play.libs.ws.ahc.AhcWSClientConfigFactory

/**
 * A WS client set up with downloaded certificates.
 *
 * This has a different than the base WS client, because it has to be loaded
 * and configured only after the certificates have been downloaded.
 */
class LetsEncryptWSClient(lifecycle: ApplicationLifecycle,
                          env: Environment)
                         (implicit mat: Materializer) extends WSClient {

  private val propsConfig = ConfigFactory.systemProperties()

  private val wsConfig = ConfigFactory.load(propsConfig.withFallback(ConfigFactory.parseString(
    """
      |play.ws {
      |  ssl {
      |    trustManager = {
      |      stores = [
      |        # Seems to be required for https://helloworld.letsencrypt.com
      |        { type = "PEM", path = "./conf/dst-x3-root.pem" }
      |        { type = "PEM", path = "./conf/letsencrypt-authority-x1.pem" }
      |      ]
      |    }
      |  }
      |}
    """.stripMargin)))

  private lazy val client = {
    val config = AhcWSClientConfigFactory.forConfig(wsConfig, env.classLoader)
    val client = AhcWSClient(config)
    lifecycle.addStopHook { () =>
      import scala.concurrent._
      import ExecutionContext.Implicits.global
      Future {
        blocking {
          client.close()
        }
      }
    }
    client
  }

  override def underlying[T]: T = client.underlying[T]

  override def url(url: String): WSRequest = client.url(url)

  override def close(): Unit = client.close()
}

