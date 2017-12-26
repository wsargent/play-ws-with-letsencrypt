package controllers

import java.net.URL
import java.nio.file.{FileSystems, Files, Path, StandardOpenOption}

import com.typesafe.config.{Config, ConfigObject}
import contexts.FileIOExecutionContext
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
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
class CertificateDownloader(ws: WSClient, config:Config, fileIOExecutionContext: FileIOExecutionContext) {
  private implicit val ec: ExecutionContext = fileIOExecutionContext.underlying

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val certMap: Map[URL, Path] = {
    import scala.collection.JavaConverters._
    val letsEncryptRootCertificates = config.getObjectList("letsencrypt.root.certificates")
    letsEncryptRootCertificates.asScala.map { certObj: ConfigObject =>
      val path = certObj.get("path").unwrapped().asInstanceOf[String]
      val url = certObj.get("url").unwrapped().asInstanceOf[String]
      new URL(url) -> toPath(path)
    }.toMap
  }

  def toPath(s:String): Path = {
    FileSystems.getDefault.getPath(s)
  }

  def allCertificatesExist(): Boolean = {
    certMap.forall {
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

  def downloadCertificate(certificateUrl: URL, path: Path): Future[Path] = {
    logger.info(s"downloadCertificate: certificateUrl = $certificateUrl")
    val future = ws.url(certificateUrl.toString).get().map { response =>
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
