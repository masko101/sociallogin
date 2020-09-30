package masko101.sociallogin

import cats.data.{Kleisli, OptionT}
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import fs2.Stream
import masko101.sociallogin.model.{AuthToken, UserEntity}
import masko101.sociallogin.repository.{SecretPermissionRepository, SecretRepository, UserRepository}
import masko101.sociallogin.services.{AuthenticationService, SecretService, SharedSecretService}
import org.http4s.Request
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.util.CaseInsensitiveString

import scala.concurrent.ExecutionContext.global

object SocialLoginServer {

  private def getTokenFromHeader(req: Request[IO]): Option[String] = {
    req.headers.get(CaseInsensitiveString("Authorization")).map(h => {
      //TODO - Replace with regex extraction
      h.value.split(' ')(1)
    })
  }

  def authTokenUser(authService: AuthenticationService): Kleisli[OptionT[IO, *], Request[IO], UserEntity] =
    Kleisli { req: Request[IO] =>
      getTokenFromHeader(req).flatMap(t => {
        AuthToken.parseEncodedToken(t).map(t => authService.validateToken(t, AuthToken.AUTH_TOKEN))
      }).map(OptionT(_)).getOrElse(OptionT.none)
    }

  //TODO - find a better way to do this so that a bad token will return 401 not just a null authToken
  def permissionUser(authService: AuthenticationService): Kleisli[OptionT[IO, *], Request[IO], UserEntity] =
    Kleisli { req: Request[IO] =>
      getTokenFromHeader(req).flatMap(t => {
        AuthToken.parseEncodedToken(t).map(t => authService.validateToken(t, AuthToken.FRIEND_TOKEN))
      }).map(OptionT(_)).getOrElse(OptionT.none)
    }

  def stream(implicit T: Timer[IO], C: ContextShift[IO]): Stream[IO, Nothing] = {
    for {
      client <- BlazeClientBuilder[IO](global).stream
      helloWorldAlg = HelloWorld.impl[IO]
      userRepo = new UserRepository()
      authService = new AuthenticationService(userRepo)
      authenticateWithFriendPermissionMiddleware = AuthMiddleware.withFallThrough(permissionUser(authService))
      authedUserActionMiddleware = AuthMiddleware(authTokenUser(authService))
      secretRepo = new SecretRepository()
      secretService = new SecretService(secretRepo)
      secretPermissionRepo = new SecretPermissionRepository()
      sharedSecretService = new SharedSecretService(secretPermissionRepo, secretRepo)
      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        authenticateWithFriendPermissionMiddleware(SocialLoginRoutes.loginRoutesWithFriendPermission(authService))  <+>
        SocialLoginRoutes.loginRoutesNoFriendPermission(authService)  <+>
        SocialLoginRoutes.helloWorldRoutes[IO](helloWorldAlg) <+>
        authedUserActionMiddleware(SocialLoginRoutes.secretRoutes(secretService, sharedSecretService))
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[IO](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
