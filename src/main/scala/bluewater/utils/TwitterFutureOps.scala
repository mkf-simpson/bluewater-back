package bluewater.utils

import cats.effect.Effect
import com.twitter.util

object TwitterFutureOps {

  implicit class TwitterFToEff[A](val f: util.Future[A]) extends AnyVal {
    def to[F[_]](implicit F: Effect[F]): F[A] = {
      F.async(cb => {
        f.respond {
          case util.Return(a) => cb(Right(a))
          case util.Throw(e) => cb(Left(e))
        }
      })
    }
  }

  implicit class TwitterAwaitToF[A <: util.Awaitable[_]](val a: A) extends AnyVal {
    def ready2[F[_]](duration: util.Duration)(implicit F: Effect[F]): F[A] = {
      F.delay(util.Await.ready(a, duration))
    }

    def ready2[F[_]](implicit F: Effect[F]): F[A] = {
      F.delay(util.Await.ready(a))
    }
  }
}
