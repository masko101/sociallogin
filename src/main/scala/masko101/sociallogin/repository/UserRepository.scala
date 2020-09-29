package masko101.sociallogin.repository

import cats.effect.IO
import masko101.sociallogin.model.UserEntity

import scala.collection.mutable

class UserRepository {

  //Evil mutable map for now
  val userMap: mutable.Map[Long, UserEntity] = new mutable.HashMap[Long, UserEntity]();
  {
    val hob: UserEntity = UserEntity(1L, "hob", "bob", 2L)
    val poj: UserEntity = UserEntity(2L, "poj", "joj", 1L)
    userMap.put(hob.id, hob)
    userMap.put(poj.id, poj)
  }

  def findById(id:Int): IO[Option[UserEntity]] = {
    IO(userMap.get(id));
  }

  def findByUserNameAndPassword(username:String, password: String): IO[Option[UserEntity]] = {
    IO(userMap.find(u => u._2.username == username && u._2.password == password).map(_._2));
  }
}
