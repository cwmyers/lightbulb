package app

import java.time.Instant

import app.Main._
import app.model.{DeviceId, Lightbulb}
import app.mqtt.MqttApi
import app.tuya.Protocol.createTuyaMessage
import app.tuya.Transport.encryptMessageF
import cats.effect.Sync
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttMessage}
import cats.implicits._

object Communication {
  def publishMessageToMqtt[F[_] : Sync](mqttClient: MqttClient, topic: String)(message: MqttMessage): F[Unit] = {
    Sync[F].delay(
      mqttClient.publish(topic, message)
    )
  }

  def getLightbridgeTopic(deviceId: DeviceId): String = {
    s"lightbridge/${deviceId.value}"
  }

  def getStatusTopic(deviceId: DeviceId): String = {
    s"${getLightbridgeTopic(deviceId)}/status"
  }

  def getCommandTopic(deviceId: DeviceId): String = {
    s"${getLightbridgeTopic(deviceId)}/command"
  }

  def publishCommand[F[_]: Sync](lightbulb: Lightbulb, client: MqttClient, message: String): F[Unit] = {
    for {
      m <- readyToSend[F](lightbulb, message)
      mqttMessage <- MqttApi.createMessageF[F](m, retained = false)
      _ <- publishMessageToMqtt[F](client, getLightbulbTopicIn(lightbulb.deviceId))(mqttMessage)
    } yield ()
  }

  def readyToSend[F[_] : Sync](lightbulb: Lightbulb, command: String): F[String] = {
    val timestamp = Sync[F].delay(Instant.now.getEpochSecond)
    timestamp.flatMap(t =>
      encryptMessageF[F](createTuyaMessage(lightbulb.deviceId, t, command), lightbulb.localKey)
    )
  }

}
