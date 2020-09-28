package masko101.sociallogin.apimodel

import java.time.{Instant, OffsetDateTime, ZoneId}

object AuthToken {
  //Should be enums
  val FRIEND_TOKEN = "friend"
  val AUTH_TOKEN = "auth"

  def parseToken(token: String) = {
    val tokenValues = token.split(':')
    val userId = tokenValues(0).toInt
    val tokenType = tokenValues(1)
    val expires = OffsetDateTime.ofInstant(Instant.ofEpochMilli(tokenValues(2).toLong), ZoneId.systemDefault())
    val signature = tokenValues(3)
    //Needs error handling in future should return either
    AuthToken(userId, tokenType, expires, signature)
  }
}

case class AuthToken(userId: Int, tokenType: String, expires: OffsetDateTime, signature: String) {
  def validate(checkTokenType: String): Boolean = {
    tokenType == checkTokenType && expires.isAfter(OffsetDateTime.now()) && signature == "IAmSigned"
  }
}

