package controllers

import java.net.URL
import javax.inject._

import contexts._
import play.api.libs.ws.ssl.CompositeCertificateException
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}

import scala.concurrent.Future
import scala.util.control.NonFatal

case class UrlData(url: String)

@Singleton
class HomeController @Inject()(downloader: CertificateDownloader,
                               lcWsClient: LetsEncryptWSClient)
                              (implicit val messagesApi: MessagesApi,
                               wsExecutionContext: WSExecutionContext) extends Controller with I18nSupport {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val form = Form(
    mapping = mapping(
      "url" -> nonEmptyText.verifying { urlString =>
        try {
          new URL(urlString)
          true
        } catch {
          case NonFatal(e) =>
            false
        }
      }
    )(UrlData.apply)(UrlData.unapply)
  )

  val defaultFormData = form.fill(UrlData("https://helloworld.letsencrypt.org/"))

  def index = Action.async { implicit request =>
    // Download the LC certificates if necessary
    val downloadFuture = if (downloader.allCertificatesExist()) {
      Future.successful(())
    } else {
      downloader.downloadCertificates()
    }

    downloadFuture.map { _ =>
      Ok(views.html.index(defaultFormData))
    }
  }

  def submit = Action.async { implicit request =>
    // Call out to the client using the certificates we just downloaded.
    val result: Future[Result] = form.bindFromRequest.fold(
      formWithErrors => formFailure(formWithErrors),
      urlData => formSuccess(new URL(urlData.url))
    )
    result
  }

  private def formSuccess(url: URL)(implicit r: Request[_]): Future[Result] = {
    lcWsClient.url(url.toExternalForm).head().map { response =>
      val message = s"Connection to ${url.toExternalForm} is ${response.statusText}"
      Redirect(routes.HomeController.index()).flashing("info" -> message)
    }.recover {
      case e: java.net.ConnectException =>
        CompositeCertificateException.unwrap(e) { certException =>
          val msg = s"Certificate exception: ${certException.getMessage}"
          logger.error(msg, certException)
        }
        val msg = s"Cannot validate certificate for $url, e = ${e.getMessage}"
        Redirect(routes.HomeController.index()).flashing("error" -> msg)

      case NonFatal(e) =>
        val msg = s"General failure for $url, e = ${e.getMessage}"
        logger.error(msg, e)
        GatewayTimeout(msg)
    }
  }

  private def formFailure(formWithErrors: Form[UrlData])(implicit r: Request[_]): Future[Result] = {
    Future.successful {
      BadRequest(views.html.index(formWithErrors))
    }
  }
}
