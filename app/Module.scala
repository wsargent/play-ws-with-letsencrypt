import com.google.inject.AbstractModule
import javax.inject.{Inject, Provider}

import akka.actor.ActorSystem
import akka.stream.Materializer
import contexts._
import controllers.{CertificateDownloader, LetsEncryptWSClient}

import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}

/**
 * Set up a couple of typed execution contexts to make lookup easier
 */
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {

  override def configure() = {
    bind(classOf[WSExecutionContext]).toProvider(classOf[WSExecutionContextProvider])
    bind(classOf[DefaultExecutionContext]).toProvider(classOf[DefaultExecutionContextProvider])
    bind(classOf[CertificateDownloader]).toProvider(classOf[CertificateDownloaderProvider])
    bind(classOf[LetsEncryptWSClient]).toProvider(classOf[LetsEncryptWSClientProvider])
  }
}

class DefaultExecutionContextProvider @Inject() (actorSystem: ActorSystem)
  extends Provider[DefaultExecutionContext] {

  lazy val get: DefaultExecutionContext = {
    new DefaultExecutionContext(play.api.libs.concurrent.Execution.defaultContext)
  }

}

class WSExecutionContextProvider @Inject() (actorSystem: ActorSystem)
  extends Provider[WSExecutionContext] {

  lazy val get: WSExecutionContext = {
    new WSExecutionContext(actorSystem.dispatchers.lookup("dispatchers.ws"))
  }

}

class CertificateDownloaderProvider @Inject()(ws: WSClient,
                                              config:Configuration,
                                              wsExecutionContext: WSExecutionContext)
  extends Provider[CertificateDownloader] {

  lazy val get: CertificateDownloader = {
    new CertificateDownloader(ws, config)(wsExecutionContext)
  }

}

class LetsEncryptWSClientProvider @Inject()(lifecycle: ApplicationLifecycle,
                                            env: Environment)
                                           (implicit mat: Materializer, wsEc: WSExecutionContext)
  extends Provider[LetsEncryptWSClient] {

  override lazy val get: LetsEncryptWSClient = new LetsEncryptWSClient(lifecycle, env)

}