package masko101.sociallogin

import cats.effect.IO
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import masko101.sociallogin.SocialLoginRoutes
import masko101.sociallogin.apimodel.{Secret, SecretCreate, ShareSecret, SharedSecret}
import masko101.sociallogin.model.{SecretCreateEntity, SecretEntity, SharedSecretEntity, UserEntity}
import masko101.sociallogin.repository.{SecretPermissionRepository, SecretRepository}
import masko101.sociallogin.services.{SecretService, SharedSecretService}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import org.specs2.specification.BeforeEach

class SecretSpec extends org.specs2.mutable.Specification with BeforeEach {
  sequential

  private val secretPermissionRepo = new SecretPermissionRepository
  private val secretRepo = new SecretRepository
  val secretRoutes: AuthedRoutes[UserEntity, IO] =
    SocialLoginRoutes.secretRoutes(new SecretService(secretRepo), new SharedSecretService(secretPermissionRepo, secretRepo))

  val userEntity: UserEntity = UserEntity(1, "hob", "bob", 2)

  override protected def before: Unit = {
    secretRepo.clear()
  }

  "secret" >> {
    "create returns the new secret" >> {
      secretCreateReturnsSuccess()
    }
    "get all returns a list of owned secrets" >> {
      secretGetAllReturnsList()
    }
    "with valid secret id returns an owned secrets" >> {
      secretValidIdReturnsSecret()
    }
    "with invalid secret id returns bad request" >> {
      secretInvalidIdReturnsBadRequest()
    }
  }

  "shared secret" >> {
    "create returns the new shared secret" >> {
      shareSecretReturnsSuccess()
    }
    "get all returns a list of shared secrets" >> {
      sharedSecretGetAllReturnsList()
    }
    "with valid secret id returns a shared secret" >> {
      secretIdSharedReturnsSecret()
    }
    "with unshared secret id returns forbidden" >> {
      secretIdUnsharedReturnsUnauthorised()
    }
  }

  private[this] def retSecretCreate(userId: Long, secretText: String): Response[IO] = {
    val postSecret = Request[IO](Method.POST, Uri(path = s"/users/$userId/secrets"), body =
        Stream.evalSeq(IO(SecretCreate(secretText).asJson.toString().getBytes.toSeq))
    )
    secretRoutes.orNotFound(AuthedRequest(userEntity, postSecret)).unsafeRunSync()
  }

  private[this] def retSecretsGet(userId: Long): Response[IO] = {
    val getSecret = Request[IO](Method.GET, Uri(path = s"/users/$userId/secrets"))
    secretRoutes.orNotFound(AuthedRequest(userEntity, getSecret)).unsafeRunSync()
  }

  private[this] def retSecretGet(userId: Long, secretId: Long): Response[IO] = {
    val getSecret = Request[IO](Method.GET, Uri(path = s"/users/$userId/secrets/$secretId"))
    secretRoutes.orNotFound(AuthedRequest(userEntity, getSecret)).unsafeRunSync()
  }

  private[this] def secretGetAllReturnsList(): MatchResult[Secret] = {
    secretRepo.create(SecretCreateEntity(1, "My Secret 1"))
    secretRepo.create(SecretCreateEntity(1, "My Secret 2"))
    val secretResponse = retSecretsGet(1L)
    secretResponse.status must beEqualTo(Status.Ok)
    val secrets = secretResponse.as[List[Secret]].unsafeRunSync()
    secrets must haveSize(2)
    secrets.head must beEqualTo(Secret(1L, 1L, "My Secret 1"))
    secrets(1) must beEqualTo(Secret(2L, 1L, "My Secret 2"))
  }


  private[this] def secretInvalidIdReturnsBadRequest(): MatchResult[Status] = {
    secretRepo.create(SecretCreateEntity(1, "My Secret 1"))
    retSecretGet(1L, 999L).status must beEqualTo(Status.NotFound)
  }

  private[this] def secretValidIdReturnsSecret(): MatchResult[Secret] = {
    secretRepo.create(SecretCreateEntity(1, "My Secret 1"))
    val secretResponse = retSecretGet(1L, 1L)
    secretResponse.status must beEqualTo(Status.Ok)
    val secret = secretResponse.as[Secret].unsafeRunSync()
    secret must beEqualTo(Secret(1L, 1L, "My Secret 1"))
  }

