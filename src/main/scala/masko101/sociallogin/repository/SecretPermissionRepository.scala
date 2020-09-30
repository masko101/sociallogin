package masko101.sociallogin.repository

import cats.effect.IO
import masko101.sociallogin.model.SharedSecretEntity

import scala.collection.mutable

/*
  Will replace with an actual DB & tests
 */
//TODO - Create interface
class SecretPermissionRepository {

  //Evil mutable map for now
  //UserId to list of SharedSecretEntity
  val permissionMap: mutable.Map[Long, Set[SharedSecretEntity]] = new mutable.TreeMap[Long, Set[SharedSecretEntity]]();


  def findByUserId(id:Long): IO[Set[SharedSecretEntity]] = {
    IO.pure(permissionMap.getOrElse(id, Set.empty[SharedSecretEntity]));
  }

  def create(sharedSecret: SharedSecretEntity): IO[SharedSecretEntity] = {
    permissionMap.put(sharedSecret.userId, permissionMap.getOrElse(sharedSecret.userId, Set.empty) + sharedSecret)
    IO.pure(sharedSecret)
  }

  def delete(sharedSecret: SharedSecretEntity): IO[Option[SharedSecretEntity]] = {
    val secrets = permissionMap.getOrElse(sharedSecret.userId, Set.empty)
    val foundSecret = secrets.find(_ == sharedSecret)
    foundSecret.foreach(fs => permissionMap.put(fs.userId, secrets - fs))
    IO.pure(foundSecret)
  }

  def clear(): Unit = {
    permissionMap.clear()
  }

}
