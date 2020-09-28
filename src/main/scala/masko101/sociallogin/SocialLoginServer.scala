package masko101.sociallogin

import java.time.OffsetDateTime

import cats.data.{Kleisli, OptionT}
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import fs2.Stream
import masko101.sociallogin.apimodel.AuthToken
import masko101.sociallogin.model.User
import masko101.sociallogin.repository.UserRepository
import masko101.sociallogin.services.AuthenticationService
import org.http4s.Request
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.util.CaseInsensitiveString

import scala.concurrent.ExecutionContext.global

object SocialLoginServer {

  def stream(implicit T: Timer[IO], C: ContextShift[IO]): Stream[IO, Nothing] = {

    def authUser(authService: AuthenticationService): Kleisli[OptionT[IO, *], Request[IO], User] =
      Kleisli { req: Request[IO] =>
        //      val maybeAuth = req.headers.get(Authorization)
        val authType = req.authType
        println("BOOOOOM: " + authType)
        println("BAAAAAM: " + req.headers.get(CaseInsensitiveString("Authorization")))
        //Better error handling needed with more specific exceptions
        //Not sure about this lift
//        OptionT.liftF(authService.validateAuthToken(AuthToken(1, "", OffsetDateTime.now(), "")).map(OptionT.fromOption(_)))
        req.headers.get(CaseInsensitiveString("Authorization")).map(h => {
          OptionT(authService.validateAuthToken(AuthToken.parseToken(h.value.split(' ')(1))))
        }).getOrElse(OptionT.none)
      }

//    val authMiddleware: AuthenticationService => AuthMiddleware[IO, User] = (authService: AuthenticationService) =>
//      AuthMiddleware(authUser(authService))

    for {
      client <- BlazeClientBuilder[IO](global).stream
      helloWorldAlg = HelloWorld.impl[IO]
      jokeAlg = Jokes.impl[IO](client)
      userRepo = new UserRepository()
      authService = new AuthenticationService(userRepo)
      authMiddleware = AuthMiddleware(authUser(authService))
      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        SocialLoginRoutes.loginRoutes(authService)  <+>
        SocialLoginRoutes.helloWorldRoutes[IO](helloWorldAlg) <+>
        authMiddleware(SocialLoginRoutes.jokeRoutes[IO](jokeAlg))
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[IO](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
