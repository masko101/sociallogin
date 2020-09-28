package masko101.sociallogin

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

object Main extends IOApp {
  def run(args: List[String]) =
    SocialLoginServer.stream.compile.drain.as(ExitCode.Success)
}