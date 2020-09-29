package masko101.sociallogin

import cats.effect.{IO, Sync}
import cats.implicits._
import masko101.sociallogin.apimodel.{Credentials, GeneralError, Secret, SecretCreate}
import masko101.sociallogin.services.{AuthenticationService, SecretService}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.dsl.Http4sDsl
import masko101.sociallogin.apimodel.CirceEncodersDecoders._
import masko101.sociallogin.model.{SecretCreateEntity, UserEntity}

object SocialLoginRoutes {

  def secretRoutes(secretService: SecretService): AuthedRoutes[UserEntity, IO] = {
    val dsl: Http4sDsl[IO] = new Http4sDsl[IO]{}
    import dsl._
    AuthedRoutes.of[UserEntity, IO] {
      case GET -> Root / "secrets" as user =>
        for {
          secrets <- secretService.getUserOwnedSecrets(user)
          resp <- Ok(secrets.map(s => Secret(s.id, s.ownerId, s.secretText)).asJson)
        } yield resp
      case GET -> Root / "secrets" / secretIdString as user =>
        try {
          val secretId = secretIdString.toLong
          for {
            secrets <- secretService.getUserOwnedSecret(user, secretId)
            resp <- secrets.map(s => Ok(Secret(s.id, s.ownerId, s.secretText))).getOrElse(NotFound())
          } yield resp
        } catch {
          case nfe: NumberFormatException =>
            BadRequest()
        }
      case authReq@POST -> Root / "secrets" as user =>
        for {
          secretCreate <- authReq.req.as[SecretCreate]
          secret <- secretService.createNewSecret(SecretCreateEntity(user.id, secretCreate.secretText))
          response <- Ok(Secret(secret.id, secret.ownerId, secret.secretText))
        } yield response
    }
  }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }

  def loginRoutes(authService: AuthenticationService): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO]{}
    import dsl._
    HttpRoutes.of[IO] {
      case req@POST -> Root / "login" =>
        for {
          credentials <- req.as[Credentials]
          authTokensOption <- authService.authenticate(credentials)
          response <- authTokensOption.map(a => Ok(a.asJson))
            .getOrElse(BadRequest(GeneralError(Some("Invalid Username or Password")).asJson))
        } yield {
          response
        }
    }
  }
}