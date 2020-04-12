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
import doobie.util.update.Update

trait RoleRepository[DB[_]] {
  def findAllPaginated(lastKey: Option[PosInt]): Stream[DB, Role]
  def findOne(id: PosInt): DB[Option[Role]]
  def insert(groupId: PosInt, description: Option[String]): DB[Int]
  def update(role: Role): DB[Int]
  def remove(id: PosInt): DB[Int]
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

    def findOne(id: PosInt): DB[Option[Role]] = {
      xa(sql"select id, group_id, description from roles where id = $id".query[Role].option)
    }

    def remove(id: PosInt): DB[Int] = {
      xa(sql"delete from roles where id = $id".update.run)
    }

    def update(role: Role): DB[Int] = {
      xa(
        Update[((Option[String], PosInt), PosInt)](
          s"update roles set description = ?, group_id = ? where id = ?"
        ).toUpdate0(((role.description, role.groupId), role.id)).run
      )
    }

    def insert(groupId: PosInt, description: Option[String]): DB[Int] = {
      xa(
        Update[(PosInt, Option[String])](
          s"insert into roles (group_id, description) values (?, ?)"
        ).toUpdate0((groupId, description)).run
      )
    }
  }

  type Trans[F[_]] = ConnectionIO ~> F
}
