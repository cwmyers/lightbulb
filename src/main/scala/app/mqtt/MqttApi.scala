package app.mqtt

import app.tuya.Transport.HasErrorA
import cats.implicits._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttAsyncClient, MqttConnectOptions, MqttMessage}

object MqttApi {

  def createClient(clientId: String): Either[Throwable, MqttAsyncClient] =
    createClientF[Either[Throwable, ?]](clientId)

  def createClientF[F[_] : HasErrorA](clientId: String): F[MqttAsyncClient] =
    implicitly[HasErrorA[F]].catchNonFatal {
      val broker = "tcp://192.168.1.198:1883"
      val persistence: MemoryPersistence = new MemoryPersistence()
      val mqttClient = new MqttAsyncClient(broker, clientId, persistence)
      val connOpts = new MqttConnectOptions()
      connOpts.setCleanSession(true)
      connOpts.setAutomaticReconnect(true)
      connOpts.setConnectionTimeout(0)
      println("Connecting to broker: " + broker)
      mqttClient.connect(connOpts)
      println("Connected")
      mqttClient
    }

  def createMessage(message: String, retained: Boolean = true): Either[Throwable, MqttMessage] =
    createMessageF[Either[Throwable, ?]](message, retained)

  def createMessageF[F[_] : HasErrorA](message: String, retained: Boolean = true): F[MqttMessage] =
    implicitly[HasErrorA[F]].catchNonFatal {
      val m = new MqttMessage(message.getBytes)
      m.setRetained(true)
      m
    }


}
