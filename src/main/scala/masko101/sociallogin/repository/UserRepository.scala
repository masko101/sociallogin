package masko101.sociallogin.repository

import cats.effect.IO
import masko101.sociallogin.model.User

import scala.collection.mutable

class UserRepository {

  //Evil mutable map for now
  val userMap: mutable.Map[Int, User] = new mutable.HashMap[Int, User]();
  {
    val hob: User = User(1, "hob", "bob", 2)
    val poj: User = User(2, "poj", "joj", 1)
    userMap.put(hob.id, hob)
    userMap.put(poj.id, poj)
  }

  def findById(id:Int): IO[Option[User]] = {
    IO(userMap.get(id));
  }

  def findByUserNameAndPassword(username:String, password: String): IO[Option[User]] = {
    IO(userMap.find(u => u._2.username == username && u._2.password == password).map(_._2));
  }
}
