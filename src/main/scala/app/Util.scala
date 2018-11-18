package app

import app.tuya.Transport.HasErrorA
import cats.ApplicativeError
import cats.effect.IO
import io.circe.Decoder
import io.circe.parser.decode
import org.eclipse.paho.client.mqttv3.{IMqttMessageListener, MqttMessage}

object Util {
  def toHex(s: String) = f"${s.toInt}%02x"

  def unsafeConvert(f: (String, MqttMessage) => IO[Unit]): IMqttMessageListener = {
    (topic: String, message: MqttMessage) => {
      f(topic, message).unsafeRunSync()
    }
  }

  def decodeF[F[_] : HasErrorA, A: Decoder](message: String): F[A] =
    ApplicativeError[F, Throwable].fromEither(decode[A](message))

}