  private[this] def secretCreateReturnsSuccess(): MatchResult[Option[SecretEntity]] = {
    val newSecretResponse = retSecretCreate(1L, "My Secret 1")
    newSecretResponse.status must beEqualTo(Status.Ok)
    val newSecret = newSecretResponse.as[Secret].unsafeRunSync()
    newSecret must beEqualTo(Secret(1L, 1L, "My Secret 1"))

    secretRepo.findById(1).unsafeRunSync() must beSome(equalTo(SecretEntity(1L, 1L, "My Secret 1")))
  }

  private[this] def retShareSecret(userId: Long, withUserId: Long, secretId: Long): Response[IO] = {
    val postSecret = Request[IO](Method.POST, Uri(path = s"/users/$userId/sharedsecrets/$secretId"), body =
      Stream.evalSeq(IO(ShareSecret(withUserId).asJson.toString().getBytes.toSeq))
    )
    secretRoutes.orNotFound(AuthedRequest(userEntity, postSecret)).unsafeRunSync()
  }

  private[this] def retSharedSecretsGet(userId: Long): Response[IO] = {
    val getSecret = Request[IO](Method.GET, Uri(path = s"/users/$userId/sharedsecrets"))
    secretRoutes.orNotFound(AuthedRequest(userEntity, getSecret)).unsafeRunSync()
  }

  private[this] def retSharedSecretGet(userId: Long, secretId: Long): Response[IO] = {
    val getSecret = Request[IO](Method.GET, Uri(path = s"/users/$userId/sharedsecrets/$secretId"))
    secretRoutes.orNotFound(AuthedRequest(userEntity, getSecret)).unsafeRunSync()
  }

  private[this] def shareSecretReturnsSuccess(): MatchResult[Set[SharedSecretEntity]] = {
    secretRepo.create(SecretCreateEntity(1, "My Secret 1"))
    val newSecretResponse = retShareSecret(1, 2, 1)
    newSecretResponse.status must beEqualTo(Status.Ok)
    val newSecret = newSecretResponse.as[SharedSecret].unsafeRunSync()
    newSecret must beEqualTo(SharedSecret(2L, 1L))

    secretPermissionRepo.findByUserId(2).unsafeRunSync() must beEqualTo(Set(SharedSecretEntity(2L, 1L)))
  }

  private[this] def sharedSecretGetAllReturnsList(): MatchResult[Secret] = {
    secretRepo.create(SecretCreateEntity(2, "His Secret 1"))
    secretRepo.create(SecretCreateEntity(2, "His Secret 2"))
    secretRepo.create(SecretCreateEntity(2, "His Secret 3"))
    secretPermissionRepo.create(SharedSecretEntity(1, 1))
    secretPermissionRepo.create(SharedSecretEntity(1, 3))
    val secretResponse = retSharedSecretsGet(1)
    secretResponse.status must beEqualTo(Status.Ok)
    val secrets = secretResponse.as[List[Secret]].unsafeRunSync()
    secrets must haveSize(2)
    secrets.head must beEqualTo(Secret(1L, 2L, "His Secret 1"))
    secrets(1) must beEqualTo(Secret(3L, 2L, "His Secret 3"))
  }


  private[this] def secretIdUnsharedReturnsUnauthorised(): MatchResult[Status] = {
    secretRepo.create(SecretCreateEntity(1, "My Secret 1"))
    retSharedSecretGet(2, 1L).status must beEqualTo(Status.Forbidden)
  }

  private[this] def secretIdSharedReturnsSecret(): MatchResult[SharedSecret] = {
    secretRepo.create(SecretCreateEntity(1, "My Secret 1"))
    val secretResponse = retShareSecret(1L, 2L, 1L)
    secretResponse.status must beEqualTo(Status.Ok)
    val secret = secretResponse.as[SharedSecret].unsafeRunSync()
    secret must beEqualTo(SharedSecret(2L, 1L))
  }

}