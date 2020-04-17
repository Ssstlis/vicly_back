package io.github.weakteam.service

import cats.{~>, FlatMap, Monad}
import cats.syntax.apply._
import cats.syntax.functor._
import eu.timepit.refined.types.numeric.PosLong
import fs2.Stream
import io.github.weakteam.model.{Role, WithId}
import io.github.weakteam.model.Role.{RichRole, RoleId}
import io.github.weakteam.repository.RoleRepository
import io.github.weakteam.util.tofu.logging.implicits._
import tofu.logging.{LoggingBase, Logs}
import tofu.syntax.logging._

trait RoleService[F[_]] {
  def findAllPaginated(lastKey: Option[PosLong]): F[Stream[F, WithId[RoleId, Role]]]
  def findOne(key: PosLong): F[Option[RichRole]]
  def insert(role: Role): F[Int]
  def update(role: Role, id: PosLong): F[Int]
  def remove(id: PosLong): F[Int]
}

object RoleService {

  def apply[I[_]: FlatMap, F[_]: Monad, DB[_]](
    repository: RoleRepository[DB],
    xa: DB ~> F,
    logPv: Logs[I, F]
  ): I[RoleService[F]] = {
    for {
      implicit0(loggerI: LoggingBase[F]) <- logPv.forService[RoleService[F]]
    } yield {
      new Impl[F, DB](repository, xa)
    }
  }

  private final class Impl[F[_]: Monad, DB[_]](
    repository: RoleRepository[DB],
    xa: DB ~> F
  )(implicit logger: LoggingBase[F])
    extends RoleService[F] {

    def findAllPaginated(lastKey: Option[PosLong]): F[Stream[F, WithId[RoleId, Role]]] = {
      info"findAllPaginated with lastKey $lastKey".as {
        repository.findAllPaginated(lastKey.map(RoleId(_))).translate(xa)
      }
    }

    def findOne(key: PosLong): F[Option[RichRole]] = {
      info"findOne with key $key" *>
        xa(repository.findOne(RoleId(key)))
    }

    def insert(role: Role): F[Int] = {
      info"insert $role" *>
        xa(repository.insert(role.groupId, role.description))
    }

    def update(role: Role, id: PosLong): F[Int] = {
      info"update $role with id $id" *>
        xa(repository.update(WithId(RoleId(id), role)))
    }

    def remove(id: PosLong): F[Int] = {
      info"remove $id" *> xa(repository.remove(RoleId(id)))
    }
  }
}
