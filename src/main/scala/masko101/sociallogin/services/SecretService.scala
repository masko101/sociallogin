package masko101.sociallogin.services

import cats.effect.IO
import masko101.sociallogin.model.{SecretCreateEntity, SecretEntity, UserEntity}
import masko101.sociallogin.repository.SecretRepository


class SecretService(secretRepository: SecretRepository) {

  def getUserOwnedSecret(userId: Long, secretId: Long): IO[Option[SecretEntity]] = {
    secretRepository.findById(secretId).map(_.filter(_.ownerId == userId))
  }

  def getUserOwnedSecrets(userId: Long): IO[Set[SecretEntity]] = {
    secretRepository.findByUserId(userId)
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
