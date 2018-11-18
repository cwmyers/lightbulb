package app

import java.nio.charset.StandardCharsets

import app.Communication.{getStatusTopic, publishCommand, publishMessageToMqtt}
import app.Util.toHex
import app.codecs.Codecs
import app.model.{Lightbulb, Structure}
import app.mqtt.MqttApi.createMessageF
import app.tuya.Transport.processF
import cats.ApplicativeError.liftFromOption
import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttMessage}


trait Commands[F[_]] extends Codecs {


  def handleRgbMessage(l: Lightbulb, mqttClient: MqttClient)(implicit F:Sync[F]): (String, MqttMessage) => F[Unit] =
    (_: String, message: MqttMessage) => {
      val status = new String(message.getPayload)
      println(status)
      val colours = status.split(",")
      val r = toHex(colours(0))
      val g = toHex(colours(1))
      val b = toHex(colours(2))
      val command = s"""{"2":"colour","5":"$r$g${b}ffffffff"}"""
      publishCommand[F](l, mqttClient, command)
    }


  def handleOnMessages(lightbulb: Lightbulb, mqttClient: MqttClient)(implicit F:Sync[F]): (String, MqttMessage) => F[Unit] =
    (_: String, message: MqttMessage) => {
      val status = new String(message.getPayload)
      println(status)
      val command = s"""{"1":$status}"""
      publishCommand[F](lightbulb, mqttClient, command)
    }


  def handleUpdateMessage(l: Lightbulb, mqttClient: MqttClient)(implicit F:Sync[F]): (String, MqttMessage) => F[Unit] =
    (_: String, message: MqttMessage) => {
      val currentState = new String(message.getPayload, StandardCharsets.UTF_8)
      val statusTopic = getStatusTopic(l.deviceId)

      val maybeStructure = decodeMessage(l, currentState)

      val onStatus = for {
        structure <- maybeStructure
        status <- liftFromOption[F](structure.data.dps.status, new RuntimeException("Empty Status Value"))
        message <- createMessageF[F](status.toString.toLowerCase)
        _ <- publishMessageToMqtt[F](mqttClient, s"$statusTopic/on")(message)
      } yield ()

      val colourStatus = for {
        structure <- maybeStructure
        colour <- liftFromOption[F](structure.data.dps.colourValue, new RuntimeException("Empty Colour Value"))
        message <- processColour(colour)
        _ <- publishMessageToMqtt[F](mqttClient, s"$statusTopic/rgb")(message)
      } yield ()

      onStatus *> colourStatus
    }

  private def processColour(colourValue: String)(implicit F:Sync[F]): F[MqttMessage] = {
    val red = Integer.parseInt(colourValue.substring(0, 2), 16)
    val green = Integer.parseInt(colourValue.substring(2, 4), 16)
    val blue = Integer.parseInt(colourValue.substring(4, 6), 16)
    createMessageF[F](s"$red,$green,$blue")
  }

  private def decodeMessage(l: Lightbulb, currentState: String)(implicit F:Sync[F]): F[Structure] = {
    processF[F](l.localKey.value, currentState).flatMap {
      message =>
        Util.decodeF[F, Structure](message)
    }
  }



}
