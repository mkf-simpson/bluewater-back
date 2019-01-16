package bluewater

import bluewater.config.Configuration
import bluewater.services._
import bluewater.utils.SSLFix
import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import com.twitter.finagle.Http
import com.twitter.util
import org.slf4j.bridge.SLF4JBridgeHandler
import pureconfig.generic.auto._

object Dependencies {
  
  def nlpService[F[_]: ConcurrentEffect](config: Configuration)(implicit cs: ContextShift[F]): F[NLPService[F]] = for {
    sslContext <- SSLFix.context[F]
    instance <- Async[F].delay(new NLPServiceHydroServingHttpInterpreter[F](config, sslContext))
  } yield instance
  
  def chatService[F[_]: Sync](config: Configuration): F[ChatService[F]] =
    Sync[F].delay(new ChatServiceSlackInterpreter[F](config))
  
  def queueService[F[_]: Sync]: F[QueueService[F]] = for {
    state <- Ref.of[F, Seq[String]](Seq.empty)
    instance <- Sync[F].delay(new QueueServiceInMemoryInterpreter[F](state))
  } yield instance
  
}

object Main extends IOApp with Logging {
  
  import bluewater.utils.TwitterFutureOps._

  def loadConfiguration[F[_] : Sync]: F[Configuration] = Sync[F].delay(pureconfig.loadConfigOrThrow[Configuration])

  def setupLogging[F[_] : Sync]: F[Unit] = Sync[F].delay {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
  }

  def whenTerminated[F[_]](implicit A: Async[F]): F[Unit] = {
    A.async(cb => {
      sys.addShutdownHook(cb(Right(())))
    })
  }

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- setupLogging[IO]
    config <- loadConfiguration[IO]
    _ <- IO(logger.info(config.toString))
    
    nlpService <- Dependencies.nlpService[IO](config)
    chatService <- Dependencies.chatService[IO](config)
    queueInstance <- Dependencies.queueService[IO]

    http <- IO(Http.serve(s"${config.http.host}:${config.http.port}", new HttpService[IO](nlpService, chatService, queueInstance).api))
    _ <- IO(logger.info(s"HTTP server started on ${http.boundAddress}"))
    
    stop <- IO.race(whenTerminated[IO], http.ready2[IO])
    code <- stop match {
      case Left(_) => http.close(util.Duration.Top).to[IO].map(_ => 0)
      case Right(_) => IO.pure(1)
    }
  } yield ExitCode(code)

}
