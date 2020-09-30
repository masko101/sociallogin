package masko101.sociallogin

import cats.effect.{IO, Sync}
import cats.implicits._
import masko101.sociallogin.apimodel.{Credentials, GeneralError, Secret, SecretCreate, ShareSecret, SharedSecret}
import masko101.sociallogin.services.{AuthenticationService, SecretService, SharedSecretService}
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import org.http4s.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.dsl.Http4sDsl
import masko101.sociallogin.apimodel.CirceEncodersDecoders._
import masko101.sociallogin.model.{SecretCreateEntity, SharedSecretEntity, UserEntity}

object SocialLoginRoutes {

  //TODO - replace with middleware
  def checkUserIdAuthorised(reqUserId: String, authUser: UserEntity, f: Long => IO[Response[IO]]):
  IO[Response[IO]] = {

    val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}
    import dsl._
    try {
      val userId = reqUserId.toLong
      if (authUser.id != userId)
        Forbidden()
      else
        f(userId)
    } catch {
      case _: NumberFormatException =>
        BadRequest()
      case _: Throwable =>
        InternalServerError()
    }
  }

  //TODO - better error handling and responses
  def secretRoutes(secretService: SecretService, sharedSecretService: SharedSecretService): AuthedRoutes[UserEntity, IO] = {
    val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}
    import dsl._
    AuthedRoutes.of[UserEntity, IO] {
      case GET -> Root / "users" / userId / "secrets" as user =>
        checkUserIdAuthorised(userId, user, (userId: Long) =>
          for {
            secrets <- secretService.getUserOwnedSecrets(userId)
            resp <- Ok(secrets.map(s => Secret(s.id, s.ownerId, s.secretText)).asJson)
          } yield resp
        )
      case GET -> Root / "users" / userId / "secrets" / secretIdString as user =>
        checkUserIdAuthorised(userId, user, (userId: Long) =>
          try {
            val secretId = secretIdString.toLong
            for {
              secrets <- secretService.getUserOwnedSecret(userId, secretId)
              resp <- secrets.map(s => Ok(Secret(s.id, s.ownerId, s.secretText))).getOrElse(NotFound())
            } yield resp
          } catch {
            case _: NumberFormatException =>
              BadRequest()

          }
        )
      case authReq@POST -> Root / "users" / userId / "secrets" as user =>
        checkUserIdAuthorised(userId, user, (userId: Long) =>
          for {
            secretCreate <- authReq.req.as[SecretCreate]
            secret <- secretService.createNewSecret(SecretCreateEntity(userId, secretCreate.secretText))
            response <- Ok(Secret(secret.id, secret.ownerId, secret.secretText))
          } yield response
        )
      case GET -> Root / "users" / userId / "sharedsecrets" as user =>
        checkUserIdAuthorised(userId, user, (userId: Long) =>
          for {
            secrets <- sharedSecretService.getSharedSecrets(userId)
            resp <- Ok(secrets.map(s => Secret(s.id, s.ownerId, s.secretText)).asJson)
          } yield resp
        )
      case authReq@POST -> Root / "users" / userId / "sharedsecrets"/ secretIdString as user =>
        checkUserIdAuthorised(userId, user, (userId: Long) =>
          try {
            val secretId = secretIdString.toLong
            for {
              shareSecret <- authReq.req.as[ShareSecret]
              sharedSecretEntity = SharedSecretEntity(shareSecret.userId, secretId)
              sharedSecretResp <- sharedSecretService.shareSecret(userId, sharedSecretEntity)
              response <- Ok(SharedSecret(sharedSecretResp.userId, sharedSecretResp.secretId))
            } yield response
          } catch {
            case _: NumberFormatException =>
              BadRequest()
          }
        )
      case GET -> Root / "users" / userId / "sharedsecrets" / secretIdString as user =>
        checkUserIdAuthorised(userId, user, (userId: Long) =>
          try {
            val secretId = secretIdString.toLong
            for {
              secrets <- sharedSecretService.getSharedSecret(userId, secretId)
              resp <- secrets.map(s => Ok(Secret(s.id, s.ownerId, s.secretText))).getOrElse(NotFound())
            } yield resp
          } catch {
            case _: NumberFormatException =>
              BadRequest()
          }
        )
    }
  }

  def helloWorldRoutes[F[_] : Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }

  def loginRoutesNoFriendPermission(authService: AuthenticationService): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._
    HttpRoutes.of[IO] {
      case req@POST -> Root / "login" =>
        for {
          credentials <- req.as[Credentials]
          authTokensOption <- authService.authenticate(credentials, None)
          response <- authTokensOption.map(a => Ok(a.asJson))
            .getOrElse(BadRequest(GeneralError(Some("Invalid Username or Password")).asJson))
        } yield {
          response
        }
    }
  }

  def loginRoutesWithFriendPermission(authService: AuthenticationService): AuthedRoutes[UserEntity, IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._
    AuthedRoutes.of[UserEntity, IO] {
      case authedReq@POST -> Root / "login" as permissionUser =>
        for {
          credentials <- authedReq.req.as[Credentials]
          authTokensOption <- authService.authenticate(credentials, Some(permissionUser))
          response <- authTokensOption.map(a => Ok(a.asJson))
            .getOrElse(BadRequest(GeneralError(Some("Invalid Username or Password")).asJson))
        } yield {
          response
        }
    }
  }
}