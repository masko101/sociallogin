package masko101.sociallogin.repository

import cats.effect.IO
import masko101.sociallogin.model.{SecretCreateEntity, SecretEntity}

import scala.collection.mutable

/*
  Will replace with an actual DB & tests
 */
//TODO - Create interface
class SecretRepository {

  //Evil mutable map for now
  val secretMap: mutable.Map[Long, SecretEntity] = new mutable.TreeMap[Long, SecretEntity]();
/*
  {
    val hobSec1: SecretEntity = SecretEntity(1L, 1L, "hob's biggest secret")
    val hobSec2: SecretEntity = SecretEntity(2L, 1L, "hob's scariest secret")
    val pojSec1: SecretEntity = SecretEntity(3L, 2L, "poj's embarrasing secret")
    val pojSec2: SecretEntity = SecretEntity(4L, 2L, "poj's  secret that hob knows")
    secretMap.put(hobSec1.id, hobSec1)
    secretMap.put(hobSec2.id, hobSec2)
    secretMap.put(pojSec1.id, pojSec1)
    secretMap.put(pojSec2.id, pojSec2)
  }
*/

  def findById(id:Long): IO[Option[SecretEntity]] = {
    IO.pure(secretMap.get(id));
  }

  def findByUserId(userId: Long): IO[List[SecretEntity]] = {
    IO.pure(secretMap.filter(_._2.ownerId == userId).values.toList);
  }

  def create(secret: SecretCreateEntity): IO[SecretEntity] = {
    val newSecret: SecretEntity = SecretEntity(getNextId, secret.userId, secret.secretText)
    secretMap.put(newSecret.id, newSecret)
    //TODO - Custom exceptions
    IO.pure(newSecret)
  }

  def update(secret: SecretEntity): IO[SecretEntity] = {
    secretMap.put(secret.id, secret)
    IO.pure(secret)
  }

  def delete(secretId: Long): IO[Option[SecretEntity]] = {
    IO.pure(secretMap.remove(secretId))
  }

  def clear(): Unit = {
    secretMap.clear()
  }

  private def getNextId: Long = {
    secretMap.keySet.lastOption.getOrElse(0L) + 1L
  }

}
