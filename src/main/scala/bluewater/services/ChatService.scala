package bluewater.services

import bluewater.Logging
import bluewater.config.Configuration
import cats.effect.Sync
import cats.implicits._
import com.github.seratch.jslack.Slack
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest

trait ChatService[F[_]] {
  def sendMessage(message: String, chatId: String): F[Unit]
}

class ChatServiceSlackInterpreter[F[_]: Sync](config: Configuration) extends ChatService[F] with Logging {
  override def sendMessage(message: String, chatId: String): F[Unit] = {
    val slack = Slack.getInstance()
    val request = ChatPostMessageRequest.builder()
      .token(config.slack.apiToken)
      .channel(chatId)
      .text(message)
      .build()
    for {
      response <- Sync[F].delay(slack.methods().chatPostMessage(request))
      _ <- if (!response.isOk)
             Sync[F].delay(logger.error(s"Can't send message to slack: ${response.getError}"))
           else
             Sync[F].unit
    } yield Unit
  }
}
