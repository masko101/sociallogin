package masko101.sociallogin.apimodel

import cats.effect.IO
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.{EntityDecoder, EntityEncoder}

object CirceEncodersDecoders {
  implicit val credentialsDecoder: EntityDecoder[IO, Credentials] = jsonOf[IO, Credentials]
  implicit val secretCreateDecoder: EntityDecoder[IO, SecretCreate] = jsonOf[IO, SecretCreate]
  implicit val secretDecoder: EntityDecoder[IO, Secret] = jsonOf[IO, Secret]
  implicit val authTokensDecoder: EntityDecoder[IO, AuthTokens] = jsonOf[IO, AuthTokens]
  implicit val sharedSecretDecoder: EntityDecoder[IO, SharedSecret] = jsonOf[IO, SharedSecret]
  implicit val shareSecretDecoder: EntityDecoder[IO, ShareSecret] = jsonOf[IO, ShareSecret]

  implicit val secretEncoder: EntityEncoder[IO, Secret] = jsonEncoderOf[IO, Secret]
  implicit val sharedSecretEncoder: EntityEncoder[IO, SharedSecret] = jsonEncoderOf[IO, SharedSecret]
}
