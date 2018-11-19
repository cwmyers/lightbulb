package app.tuya

import java.nio.charset.StandardCharsets
import java.util.Base64

import app.model.LocalKey
import cats.implicits._
import cats.{ApplicativeError, MonadError}


object Transport {
  type ErrorOr[A] = Either[Throwable, A]
  type HasErrorM[F[_]] = MonadError[F, Throwable]
  type HasErrorA[F[_]] = ApplicativeError[F, Throwable]

  val tuyaApiVersion = "2.1"
  private val hashLength = 16


  import javax.crypto.Cipher
  import javax.crypto.spec.SecretKeySpec

  def process(localKey: String, message: String): ErrorOr[String] =
    processF[ErrorOr](localKey, message)

  def processF[F[_]](localKey: String, message: String)(implicit F: HasErrorM[F]): F[String] = for {
    payload <- F.catchNonFatal(message.substring(Transport.tuyaApiVersion.length + hashLength))
    bytes <- F.catchNonFatal(Base64.getDecoder.decode(payload.getBytes))
    payload <- decrypt[F](localKey, bytes)
  } yield payload

  def encrypt[F[_] : HasErrorA](secretKey: String, data: String): F[String] =
    implicitly[HasErrorA[F]].catchNonFatal {
      val key = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES")
      val c = Cipher.getInstance("AES")
      c.init(Cipher.ENCRYPT_MODE, key)
      val encVal = c.doFinal(data.getBytes)
      new String(Base64.getEncoder.encode(encVal))
    }

  def decrypt[F[_] : HasErrorA](secretKey: String, data: Array[Byte]): F[String] =
    implicitly[HasErrorA[F]].catchNonFatal {
      val key = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES")
      val c = Cipher.getInstance("AES")
      c.init(Cipher.DECRYPT_MODE, key)
      val decVal: Array[Byte] = c.doFinal(data)

      new String(decVal, StandardCharsets.UTF_8)
    }

  def byte2hex(b: Array[Byte]): String =
    b.map("%02X".format(_)).mkString.toUpperCase

  def digest[F[_] : HasErrorA](message: String): F[String] =
    implicitly[HasErrorA[F]].catchNonFatal {
      import java.security.MessageDigest
      val e = MessageDigest.getInstance("MD5")
      e.update(message.getBytes)
      byte2hex(e.digest())
    }

  def buildDataToHash(data: String, key: String): String =
    s"data=$data||pv=2.1||$key"

  def buildPayload(encrypted: String, md5Hash: String): String =
    s"2.1${sliceHash(md5Hash)}$encrypted"


  def sliceHash(md5Hash: String): String = md5Hash.slice(8, 8 + hashLength)

  def encryptMessage(message: String, localKey: LocalKey): ErrorOr[String] =
    encryptMessageF[ErrorOr](message, localKey)

  def encryptMessageF[F[_] : HasErrorM](message: String, localKey: LocalKey): F[String] =
    for {
      encryptedMessage <- encrypt[F](localKey.value, message)
      md5 <- digest[F](buildDataToHash(encryptedMessage, localKey.value))
    } yield buildPayload(encryptedMessage, md5.toLowerCase)

}
