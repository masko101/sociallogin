package masko101.sociallogin

import java.time.OffsetDateTime

import cats.effect.IO
import masko101.sociallogin.apimodel.AuthTokens
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import io.circe.syntax._
import io.circe.generic.auto._
import masko101.sociallogin.apimodel.CirceEncodersDecoders._
import masko101.sociallogin.repository.UserRepository
import masko101.sociallogin.services.AuthenticationService
import fs2.Stream

class LoginSpec extends org.specs2.mutable.Specification {

  "Login" >> {
    "with valid credentials returns tokens" >> {
      loginReturnsSuccess()
    }
    "with invalid credentials returns bad request" >> {
      loginInvalidCredentialsReturnsBadRequest()
    }
  }

  private[this] def retLogin(username: String, password: String): Response[IO] = {
    val postLogin = Request[IO](Method.POST, uri"/login", body =
        Stream.evalSeq(IO(apimodel.Credentials(username, password).asJson.toString().getBytes.toSeq)))
    SocialLoginRoutes.loginRoutes(new AuthenticationService(new UserRepository())).orNotFound(postLogin).unsafeRunSync()
  }

  private[this] def loginInvalidCredentialsReturnsBadRequest(): MatchResult[Status] =
    retLogin("hob", "wrong").status must beEqualTo(Status.BadRequest)

  private[this] def loginReturnsSuccess(): MatchResult[String] = {
    val loginReturn = retLogin("hob", "bob")
    loginReturn.status must beEqualTo(Status.Ok)
    val retAuthTokens = loginReturn.as[AuthTokens].unsafeRunSync()
    retAuthTokens.authToken must beNone
    val tokenValues = retAuthTokens.friendToken.split(':')
    tokenValues(0) must beEqualTo("1")
    tokenValues(1) must beEqualTo("friend")
    val milliMin = OffsetDateTime.now().plusMinutes(19).toInstant.toEpochMilli
    val milliMax = OffsetDateTime.now().plusMinutes(20).toInstant.toEpochMilli
    tokenValues(2).toLong must beBetween(milliMin, milliMax)
    tokenValues(3) must beEqualTo("IAmSigned")
  }
}