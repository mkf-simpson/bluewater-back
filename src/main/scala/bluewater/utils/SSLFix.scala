package bluewater.utils

import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory

import cats.effect.{Resource, Sync}
import javax.net.ssl.{SSLContext, TrustManagerFactory}

object SSLFix {
  private val certPath = "letsencryptauthorityx3.pem"
  
  private def certAcquire[F[_]: Sync]: F[InputStream] = Sync[F].delay(Thread.currentThread().getContextClassLoader.getResourceAsStream(certPath))
  private def certRelease[F[_]: Sync]: InputStream => F[Unit] = (is: InputStream) => Sync[F].delay(is.close())  
  
  def getCert[F[_]: Sync]: Resource[F, InputStream] = Resource.make(certAcquire)(certRelease)
  
  def addCert[F[_]: Sync](inputStream: InputStream): F[SSLContext] = Sync[F].delay {
    val ca = CertificateFactory.getInstance("X.509").generateCertificate(inputStream)
    val ks = KeyStore.getInstance(KeyStore.getDefaultType)
    ks.load(null, null)
    ks.setCertificateEntry("1", ca)
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(ks)
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, tmf.getTrustManagers, null)
    ctx
  }
  
  def context[F[_]: Sync]: F[SSLContext] = getCert[F].use(addCert[F])
}
