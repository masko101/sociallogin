package masko101.sociallogin

import cats.effect.IO
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import masko101.sociallogin.SocialLoginRoutes
import masko101.sociallogin.apimodel.{Secret, SecretCreate}
import masko101.sociallogin.model.{SecretCreateEntity, SecretEntity, UserEntity}
import masko101.sociallogin.repository.SecretRepository
import masko101.sociallogin.services.SecretService
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import org.specs2.specification.BeforeEach

class SecretSpec extends org.specs2.mutable.Specification with BeforeEach {
  sequential

  private val secretRepository = new SecretRepository
  val secretRoutes: AuthedRoutes[UserEntity, IO] = SocialLoginRoutes.secretRoutes(new SecretService(secretRepository))

  val userEntity: UserEntity = UserEntity(1, "hob", "bob", 2)

  override protected def before: Unit = {
    secretRepository.clear()
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

  private[this] def retSecretCreate(secretText: String): Response[IO] = {
    val postSecret = Request[IO](Method.POST, uri"/secrets", body =
        Stream.evalSeq(IO(SecretCreate(secretText).asJson.toString().getBytes.toSeq))
    )
    secretRoutes.orNotFound(AuthedRequest(userEntity, postSecret)).unsafeRunSync()
  }

  private[this] def retSecretsGet(): Response[IO] = {
    val getSecret = Request[IO](Method.GET, uri"/secrets")
    secretRoutes.orNotFound(AuthedRequest(userEntity, getSecret)).unsafeRunSync()
  }

  private[this] def retSecretGet(id: Long): Response[IO] = {
    val getSecret = Request[IO](Method.GET, Uri(path = s"/secrets/$id"))
    secretRoutes.orNotFound(AuthedRequest(userEntity, getSecret)).unsafeRunSync()
  }

  private[this] def secretGetAllReturnsList(): MatchResult[Secret] = {
    secretRepository.create(SecretCreateEntity(1, "My Secret 1"))
    secretRepository.create(SecretCreateEntity(1, "My Secret 2"))
    val secretResponse = retSecretsGet()
    secretResponse.status must beEqualTo(Status.Ok)
    val secrets = secretResponse.as[List[Secret]].unsafeRunSync()
    secrets must haveSize(2)
    secrets.head must beEqualTo(Secret(1L, 1L, "My Secret 1"))
    secrets(1) must beEqualTo(Secret(2L, 1L, "My Secret 2"))
  }


  private[this] def secretInvalidIdReturnsBadRequest(): MatchResult[Status] = {
    secretRepository.create(SecretCreateEntity(1, "My Secret 1"))
    retSecretGet(999L).status must beEqualTo(Status.NotFound)
  }

  private[this] def secretValidIdReturnsSecret(): MatchResult[Secret] = {
    secretRepository.create(SecretCreateEntity(1, "My Secret 1"))
    val secretResponse = retSecretGet(1L)
    secretResponse.status must beEqualTo(Status.Ok)
    val secret = secretResponse.as[Secret].unsafeRunSync()
    secret must beEqualTo(Secret(1L, 1L, "My Secret 1"))
  }

  private[this] def secretCreateReturnsSuccess(): MatchResult[Option[SecretEntity]] = {
    val newSecretResponse = retSecretCreate("My Secret 1")
    newSecretResponse.status must beEqualTo(Status.Ok)
    val newSecret = newSecretResponse.as[Secret].unsafeRunSync()
    newSecret must beEqualTo(Secret(1L, 1L, "My Secret 1"))

    secretRepository.findById(1).unsafeRunSync() must beSome(equalTo(SecretEntity(1L, 1L, "My Secret 1")))
  }

}