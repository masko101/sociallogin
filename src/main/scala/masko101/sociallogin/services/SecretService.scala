package masko101.sociallogin.services

import cats.effect.IO
import masko101.sociallogin.model.{SecretCreateEntity, SecretEntity, UserEntity}
import masko101.sociallogin.repository.SecretRepository


class SecretService(secretRepository: SecretRepository) {

  def getUserOwnedSecret(authUser: UserEntity, secretId: Long): IO[Option[SecretEntity]] = {
    secretRepository.findById(secretId)
  }

  def getUserOwnedSecrets(authUser: UserEntity): IO[List[SecretEntity]] = {
    secretRepository.findByUserId(authUser.id)
  }

  def createNewSecret(secret: SecretCreateEntity): IO[SecretEntity] = {
    secretRepository.create(secret)
  }

  def updateSecret(secret: SecretEntity): IO[SecretEntity] = {
    secretRepository.update(secret)
  }

  def deleteSecret(secret: SecretEntity): IO[SecretEntity] = {
    secretRepository.update(secret)
  }
}
