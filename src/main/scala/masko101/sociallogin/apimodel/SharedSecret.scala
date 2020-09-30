package masko101.sociallogin.apimodel

/**
 * @param userId  for example: ''1234''
 * @param secretId  for example: ''1234''
 */
case class SharedSecret (
  userId: Long,
  secretId: Long
)

