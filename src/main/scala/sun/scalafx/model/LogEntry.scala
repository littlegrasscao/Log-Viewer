package sun.scalafx.model

import scalafx.beans.property.{IntegerProperty, StringProperty}
import sun.scalafx.config.AppConfig.DateFormats

import java.time.LocalDateTime

/**
 * Represents a single parsed log entry.
 *
 * This is the core data model for the application. Each log line
 * is parsed into a LogEntry with observable properties for UI binding.
 *
 * @param id        Line number in the original file
 * @param timestamp When the log was recorded
 * @param level     Log level (INFO, WARN, ERROR, DEBUG)
 * @param source    Source/service name that generated the log
 * @param message   The actual log message content
 */
case class LogEntry(
  id: Int,
  timestamp: LocalDateTime,
  level: String,
  source: String,
  message: String
) {

  // Observable properties for TableView binding
  val idProperty: IntegerProperty = IntegerProperty(id)
  val timestampProperty: StringProperty = StringProperty(formatTimestamp)
  val levelProperty: StringProperty = StringProperty(level)
  val sourceProperty: StringProperty = StringProperty(source)
  val messageProperty: StringProperty = StringProperty(message)

  /**
   * Formats the timestamp for display.
   * Returns empty string for MIN timestamp (unset/unparseable).
   */
  private def formatTimestamp: String = {
    if (timestamp == LocalDateTime.MIN) "" else timestamp.format(DateFormats.Display)
  }

  /**
   * Creates a copy with the message appended (for continuation lines).
   */
  def appendMessage(additionalLine: String): LogEntry = {
    copy(message = s"$message\n$additionalLine")
  }
}

object LogEntry {

  /**
   * Creates a LogEntry for unparseable lines.
   */
  def unparsed(lineNumber: Int, rawLine: String): LogEntry = {
    LogEntry(lineNumber, LocalDateTime.MIN, "", "", rawLine)
  }

  /**
   * Formats a LogEntry for display in the details pane.
   */
  def formatDetails(entry: Option[LogEntry]): String = entry match {
    case Some(e) =>
      s"""Line: ${e.id}
         |Timestamp: ${e.timestampProperty.value}
         |Level: ${e.level}
         |Source: ${e.source}
         |
         |Message:
         |${e.message}""".stripMargin
    case None => ""
  }
}
