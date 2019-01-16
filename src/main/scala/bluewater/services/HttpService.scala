package bluewater.services

import bluewater.terms.{Intent, SensorData, Slack}
import bluewater.Logging
import cats.data.NonEmptyList
import cats.effect.Effect
import cats.implicits._
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.{Configuration => CirceExtraConfiguration}
import io.circe.generic.extras.auto._
import io.finch._
import io.finch.circe._
import shapeless.{Coproduct, Generic}

import scala.util.control.NonFatal

//noinspection TypeAnnotation
class HttpService[F[_]: Effect](nlpService: NLPService[F], chatService: ChatService[F], queueService: QueueService[F]) extends Endpoint.Module[F] with Logging {
  
  implicit val genDevConfig: CirceExtraConfiguration =
    CirceExtraConfiguration.default.withDefaults.withSnakeCaseMemberNames.withDiscriminator("type")
  
  implicit def encodeAdt[A, Repr <: Coproduct](implicit gen: Generic.Aux[A, Repr], encodeRepr: Encoder[Repr]): Encoder[A] = encodeRepr.contramap(gen.to)
  implicit def decodeAdt[A, Repr <: Coproduct](implicit gen: Generic.Aux[A, Repr], decodeRepr: Decoder[Repr]): Decoder[A] = decodeRepr.map(gen.from)

  def encodeErrorList(es: NonEmptyList[Exception]): Json = {
    val messages = es.map(x => Json.fromString(x.getMessage)).toList
    Json.obj("errors" -> Json.arr(messages: _*))
  }

  implicit val encodeException: Encoder[Exception] = Encoder.instance({
    case e: io.finch.Errors => encodeErrorList(e.errors)
    case e: io.finch.Error =>
      e.getCause match {
        case e: io.circe.Errors => encodeErrorList(e.errors)
        case _ => encodeErrorList(NonEmptyList.one(e))
      }
    case e: Exception => encodeErrorList(NonEmptyList.one(e))
  })

  def healthCheck = get("health") {
    Ok("ok").pure[F]
  }
  
  def sensor = post("sensor" :: jsonBody[SensorData]) { sensor: SensorData =>
    logger.info(s"$sensor")
    Ok("ok").pure[F]
  }
  
  def slack = post("slack" :: jsonBody[Slack.BotRequest]) { msg: Slack.BotRequest=>
    msg match {
      case Slack.Challenge(_, challenge, _) => Ok(challenge).pure[F]
      case m: Slack.Message => for {
        intent <- nlpService.getIntent(m.event.text).recover {
          case exc: Throwable =>
            logger.error(s"Error while getting intent: $exc")
            NLPResult(Intent.Fail, 100d)
        }
        _ <- Effect[F].delay(logger.info(s"${m.event.text} -> $intent"))
        _ <- intent.intent match {
          case Intent.Book => queueService.add(m.event.user)
          case _ => Effect[F].unit
        }
        _ <- chatService.sendMessage(s"<@${m.event.user}> ваше сообщение было распознано как ${intent.intent} с вероятностью ${intent.confidence}", m.event.channel)
      } yield Ok("ok")
      case _ => for {
        _ <- Effect[F].delay(logger.info("Fucken BotRequest"))
      } yield Ok("ok")
    }
  }
  
  def endpoints = (healthCheck :+: sensor) handle {
    case e: io.finch.Error.NotParsed =>
      logger.warn(s"Can't parse json with message: ${e.getMessage()}")
      BadRequest(new RuntimeException(e))
    case NonFatal(e) =>
      logger.error(e.getLocalizedMessage, e)
      InternalServerError(new RuntimeException(e))
  }

  def api: Service[Request, Response] = Bootstrap
    .configure(negotiateContentType = true, enableMethodNotAllowed = true)
    .serve[Application.Json](endpoints)
    .serve[Text.Plain](slack)
    .toService
}
