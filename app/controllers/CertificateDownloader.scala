package controllers

import java.nio.file.{FileSystems, Files, Path, StandardOpenOption}

import contexts.WSExecutionContext
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Downloads the Letsencrypt certificates using the main WS client.
 *
 * This shouldn't require a custom cert, but it looks like JDK 1.8.0_b77 doesn't
 * doesn't have the DST X3 root either, so it has to be added in manually
 * so that we can download it.
 *
 * https://www.identrust.com/certificates/trustid/root-download-x3.html
 */
class CertificateDownloader(ws: WSClient, config:Configuration)(implicit wsExecutionContext: WSExecutionContext) {
  import contexts._

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val letsEncryptRootUrl = config.getString("letsencrypt.root.url").get
  private val letsEncryptRootPath = toPath(config.getString("letsencrypt.root.path").get)
  private val certMap = Map(letsEncryptRootUrl -> letsEncryptRootPath)

  def toPath(s:String) = {
    FileSystems.getDefault().getPath(s)
  }

  def certificatesExist() = {
    certMap.exists {
      case (k, v) =>
        certificateExists(v)
    }
  }

  def certificateExists(path: Path): Boolean = {
    Files.exists(path)
  }

  def downloadCertificates(): Future[Unit] = {
    Future.sequence(certMap.map { case (k, v) =>
      downloadCertificate(k, v)
    }).map(_ => ())
  }

  def downloadCertificate(certificateUrl: String, path: Path): Future[Path] = {
    logger.info(s"downloadCertificate: certificateUrl = $certificateUrl")
    val future = ws.url(certificateUrl).get().map { response =>
      response.status match {
        case 200 =>
          logger.info("Create file!")
          val body = response.bodyAsBytes
          Files.write(path, body.toArray, StandardOpenOption.CREATE_NEW)
        case other =>
          throw new IllegalStateException(s"Cannot download certificate!, status = $other, statusText = ${response.statusText}")
      }
    }.recover {
      case NonFatal(e) =>
        logger.error("Cannot download certificate", e)
        throw e
    }
    future
  }

}
