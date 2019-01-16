package bluewater.terms

import enumeratum._
import scala.collection.immutable

sealed trait Intent extends EnumEntry
object Intent extends Enum[Intent] with CirceEnum[Intent] {
  val values: immutable.IndexedSeq[Intent] = findValues

  case object Book extends Intent
  case object OMG extends Intent
  case object CheckStatus extends Intent
  case object GoToHell extends Intent
  case object Fail extends Intent
}