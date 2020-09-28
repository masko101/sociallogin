package masko101.sociallogin.apimodel


/**
 * @param friendToken  for example: ''1|friend|1601106944853|IAmSigned''
 * @param authToken  for example: ''1|auth|1601106944853|IAmSigned''
 */
case class AuthTokens (
  friendToken: String,
  authToken: Option[String]
)

