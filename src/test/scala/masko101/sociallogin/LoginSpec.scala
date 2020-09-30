package masko101.sociallogin

import java.time.OffsetDateTime

import cats.effect.IO
import masko101.sociallogin.apimodel.{AuthToken, AuthTokens}
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import io.circe.syntax._
import io.circe.generic.auto._
import masko101.sociallogin.apimodel.CirceEncodersDecoders._
import masko101.sociallogin.repository.UserRepository
import masko101.sociallogin.services.AuthenticationService
import fs2.Stream
import masko101.sociallogin.model.UserEntity

class LoginSpec extends org.specs2.mutable.Specification {

  "Login" >> {
    "with valid credentials but no friend permission returns only the friend token" >> {
      loginNoFriendPermissionReturnsSuccess()
    }
    "with valid credentials and friend permission token in the auth header returns both the friend token and auth token" >> {
      loginWithFriendPermissionReturnsSuccess()
    }
    "with invalid credentials returns bad request" >> {
      loginInvalidCredentialsReturnsBadRequest()
    }
  }

  private[this] def retLoginNoFriendPermission(username: String, password: String): Response[IO] = {
    val postLogin = Request[IO](Method.POST, uri"/login", body =
        Stream.evalSeq(IO(apimodel.Credentials(username, password).asJson.toString().getBytes.toSeq)))
    SocialLoginRoutes.loginRoutesNoFriendPermission(new AuthenticationService(new UserRepository())).orNotFound(postLogin).unsafeRunSync()
  }

  private[this] def retLoginWithFriendPermission(username: String, password: String,
                                                 permissionUserId: Long): Response[IO] = {
    val postLogin = Request[IO](Method.POST, uri"/login", body =
      Stream.evalSeq(IO(apimodel.Credentials(username, password).asJson.toString().getBytes.toSeq)))
    val permissionUserEntity: UserEntity = UserEntity(permissionUserId, "", "", 1)
    SocialLoginRoutes.loginRoutesWithFriendPermission(new AuthenticationService(new UserRepository()))
      .orNotFound(AuthedRequest(permissionUserEntity, postLogin)).unsafeRunSync()
  }

  private[this] def loginInvalidCredentialsReturnsBadRequest(): MatchResult[Status] =
    retLoginNoFriendPermission("hob", "wrong").status must beEqualTo(Status.BadRequest)

  private[this] def loginNoFriendPermissionReturnsSuccess(): MatchResult[String] = {
    val loginReturn = retLoginNoFriendPermission("hob", "bob")
    loginReturn.status must beEqualTo(Status.Ok)
    val retAuthTokens = loginReturn.as[AuthTokens].unsafeRunSync()
    retAuthTokens.authToken must beNone
    val maybeFriendToken = AuthToken.parseEncodedToken(retAuthTokens.friendToken)
    maybeFriendToken must beSome(haveClass[AuthToken])
    maybeFriendToken.get.userId must beEqualTo(1)
    maybeFriendToken.get.tokenType must beEqualTo("friend")
    val milliMin = OffsetDateTime.now().plusMinutes(19).toInstant.toEpochMilli
    val milliMax = OffsetDateTime.now().plusMinutes(20).toInstant.toEpochMilli
    maybeFriendToken.get.expires.toInstant.toEpochMilli must beBetween(milliMin, milliMax)
    maybeFriendToken.get.signature must beEqualTo("IAmSigned")
  }

  private[this] def loginWithFriendPermissionReturnsSuccess(): MatchResult[String] = {
    val loginReturn = retLoginWithFriendPermission("hob", "bob", 2L)
    loginReturn.status must beEqualTo(Status.Ok)
    val retAuthTokens = loginReturn.as[AuthTokens].unsafeRunSync()

    val milliMin = OffsetDateTime.now().plusMinutes(19).toInstant.toEpochMilli
    val milliMax = OffsetDateTime.now().plusMinutes(20).toInstant.toEpochMilli

    val maybeAuthToken = retAuthTokens.authToken.flatMap(AuthToken.parseEncodedToken)
    maybeAuthToken must beSome(haveClass[AuthToken])
    maybeAuthToken.get.userId must beEqualTo(1)
    maybeAuthToken.get.tokenType must beEqualTo(AuthToken.AUTH_TOKEN)
    maybeAuthToken.get.expires.toInstant.toEpochMilli must beBetween(milliMin, milliMax)
    maybeAuthToken.get.signature must beEqualTo("IAmSigned")

    val maybeFriendToken = AuthToken.parseEncodedToken(retAuthTokens.friendToken)
    maybeFriendToken must beSome(haveClass[AuthToken])
    maybeFriendToken.get.userId must beEqualTo(1)
    maybeFriendToken.get.tokenType must beEqualTo(AuthToken.FRIEND_TOKEN)
    maybeFriendToken.get.expires.toInstant.toEpochMilli must beBetween(milliMin, milliMax)
    maybeFriendToken.get.signature must beEqualTo("IAmSigned")
  }
}