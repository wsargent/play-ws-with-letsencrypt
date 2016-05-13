package controllers

import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient, AhcWSClientConfig}
import play.api.libs.ws.{WSClient, WSConfigParser, WSRequest}
import play.api.{Configuration, Environment}

import scala.concurrent.Future

import contexts._

/**
 * A WS client set up with downloaded certificates.
 *
 * This has a different than the base WS client, because it has to be loaded
 * and configured only after the certificates have been downloaded.
 */
class LetsEncryptWSClient(lifecycle: ApplicationLifecycle,
                          env: Environment)(implicit mat: Materializer, wsEc: WSExecutionContext) extends WSClient {

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
    val configuration = Configuration.reference ++ Configuration(wsConfig)
    val parser = new WSConfigParser(configuration, env)
    val config = new AhcWSClientConfig(wsClientConfig = parser.parse())
    val builder = new AhcConfigBuilder(config)
    val client = AhcWSClient(builder.build())
    lifecycle.addStopHook { () =>
      Future(client.close())
    }
    client
  }

  override def underlying[T]: T = client.underlying[T]

  override def url(url: String): WSRequest = client.url(url)

  override def close(): Unit = client.close()
}

