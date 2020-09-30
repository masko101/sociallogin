package masko101.sociallogin.services

import cats.effect.IO
import masko101.sociallogin.model.{SecretCreateEntity, SecretEntity, SharedSecretEntity, UserEntity}
import masko101.sociallogin.repository.{SecretPermissionRepository, SecretRepository}

class SharedSecretService(secretPermissionRepository: SecretPermissionRepository,
                          secretRepository: SecretRepository) {

  def getSharedSecrets(userId: Long): IO[Set[SecretEntity]] = {
    for {
      secretsSharedWithUser <- secretPermissionRepository.findByUserId(userId)
      secrets <- secretRepository.findAllById(secretsSharedWithUser.map(_.secretId))
    } yield secrets
  }

  def getSharedSecret(userId: Long, secretId: Long): IO[Option[SecretEntity]] = {
    secretPermissionRepository.findByUserId(userId).flatMap { secretsSharedWithUser =>
      if (secretsSharedWithUser.exists(_.secretId == secretId)) {
        secretRepository.findById(secretId)
      } else {
        IO.raiseError(NotAuthorisedException())
      }
    }
  }

  def shareSecret(userId: Long, sharedSecret: SharedSecretEntity): IO[SharedSecretEntity] = {
    secretRepository.findById(sharedSecret.secretId).flatMap {
      case Some(secret) =>
        if (secret.ownerId == userId)
          secretPermissionRepository.create(sharedSecret)
        else
          IO.raiseError(NotAuthorisedException())
      case None =>
        IO.raiseError(NotFoundException("Secret not found"))
    }
  }

  def deleteSharedSecret(userId: Long, sharedSecret: SharedSecretEntity): IO[Option[SharedSecretEntity]] = {
    secretRepository.findById(sharedSecret.secretId).flatMap {
      case Some(secret) =>
        if (secret.ownerId == userId)
          secretPermissionRepository.delete(sharedSecret)
        else
          IO.raiseError(NotAuthorisedException())
      case None =>
        IO.raiseError(NotFoundException("Secret not found"))
    }}
}
