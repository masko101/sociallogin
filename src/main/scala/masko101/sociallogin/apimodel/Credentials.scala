package masko101.sociallogin.apimodel

/**
 * @param username          for example: ''rob5000''
 * @param password          for example: ''robsbirthday''
 */
// * @param permissionToken   for example: ''1|friend|1601106944853|IAmSigned''
case class Credentials(
  username: String,
  password: String
//  ,
//  permissionToken: Option[String]
)

