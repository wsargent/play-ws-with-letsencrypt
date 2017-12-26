import com.google.inject.AbstractModule
import javax.inject.{Inject, Provider}

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.Config
import contexts._
import controllers.{CertificateDownloader, LetsEncryptWSClient}
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}

/**
 * Set up a downloader and an execution context for File IO work
 */
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {

  override def configure() = {
    bind(classOf[CertificateDownloader]).toProvider(classOf[CertificateDownloaderProvider])
    bind(classOf[LetsEncryptWSClient]).toProvider(classOf[LetsEncryptWSClientProvider])
  }
}

class FileIOExecutionContextProvider @Inject() (actorSystem: ActorSystem)
  extends Provider[FileIOExecutionContext] {

  lazy val get: FileIOExecutionContext = {
    new FileIOExecutionContext(actorSystem.dispatchers.lookup("dispatchers.ws"))
  }

}

class CertificateDownloaderProvider @Inject()(ws: WSClient,
                                              config:Config,
                                              ioExecutionContext: FileIOExecutionContext)
  extends Provider[CertificateDownloader] {

  lazy val get: CertificateDownloader = {
    new CertificateDownloader(ws, config, ioExecutionContext)
  }

}

class LetsEncryptWSClientProvider @Inject()(lifecycle: ApplicationLifecycle,
                                            env: Environment)
                                           (implicit mat: Materializer)
  extends Provider[LetsEncryptWSClient] {

  override lazy val get: LetsEncryptWSClient = new LetsEncryptWSClient(lifecycle, env)

}