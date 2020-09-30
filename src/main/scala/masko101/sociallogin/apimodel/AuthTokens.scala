package masko101.sociallogin.apimodel

/**
 * @param permissionToken Base 64 encoded token string for example: ''MTpmcmllbmQ6MTYwMTQwODk2MjczODo=''
 * @param authToken Base 64 encoded token string for example: ''1|auth|1601106944853|IAmSigned''
 */
case class AuthTokens (
  permissionToken: String,
  authToken: Option[String]
)

