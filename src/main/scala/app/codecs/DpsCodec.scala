package app.codecs

import app.model.Dps
import io.circe.Decoder

trait DpsCodec {
  implicit val dpsDecoder: Decoder[Dps] = Decoder.forProduct3("1", "3", "5")(Dps.apply)

}
