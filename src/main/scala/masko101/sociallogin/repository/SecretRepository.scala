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

  def findById(id:Long): IO[Option[SecretEntity]] = {
    IO.pure(secretMap.get(id));
  }

  def findAllById(ids: Set[Long]): IO[Set[SecretEntity]] = {
    IO.pure(secretMap.filter(se => ids.contains(se._1)).values.toSet)
  }

  def findByUserId(userId: Long): IO[Set[SecretEntity]] = {
    IO.pure(secretMap.filter(_._2.ownerId == userId).values.toSet)
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
