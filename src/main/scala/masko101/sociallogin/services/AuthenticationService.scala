package masko101.sociallogin.services

import java.time.OffsetDateTime

import cats.effect.IO
import masko101.sociallogin.apimodel.{AuthTokens, Credentials}
import masko101.sociallogin.model.{AuthToken, UserEntity}
import masko101.sociallogin.repository.UserRepository

object AuthenticationService {
  val TOKEN_EXPIRE_MIN: Long = 20
}

class AuthenticationService(userRepository: UserRepository) {
  import AuthenticationService._
  def authenticate(login: Credentials, authFriend: Option[UserEntity]): IO[Option[AuthTokens]] = {
    for {
      authUser <- userRepository.findByUserNameAndPassword(login.username, login.password)
    } yield {
      authUser.map(a => {
        val authToken = authFriend.find(af => af.id == a.friendId)
          .map(_ => AuthToken.generateAndEncodeTokenString(createToken(a, AuthToken.AUTH_TOKEN)))
/*
          login.permissionToken
            .flatMap(pt => AuthToken.parseEncodedToken(pt)
              .find(pt => pt.validate(AuthToken.FRIEND_TOKEN) && pt.userId == a.friendId)
              .map(_ => AuthToken.generateAndEncodeTokenString(createToken(a, AuthToken.AUTH_TOKEN)))
            )
*/
        val friendToken: String = AuthToken.generateAndEncodeTokenString(createToken(a, AuthToken.FRIEND_TOKEN))
        AuthTokens(friendToken, authToken)
      })
    }
  }

  def validateToken(token: AuthToken, tokenType: String): IO[Option[UserEntity]] = {
    if (token.validate(tokenType))
      userRepository.findById(token.userId)
    else
      IO(None)
  }

  private def createToken(a: UserEntity, tokenType: String): AuthToken = {
    AuthToken(a.id, tokenType, getExpires)
  }

  private def getExpires: OffsetDateTime = {
    java.time.OffsetDateTime.now().plusMinutes(TOKEN_EXPIRE_MIN)
  }


}
