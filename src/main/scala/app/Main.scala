package app

import java.nio.charset.StandardCharsets

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttConnectOptions, MqttMessage}

import scala.util.{Failure, Success, Try}

object Main {

  def main(args: Array[String]): Unit = {

    val lightbulbTopic = sys.env.getOrElse("TEMP_SENSOR_TOPIC", "smart/device/out/042000665ccf7f7bc4da")
    val thermoCurrentTempTopic = sys.env.getOrElse("THERMO_CURRENT_TEMP_TOPIC", "/kitchenThermostat/currentTemp")
    val thermoTargetTempTopic = sys.env.getOrElse("THERMO_TARGET_TEMP_TOPIC", "/kitchenThermostat/targetTemp")
    val thermoGetTargetTempTopic = sys.env.getOrElse("THERMO_GET_TARGET_TEMP_TOPIC", "/kitchenThermostat/getTargetTemp")
    val currentHeatingCoolingTopic = sys.env.getOrElse("CURRENT_HEATING_COOLING_TOPIC", "/kitchenThermostat/currentHeatingCoolingState")
    val clientId = sys.env.getOrElse("CLIENT_ID", "Kitchen Thermostat")
    val heaterTopics:List[String] = sys.env.get("HEATER_TOPICS").map(_.split(",").toList).getOrElse(List.empty)

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

  def determineMode(currentTemp: Float, targetTemp: Float):Mode =
    if (currentTemp > targetTemp) Off else On


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
