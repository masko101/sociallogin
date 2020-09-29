package masko101.sociallogin.apimodel

import cats.effect.IO
import org.http4s.circe._
import io.circe.generic.auto._

object CirceEncodersDecoders {
  implicit val credentialsDecoder = jsonOf[IO, Credentials]
  implicit val secretCreateDecoder = jsonOf[IO, SecretCreate]
  implicit val secretDecoder = jsonOf[IO, Secret]
  implicit val authTokensDecoder = jsonOf[IO, AuthTokens]

  implicit val secretEncoder = jsonEncoderOf[IO, Secret]
}
