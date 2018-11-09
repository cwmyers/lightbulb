package app

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttConnectOptions, MqttMessage}

import scala.util.{Failure, Success, Try}

object Main {
  val tuyaApiVersion = "2.1"
  type ErrorOr[A] = Either[Throwable, A]

  def main(args: Array[String]): Unit = {

    val deviceId = sys.env.getOrElse("DEV_ID","042000665ccf7f7bc4da")
    val lightbulbTopicOut = s"smart/device/out/$deviceId"
    val lightbulbTopicIn = s"smart/device/in/$deviceId"
    val lightbridgeTopic = s"lightbridge/$deviceId"
    val commandTopic = s"$lightbridgeTopic/command"
//    val statusTopic = s"$lightbridgeTopic/status"

    val localKey = sys.env.getOrElse("LOCAL_KEY", "XXXX")
    val clientId = sys.env.getOrElse("CLIENT_ID", s"Light $deviceId")
    println(s"TEMP_SENSOR_TOPIC: $lightbulbTopicOut")

    Try {
      val mqttClient: MqttClient = createClient(clientId)

      mqttClient.subscribe(lightbulbTopicOut, 2, (topic: String, message: MqttMessage) => {
        val currentState = new String(message.getPayload, StandardCharsets.UTF_8)

        process(localKey, currentState).foreach(println)
      }

      )

      mqttClient.subscribe(s"$commandTopic/on", 2, (topic: String, message: MqttMessage) => {
        val status = new String(message.getPayload)
        println(status)
        val command = s"""{"1":$status}"""
        publishCommand(mqttClient, command)
      })
      mqttClient.subscribe(s"$commandTopic/rgbw", 2, (topic: String, message: MqttMessage) => {
        val status = new String(message.getPayload)
        println(status)
        val colours = status.split(",")
        val r = toHex(colours(0))
        val g = toHex(colours(1))
        val b = toHex(colours(2))
        val w = toHex(colours(3))
        val command = s"""{"2":"colour","3":255,"4":255,"5":"${r}${g}${b}ffff$w$w"}"""
        println(command)
        publishCommand(mqttClient, command)
      })

      mqttClient.subscribe(s"$commandTopic/rgb", 2, (topic: String, message: MqttMessage) => {
        val status = new String(message.getPayload)
        println(status)
        val colours = status.split(",")
        val r = toHex(colours(0))
        val g = toHex(colours(1))
        val b = toHex(colours(2))
        val command = s"""{"2":"colour","5":"${r}${g}${b}ffffffff"}"""
        publishCommand(mqttClient, command)
      })

      mqttClient.subscribe(s"$commandTopic/hsl", 2, (topic: String, message: MqttMessage) => {
        val status = new String(message.getPayload)
        println(status)
        val hsl = status.split(",")
        val hue = hsl(0)
        val sat = hsl(1)
        val lum = hsl(2)
        val command = s"""{"2":"colour","5":"${hue.toInt.toHexString}${sat.toInt.toHexString}${lum.toInt.toHexString}0000${lum.toInt.toHexString}ff"}"""
        println(command)
        publishCommand(mqttClient, command)
      })

      while (true) {
        Thread.sleep(6 * 1000)

      }
      mqttClient.disconnect()
      println("Disconnected")
    } match {
      case Success(_) => println("all good")
      case Failure(e) => println(e.getMessage)
    }

    def publishCommand(client: MqttClient, message: String) = {
      readyToSend(deviceId, localKey, message).foreach(m =>
        client.publish(lightbulbTopicIn, createMessage(m,retained = false))
      )
    }

  }

  def toHex(s:String) = f"${s.toInt}%02x"

  def readyToSend(deviceId: String, localKey: String, command: String) = {
    encryptMessage(createTuyaMessage(deviceId, command), localKey)
  }

  def createClient(clientId: String) = {
    val broker = "tcp://192.168.1.198:1883"
    val persistence: MemoryPersistence = new MemoryPersistence()
    val mqttClient = new MqttClient(broker, clientId, persistence)
    val connOpts = new MqttConnectOptions()
    connOpts.setCleanSession(true)
    connOpts.setAutomaticReconnect(true)
    connOpts.setConnectionTimeout(0)
    println("Connecting to broker: " + broker)
    mqttClient.connect(connOpts)
    println("Connected")
    mqttClient
  }

  def createMessage(message: String, retained:Boolean=true): MqttMessage = {
    val m = new MqttMessage(message.getBytes)
    m.setRetained(true)
    m
  }


  import javax.crypto.Cipher
  import javax.crypto.spec.SecretKeySpec

  def process(localKey: String, message: String): ErrorOr[String] = {
    val payload = message.substring(tuyaApiVersion.length + 16)
    val bytes = Base64.getDecoder.decode(payload.getBytes)
    decrypt(localKey, bytes)
  }

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
        n += 1;
        n
      }
    }
    hs.toUpperCase
  }


  def digest(message: String): ErrorOr[String] = Try {
    import java.security.MessageDigest
    val e = MessageDigest.getInstance("MD5")
    e.update(message.getBytes)
    val tmp = e.digest()
    byte2hex(tmp)
  }.toEither

  def buildDataToHash(data: String, key: String): String =
    s"data=$data||pv=2.1||$key"

  def buildPayload(encrypted: String, md5Hash: String): String = {
    s"2.1${sliceHash(md5Hash)}$encrypted"
  }

  def sliceHash(md5Hash: String): String = md5Hash.slice(8, 24)

  def encryptMessage(message: String, localKey: String): ErrorOr[String] =
    for {
      encryptedMessage <- encrypt(localKey, message)
      md5 <- digest(buildDataToHash(encryptedMessage, localKey))
    } yield buildPayload(encryptedMessage, md5.toLowerCase)


  import java.security.MessageDigest

  def getMD5(source: Array[Byte]): ErrorOr[String] = Try {

    val hexDigits = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
    val e = MessageDigest.getInstance("MD5")
    e.update(source)
    val tmp = e.digest
    val str = new Array[Char](32)
    var k = 0
    var i = 0
    while ( {
      i < 16
    }) {
      val byte0 = tmp(i)
      str({
        k += 1;
        k - 1
      }) = hexDigits(byte0 >>> 4 & 15)
      str({
        k += 1;
        k - 1
      }) = hexDigits(byte0 & 15)

      {
        i += 1;
        i
      }
    }
    new String(str)

  }.toEither

  def createTuyaMessage(deviceId: String, payload: String): String = {
    val timestamp = Instant.now.getEpochSecond
    val sequence = 0
    s"""{"data":{"devId":"$deviceId","dps":$payload},"protocol":5,"s":$sequence,"t":$timestamp}"""
  }

}
