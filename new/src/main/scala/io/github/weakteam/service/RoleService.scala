package io.github.weakteam.service

import cats.{~>, FlatMap, Monad}
import cats.syntax.apply._
import cats.syntax.functor._
import eu.timepit.refined.types.numeric.PosInt
import fs2.Stream
import io.github.weakteam.model.Role
import io.github.weakteam.repository.RoleRepository
import io.github.weakteam.util.tofu.logging.implicits._
import tofu.logging.{LoggingBase, Logs}
import tofu.syntax.logging._

trait RoleService[F[_]] {
  def findAllPaginated(lastKey: Option[PosInt]): F[Stream[F, Role]]
  def findOne(key: PosInt): F[Option[Role]]
  def insert(role: Role): F[Int]
  def update(role: Role): F[Int]
  def remove(id: PosInt): F[Int]
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

    def findAllPaginated(lastKey: Option[PosInt]): F[Stream[F, Role]] = {
      info"findAllPaginated with lastKey $lastKey".as {
        repository.findAllPaginated(lastKey).translate(xa)
      }
    }

    def findOne(key: PosInt): F[Option[Role]] = {
      info"findOne with key $key" *>
        xa(repository.findOne(key))
    }

    def insert(role: Role): F[Int] = {
      info"insert ${role.groupId}, ${role.description}" *>
        xa(repository.insert(role.groupId, role.description))
    }

    def update(role: Role): F[Int] = {
      info"update ${role.id}, ${role.groupId}, ${role.description}" *>
        xa(repository.update(role))
    }

    def remove(id: PosInt): F[Int] = {
      info"remove $id" *> xa(repository.remove(id))
    }
  }
}
