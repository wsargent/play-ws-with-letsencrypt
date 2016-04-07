package controllers

import javax.inject._

import contexts._
import play.api.libs.ws.WSClient
import play.api.mvc._

@Singleton
class HomeController @Inject()(ws: WSClient)(implicit wsExecutionContext: WSExecutionContext) extends Controller {

  /**
   * Calls out to playframework.com and returns the status text.
   */
  def index = Action.async {
    ws.url("https://playframework.com").head().map { response =>
      val message = response.statusText
      Ok(views.html.index(message))
    }
  }

}
