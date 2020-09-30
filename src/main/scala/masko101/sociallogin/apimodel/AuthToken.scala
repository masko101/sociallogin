package masko101.sociallogin.apimodel

import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util.Base64

object AuthToken {
  //Should be enums
  val FRIEND_TOKEN = "friend"
  val AUTH_TOKEN = "auth"

  def apply(userId: Long, tokenType: String, expires: OffsetDateTime, signature: String): AuthToken =
    new AuthToken(userId, tokenType, expires, signature)

  def apply(userId: Long, tokenType: String, expires: OffsetDateTime): AuthToken =
    new AuthToken(userId, tokenType, expires, "").sign()

  def parseEncodedToken(tokenEncoded: String): Option[AuthToken] = {
    parseToken(new String(Base64.getDecoder.decode(tokenEncoded)))
  }

  def parseToken(token: String): Option[AuthToken] = {

    try {
      val tokenValues = token.split(':')
      val userId = tokenValues(0).toLong
      val tokenType = tokenValues(1)
      val expires = OffsetDateTime.ofInstant(Instant.ofEpochMilli(tokenValues(2).toLong), ZoneId.systemDefault())
      val signature = tokenValues(3)
      //Needs error handling in future should return either
      Some(AuthToken(userId, tokenType, expires, signature))
    } catch {
      case e: Throwable =>
        println("Exception: " + e.getMessage)
        None
    }
  }

  def generateTokenString(token: AuthToken): String = {
    s"${token.userId}:${token.tokenType}:${token.expires.toInstant.toEpochMilli}:${token.signature}"
  }

  def generateAndEncodeTokenString(token: AuthToken): String = {
    Base64.getEncoder.encodeToString(generateTokenString(token).getBytes)
  }

}

case class AuthToken(userId: Long, tokenType: String, expires: OffsetDateTime, signature: String) {
  def validate(checkTokenType: String): Boolean = {
    tokenType == checkTokenType && expires.isAfter(OffsetDateTime.now()) && checkSigned
  }

  private def checkSigned: Boolean = {
    signature == "IAmSigned"
  }

  def sign(): AuthToken = {
    //TODO - Actually sign or encrypt
    this.copy(signature = "IAmSigned")
  }
}

