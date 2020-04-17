package io.github.weakteam.repository

import cats.{~>, Monad}
import cats.syntax.functor._
import com.github.ghik.silencer.silent
import doobie.free.connection.ConnectionIO
import fs2.Stream
import io.github.weakteam.model.{Role, WithId}
import io.github.weakteam.model.Role._
import io.github.weakteam.model.Role.RoleId._
import tofu.logging.{LoggingBase, Logs}
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.fragment.Fragment
import cats.instances.list._
import doobie.util.update.Update
import io.github.weakteam.model.Group.GroupId
import io.github.weakteam.model.Role.{RichRole, RoleId}

trait RoleRepository[DB[_]] {
  def findAllPaginated(lastKey: Option[RoleId]): Stream[DB, RichRole]
  def findOne(id: RoleId): DB[Option[RichRole]]
  def insert(groupId: GroupId, description: Option[String]): DB[Int]
  def update(role: RichRole): DB[Int]
  def remove(id: RoleId): DB[Int]
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
    def findAllPaginated(lastKey: Option[RoleId]): Stream[DB, RichRole] = {
      val from = lastKey.fold(Fragment.empty)(value => fr" where id > $value")
      val frag = Fragment.const("select id, group_id, description from roles") ++
            from ++
            Fragment.const("order by id asc limit 20")

      Stream.evals(xa(frag.query[WithId[RoleId, Role]].to[List]))
    }

    def findOne(id: RoleId): DB[Option[RichRole]] = {
      xa(sql"select id, group_id, description from roles where id = $id".query[WithId[RoleId, Role]].option)
    }

    def remove(id: RoleId): DB[Int] = {
      xa(sql"delete from roles where id = $id".update.run)
    }

    def update(role: RichRole): DB[Int] = {
      xa(
        Update[(Role, RoleId)](
          s"update roles set description = ?, group_id = ? where id = ?"
        ).toUpdate0((role.entity, role.id)).run
      )
    }

    def insert(groupId: GroupId, description: Option[String]): DB[Int] = {
      xa(
        Update[(GroupId, Option[String])](
          s"insert into roles (group_id, description) values (?, ?)"
        ).toUpdate0((groupId, description)).run
      )
    }
  }

  type Trans[F[_]] = ConnectionIO ~> F
}
