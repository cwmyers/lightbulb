package app

import app.tuya.Transport.HasErrorA
import cats.ApplicativeError
import cats.effect.{IO, Sync}
import io.circe.Decoder
import io.circe.parser.decode
import org.eclipse.paho.client.mqttv3.{IMqttMessageListener, MqttMessage}

object Util {
  def toHex(s: String) = f"${s.toInt}%02x"

  def unsafeConvert(f: (String, MqttMessage) => IO[Unit]): IMqttMessageListener =
    (topic: String, message: MqttMessage) => {
      try {
        f(topic, message).unsafeRunSync()
      } catch {
        case e: Throwable => println(e)
      }
    }


  def decodeF[F[_] : HasErrorA, A: Decoder](message: String): F[A] =
    ApplicativeError[F, Throwable].fromEither(decode[A](message))

  def logF[F[_] : Sync](message: String): F[Unit] = Sync[F].delay(println(message))

  implicit class IOUtils[F[_] : Sync](io: F[Unit]) {
    def recover: F[Unit] = Sync[F].recoverWith(io) {
      case e => logF(e.getMessage)
    }
  }

}
