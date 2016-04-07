import com.google.inject.AbstractModule
import javax.inject.{Inject, Provider}

import akka.actor.ActorSystem
import contexts._
import play.api.{Configuration, Environment}

/**
 * Set up a couple of typed execution contexts to make lookup easier
 */
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {

  override def configure() = {
    bind(classOf[WSExecutionContext]).toProvider(classOf[WSExecutionContextProvider])
    bind(classOf[DefaultExecutionContext]).toProvider(classOf[DefaultExecutionContextProvider])
  }
}

class DefaultExecutionContextProvider @Inject() (actorSystem: ActorSystem) extends Provider[DefaultExecutionContext] {
  lazy val get: DefaultExecutionContext = new DefaultExecutionContext(play.api.libs.concurrent.Execution.defaultContext)
}

class WSExecutionContextProvider @Inject() (actorSystem: ActorSystem) extends Provider[WSExecutionContext] {
  lazy val get: WSExecutionContext = new WSExecutionContext(actorSystem.dispatchers.lookup("dispatchers.ws"))
}