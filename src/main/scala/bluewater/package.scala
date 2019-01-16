import org.slf4j.{Logger, LoggerFactory}

package object bluewater {
  trait Logging {
    val logger: Logger = LoggerFactory.getLogger(this.getClass)
  }
}
