package masko101.sociallogin.services

import cats.effect.IO
import masko101.sociallogin.apimodel.{AuthToken, AuthTokens, Credentials}
import masko101.sociallogin.model.User
import masko101.sociallogin.repository.UserRepository

object AuthenticationService {
  val TOKEN_EXPIRE: Long = 1000 * 60 * 20
}

class AuthenticationService(userRepository: UserRepository) {
  import AuthenticationService._
  def authenticate(login: Credentials): IO[Option[AuthTokens]] = {
    for {
      authUser <- userRepository.findByUserNameAndPassword(login.username, login.password)
    } yield {
      authUser.map(a => AuthTokens(createFriendToken(a), None))
    }
  }

  def validateAuthToken(token: AuthToken): IO[Option[User]] = {
    if (token.validate(AuthToken.AUTH_TOKEN))
      userRepository.findById(token.userId)
    else
      IO(None)
  }

  private def createFriendToken(a: User) = {
    s"${a.id}:friend:${getExpires.toString}:IAmSigned"
  }

  private def getExpires: Long = {
    java.time.OffsetDateTime.now().toInstant().toEpochMilli + TOKEN_EXPIRE
  }
}
