package io.github.weakteam.repository

import cats.{~>, Monad}
import cats.syntax.functor._
import com.github.ghik.silencer.silent
import doobie.free.connection.ConnectionIO
import fs2.Stream
import eu.timepit.refined.types.numeric.PosInt
import io.github.weakteam.model.Role
import tofu.logging.{LoggingBase, Logs}
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.fragment.Fragment
import cats.instances.list._

trait RoleRepository[DB[_]] {
  def findAllPaginated(lastKey: Option[PosInt]): Stream[DB, Role]
}

object RoleRepository {

  def apply[I[_]: Monad, DB[_]](
    xa: Trans[DB],
    logPv: Logs[I, DB]
  ): I[RoleRepository[DB]] = {
    for {
      logger <- logPv.forService[RoleRepository[DB]]
    } yield new Impl[DB](xa)(logger)
  }

  private final class Impl[DB[_]](xa: Trans[DB])(implicit @silent logger: LoggingBase[DB]) extends RoleRepository[DB] {
    def findAllPaginated(lastKey: Option[PosInt]): Stream[DB, Role] = {
      val from = lastKey.fold(Fragment.empty)(value => fr" where id > $value")
      val frag = Fragment.const("select id, group_id, description from roles") ++
            from ++
            Fragment.const("order by id asc limit 20")

      Stream.evals(xa(frag.query[Role].to[List]))
    }
  }

  type Trans[F[_]] = ConnectionIO ~> F
}
