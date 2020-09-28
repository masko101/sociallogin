package masko101.sociallogin

import cats.effect.{IO, Sync}
import cats.implicits._
import masko101.sociallogin.apimodel.{Credentials, GeneralError}
import masko101.sociallogin.services.AuthenticationService
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.dsl.Http4sDsl
import masko101.sociallogin.apimodel.CirceEncodersDecoders._
import masko101.sociallogin.model.User

object SocialLoginRoutes {

  def jokeRoutes[F[_]: Sync](J: Jokes[F]): AuthedRoutes[User, F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    AuthedRoutes.of[User, F] {
      case GET -> Root / "joke" as user =>
        println(s"Joke user: $user")
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
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