package app.tuya

import java.nio.charset.StandardCharsets
import java.util.Base64


import scala.util.Try

object TuyaApi {
  type ErrorOr[A] = Either[Throwable, A]

  val tuyaApiVersion = "2.1"
  private val hashLength = 16


  import javax.crypto.Cipher
  import javax.crypto.spec.SecretKeySpec

  def process(localKey: String, message: String): ErrorOr[String] = for {
    payload <- Try {
      message.substring(TuyaApi.tuyaApiVersion.length + hashLength)
    }.toEither
    bytes <- Try {
      Base64.getDecoder.decode(payload.getBytes)
    }.toEither
    payload <- decrypt(localKey, bytes)
  } yield payload

  def encrypt(secretKey: String, data: String): ErrorOr[String] = Try {
    val key = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES")
    val c = Cipher.getInstance("AES")
    c.init(Cipher.ENCRYPT_MODE, key)
    val encVal = c.doFinal(data.getBytes)
    new String(Base64.getEncoder.encode(encVal))
  }.toEither

  def decrypt(secretKey: String, data: Array[Byte]): ErrorOr[String] = Try {
    val key = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES")
    val c = Cipher.getInstance("AES")
    c.init(Cipher.DECRYPT_MODE, key)
    val decVal: Array[Byte] = c.doFinal(data)

    new String(decVal, StandardCharsets.UTF_8)
  }.toEither

  def byte2hex(b: Array[Byte]): String = {
    var hs = ""
    var stmp = ""
    var n = 0
    while ( {
      n < b.length
    }) {
      stmp = Integer.toHexString(b(n) & 255)
      if (stmp.length == 1) hs = hs + "0" + stmp
      else hs = hs + stmp

      {
        n += 1
        n
      }
    }
    hs.toUpperCase
  }


  def digest(message: String): ErrorOr[String] = Try {
    import java.security.MessageDigest
    val e = MessageDigest.getInstance("MD5")
    e.update(message.getBytes)
    byte2hex(e.digest())
  }.toEither

  def buildDataToHash(data: String, key: String): String =
    s"data=$data||pv=2.1||$key"

  def buildPayload(encrypted: String, md5Hash: String): String = {
    s"2.1${sliceHash(md5Hash)}$encrypted"
  }

  def sliceHash(md5Hash: String): String = md5Hash.slice(8, 8 + hashLength)

  def encryptMessage(message: String, localKey: String): ErrorOr[String] =
    for {
      encryptedMessage <- encrypt(localKey, message)
      md5 <- digest(buildDataToHash(encryptedMessage, localKey))
    } yield buildPayload(encryptedMessage, md5.toLowerCase)

}
