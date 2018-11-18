package app.codecs

import app.model.{DeviceId, Dps, Lightbulb, LocalKey}
import io.circe.Decoder

trait Codecs {
  implicit val dpsDecoder: Decoder[Dps] = Decoder.forProduct3("1", "3", "5")(Dps.apply)

  implicit val lightbulb: Decoder[Lightbulb] = Decoder {c =>
    for {
      deviceId <- c.get[String]("deviceId")
      localKey <- c.get[String]("localKey")
    } yield Lightbulb(DeviceId(deviceId), LocalKey(localKey))
  }

}
