package masko101.sociallogin.apimodel

/**
 * @param id  for example: ''1234''
 * @param secretText  for example: ''My secret text''
 */
case class Secret (
  id: Long,
  ownerId: Long,
  secretText: String
)

