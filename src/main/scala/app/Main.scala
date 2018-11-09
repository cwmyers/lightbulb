package app

import java.nio.charset.StandardCharsets
import java.time.Instant

import app.tuya.TuyaApi._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttConnectOptions, MqttMessage}

import scala.util.{Failure, Success, Try}

object Main {

  def main(args: Array[String]): Unit = {

    val deviceId = sys.env.getOrElse("DEV_ID","042000665ccf7f7bc4da")
    val lightbulbTopicOut = s"smart/device/out/$deviceId"
    val lightbulbTopicIn = s"smart/device/in/$deviceId"
    val lightbridgeTopic = s"lightbridge/$deviceId"
    val commandTopic = s"$lightbridgeTopic/command"
//    val statusTopic = s"$lightbridgeTopic/status"

    val localKey = sys.env.getOrElse("LOCAL_KEY", "XXXX")
    val clientId = sys.env.getOrElse("CLIENT_ID", s"Light $deviceId")

    Try {
      val mqttClient: MqttClient = createClient(clientId)

      mqttClient.subscribe(lightbulbTopicOut, 2, (_: String, message: MqttMessage) => {
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



  def createTuyaMessage(deviceId: String, payload: String): String = {
    val timestamp = Instant.now.getEpochSecond
    val sequence = 0
    s"""{"data":{"devId":"$deviceId","dps":$payload},"protocol":5,"s":$sequence,"t":$timestamp}"""
  }

}
