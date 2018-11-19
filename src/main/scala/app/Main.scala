package app

import app.Communication.getCommandTopic
import app.Util.{decodeF, unsafeConvert}
import app.codecs.Codecs
import app.model._
import app.mqtt.MqttApi.createClientF
import cats.effect.{IO, Sync}
import cats.implicits._
import io.circe.generic.auto._
import org.eclipse.paho.client.mqttv3.MqttClient

import scala.io.Source

object Main extends Codecs {

  def main(args: Array[String]): Unit = {

    val clientId = sys.env.getOrElse("CLIENT_ID", s"Lightbridge")
    type Eff[A] = IO[A]

    val program = for {
      jsonInput <- Sync[Eff].delay(Source.fromFile("config.json").mkString)
      config <- decodeF[Eff, Config](jsonInput)
      mqttClient <- createClientF[Eff](clientId)
      _ <- config.lightbulbs.traverse[Eff, Unit] { l => wireUpListeners(l, mqttClient) }
    } yield ()

    try {
      program.unsafeRunSync()
    } catch {
      case e: Throwable => println(e.getMessage)
    }

  }

  def wireUpListeners(l: Lightbulb, mqttClient: MqttClient): IO[Unit] = {
    object Commands extends Commands[IO]

    import Commands._

    IO.delay(mqttClient.subscribe(getSmartOutTopic(l.deviceId), 2, unsafeConvert(handleUpdateMessage(l, mqttClient)))) *>
      IO.delay(mqttClient.subscribe(s"${getCommandTopic(l.deviceId)}/on", 2, unsafeConvert(handleOnMessages(l, mqttClient)))) *>
      IO.delay(mqttClient.subscribe(s"${getCommandTopic(l.deviceId)}/rgb", 2, unsafeConvert(handleRgbMessage(l, mqttClient))))
  }


  def getLightbulbTopicIn(deviceId: DeviceId): String = {
    s"smart/device/in/${deviceId.value}"
  }

  def getSmartOutTopic(deviceId: DeviceId): String = {
    s"smart/device/out/${deviceId.value}"
  }


}
