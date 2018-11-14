package app

import app.tuya.Transport.HasError
import cats.MonadError
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

  def decodeF[F[_] : HasError, A: Decoder](message: String): F[A] =
    MonadError[F, Throwable].fromEither(decode[A](message))

}
