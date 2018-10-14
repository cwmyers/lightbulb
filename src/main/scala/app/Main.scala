package app

import java.nio.charset.StandardCharsets
import java.util.Base64

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttConnectOptions, MqttMessage}

import scala.util.{Failure, Success, Try}

object Main {

  def main(args: Array[String]): Unit = {

    val lightbulbTopic = sys.env.getOrElse("TEMP_SENSOR_TOPIC", "smart/device/out/042000665ccf7f7bc4da")
    val localKey = "cb67818210934165"
    val thermoCurrentTempTopic = sys.env.getOrElse("THERMO_CURRENT_TEMP_TOPIC", "/kitchenThermostat/currentTemp")
    val thermoTargetTempTopic = sys.env.getOrElse("THERMO_TARGET_TEMP_TOPIC", "/kitchenThermostat/targetTemp")
    val thermoGetTargetTempTopic = sys.env.getOrElse("THERMO_GET_TARGET_TEMP_TOPIC", "/kitchenThermostat/getTargetTemp")
    val currentHeatingCoolingTopic = sys.env.getOrElse("CURRENT_HEATING_COOLING_TOPIC", "/kitchenThermostat/currentHeatingCoolingState")
    val clientId = sys.env.getOrElse("CLIENT_ID", "Kitchen Thermostat")
    val heaterTopics: List[String] = sys.env.get("HEATER_TOPICS").map(_.split(",").toList).getOrElse(List.empty)

    println(s"Heater Topics $heaterTopics")
    println(s"TEMP_SENSOR_TOPIC: $lightbulbTopic")
    println(s"THERMO_CURRENT_TEMP_TOPIC: $thermoCurrentTempTopic")
    println(s"THERMO_TARGET_TEMP_TOPIC: $thermoTargetTempTopic")
    println(s"THERMO_GET_TARGET_TEMP_TOPIC: $thermoGetTargetTempTopic")
    println(s"CURRENT_HEATING_COOLING_TOPIC: $currentHeatingCoolingTopic")


    val broker = "tcp://192.168.1.198:1883"
    val persistence: MemoryPersistence = new MemoryPersistence()

    Try {
      val mqttClient = new MqttClient(broker, clientId, persistence)
      val connOpts = new MqttConnectOptions()
      connOpts.setCleanSession(true)
      connOpts.setAutomaticReconnect(true)
      connOpts.setConnectionTimeout(0)
      println("Connecting to broker: " + broker)
      mqttClient.connect(connOpts)
      println("Connected")

      mqttClient.subscribe(lightbulbTopic, 2, (topic: String, message: MqttMessage) => {
        val currentState = new String(message.getPayload, StandardCharsets.UTF_8)

        val bytes = Base64.getDecoder.decode(message.getPayload)

        decrypt(localKey, bytes)

        println(currentState)
      }
      )

      while (true) {
        Thread.sleep(60 * 1000)
      }
      mqttClient.disconnect()
      println("Disconnected")
    } match {
      case Success(_) => println("all good")
      case Failure(e) => println(e.getMessage)
    }

    // def publishState(client: MqttClient, currentTemp: Float, targetTemp: Float, mode: Mode): Unit = {
    //   client.publish(thermoCurrentTempTopic, createMessage(currentTemp.toString))
    //   client.publish(currentHeatingCoolingTopic, createMessage(mode.asNumber))
    //   heaterTopics.foreach{ topic => client.publish(topic, createMessage(mode.asValue))}
    //   println(s"currentTemp = $currentTemp targetTemp = $targetTemp heating = $mode")
    // }

  }

  def createMessage(message: String): MqttMessage = {
    val m = new MqttMessage(message.getBytes)
    m.setRetained(true)
    m
  }


  import javax.crypto.Cipher
  import javax.crypto.spec.SecretKeySpec

  def process(localKey: String, message:String): String = {
    val payload = message.substring(3+16)
    val bytes = Base64.getDecoder.decode(payload.getBytes)
    decrypt(localKey, bytes)

  }

  @throws[Exception]
  def encrypt(secretKey: String, data: String): String = {
    val key = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES")
    val c = Cipher.getInstance("AES")
    c.init(Cipher.ENCRYPT_MODE, key)
    val encVal = c.doFinal(data.getBytes)
    new String(Base64.getEncoder.encode(encVal))
  }

  def decrypt(secretKey: String, data: Array[Byte]): String = {
    val key = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES")
    val c = Cipher.getInstance("AES")
    c.init(Cipher.DECRYPT_MODE, key)
    val decVal: Array[Byte] = c.doFinal(data)

    new String(decVal, StandardCharsets.UTF_8)
  }

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


  def digest(message: String) = {
    import java.security.MessageDigest
    val e = MessageDigest.getInstance("MD5")
    e.update(message.getBytes)
    val tmp = e.digest()
    byte2hex(tmp)

  }

  def buildDataToHash(data: String, key: String): String =
    s"data=$data||pv=2.1||$key"

  def buildPayload(encrypted: String, md5Hash: String): String = {
    s"2.1${sliceHash(md5Hash)}$encrypted"
  }

  def sliceHash(md5Hash: String): String = md5Hash.slice(8, 24)

  def encryptMessage(message: String, localKey: String): String = {
    val encryptedMessage = encrypt(localKey, message)
    val md5 = digest(buildDataToHash(encryptedMessage, localKey))
    buildPayload(encryptedMessage, md5)
  }

  import java.security.MessageDigest

  @throws[Exception]
  def getMD5(source: Array[Byte]): String = {

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

  }

}

case class State(currentTemp: Float, targetTemp: Float)

sealed trait Mode {
  def asNumber: String

  def asValue: String
}

case object On extends Mode {
  def asNumber = "1"

  def asValue = "ON"
}

case object Off extends Mode {
  def asNumber = "0"

  def asValue = "OFF"
}
