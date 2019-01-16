package bluewater.services

import cats.implicits._
import cats.effect.Sync
import cats.effect.concurrent.Ref

trait QueueService[F[_]] {
  def add(id: String): F[Unit]
  def dequeue: F[Unit]
}

class QueueServiceInMemoryInterpreter[F[_]: Sync](ref: Ref[F, Seq[String]]) extends QueueService[F] {
  override def add(id: String): F[Unit] = for {
    state <- ref.get
    _ <- ref.set(id +: state)
  } yield Unit

  override def dequeue: F[Unit] = for {
    state <- ref.get
    _ <- ref.set(state.tail)
  } yield Unit
}
