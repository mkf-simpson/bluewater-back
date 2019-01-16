package bluewater.terms

object Slack {
  sealed trait BotRequest
  
  case class Challenge(token: String, challenge: String, `type`: String) extends BotRequest
  
  case class Event(clientMsgId: String, `type`: String, text: String, user: String, ts: String, channel: String, eventTs: String, channelType: String)
  case class Message(token: String, teamId: String, apiAppId: String, event: Event, `type`: String, eventId: String, eventTime: Long, authedUsers: Seq[String]) extends BotRequest
}
