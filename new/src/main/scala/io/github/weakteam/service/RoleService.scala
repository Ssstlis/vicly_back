package io.github.weakteam.service

import cats.{~>, FlatMap, Monad}
import cats.syntax.functor._
import eu.timepit.refined.types.numeric.PosInt
import fs2.Stream
import io.github.weakteam.model.Role
import io.github.weakteam.repository.RoleRepository
import tofu.logging.{Loggable, LoggingBase, Logs}
import tofu.syntax.logging._

trait RoleService[F[_]] {
  def findAllPaginated(lastKey: Option[PosInt]): F[Stream[F, Role]]
}

object RoleService {

  implicit val posIngLoggable: Loggable[PosInt] = Loggable.intLoggable.contramap[PosInt](_.value)

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
  }
}
