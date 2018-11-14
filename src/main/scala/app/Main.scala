package app

import app.Communication.getCommandTopic
import app.Util.unsafeConvert
import app.model._
import app.mqtt.MqttApi
import cats.effect.IO
import cats.implicits._

object Main {


  def main(args: Array[String]): Unit = {
    val lightbulbs = Vector[Lightbulb](
      Lightbulb(DeviceId("042000665ccf7f7bc4da"), LocalKey("34e68ce080dbdf1b"))
    )

    val clientId = sys.env.getOrElse("CLIENT_ID", s"Lightbridge")

    object Commands extends Commands[IO]

    import Commands._

    MqttApi.createClientF[IO](clientId).flatMap { mqttClient =>

      lightbulbs.traverse { l =>
        IO.delay(mqttClient.subscribe(getSmartOutTopic(l.deviceId), 2, unsafeConvert(handleUpdateMessage(l, mqttClient)))) *>
          IO.delay(mqttClient.subscribe(s"${getCommandTopic(l.deviceId)}/on", 2, unsafeConvert(handleOnMessages(l, mqttClient)))) *>
          IO.delay(mqttClient.subscribe(s"${getCommandTopic(l.deviceId)}/rgb", 2, unsafeConvert(handleRgbMessage(l, mqttClient))))
      }

    }.unsafeRunSync()

  }


  def getLightbulbTopicIn(deviceId: DeviceId): String = {
    s"smart/device/in/${deviceId.value}"
  }

  def getSmartOutTopic(deviceId: DeviceId): String = {
    s"smart/device/out/${deviceId.value}"
  }


}
