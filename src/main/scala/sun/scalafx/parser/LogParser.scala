package sun.scalafx.parser

import sun.scalafx.config.AppConfig.DateFormats
import sun.scalafx.model.LogEntry

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

/**
 * Log file parser supporting multiple log formats.
 *
 * Parses log lines into LogEntry objects using pattern matching
 * against various known log formats. Supports:
 *   - Standard timestamp formats
 *   - Millisecond precision timestamps
 *   - Cluster prefix logs
 *   - Driver logs
 */
object LogParser {

  // ============================================================================
  // Regular Expression Patterns
  // ============================================================================

  /**
   * Collection of regex patterns for different log formats.
   * Each pattern captures: timestamp, level, source/service, and message.
   */
  private object Patterns {

    /** Standard format: 2025/02/25 06:28:24 WARN Service$ File.scala:144 [attrs]: message */
    val Standard =
      """(\d{4}/\d{2}/\d{2}\s\d{2}:\d{2}:\d{2})\s+([A-Z]+)\s+([a-zA-Z0-9$\-:]+)\s+([a-zA-Z0-9._:-]+)?\s*(\[.*?\][a-zA-Z0-9._:-]?)?\s*:\s+(.*)""".r

    /** Complex service name: 2025/02/25 06:28:24 INFO Service[extra] File [attrs]: message */
    val ComplexService =
      """(\d{4}/\d{2}/\d{2}\s\d{2}:\d{2}:\d{2})\s+([A-Z]+)\s+([a-zA-Z0-9$\-:]+)(\[.*?\])\s+([a-zA-Z0-9._:-]+)?\s*(\[.*?\])?\s*:\s+(.*)""".r

    /** With milliseconds: 2025/02/25 06:28:24.123 INFO Service[extra] File [attrs]: message */
    val WithMillis =
      """(\d{4}/\d{2}/\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s([A-Z]+)\s([a-zA-Z0-9$\-:._]+)(\[.*?\])\s*([a-zA-Z0-9._:-]+)?\s*(\[.*?\])?\s*:\s+(.*)""".r

    /** With milliseconds and brackets: 2025/02/25 06:28:24.123 INFO Service [file]: message */
    val WithMillisBracket =
      """(\d{4}/\d{2}/\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s([A-Z]+)\s([a-zA-Z0-9$\-:._]+)\s*(\[.*?\])\s*:\s+(.*)""".r

    /** Cluster prefix: [cluster-id] 25/02/25 06:28:24 INFO Service: message */
    val ClusterPrefix =
      """(\[.*?\])\s+(\d{2}/\d{2}/\d{2}\s\d{2}:\d{2}:\d{2})\s+([A-Z]+)\s+([a-zA-Z0-9$\-:]+):\s+(.*)""".r

    /** Driver log: 25/02/25 06:28:24 INFO Service: message */
    val DriverLog =
      """(\d{2}/\d{2}/\d{2}\s\d{2}:\d{2}:\d{2})\s+([A-Z]+)\s+([a-zA-Z0-9$\-:]+)\s*:\s+(.*)""".r
  }

  // ============================================================================
  // Public API
  // ============================================================================

  /**
   * Attempts to parse a log line using all known patterns.
   *
   * @param line       The raw log line to parse
   * @param lineNumber The line number in the file (for reference)
   * @return Some(LogEntry) if parsing succeeded, None otherwise
   */
  def parseLine(line: String, lineNumber: Int): Option[LogEntry] = {
    parseStandardFormat(line, lineNumber)
      .orElse(parseComplexServiceFormat(line, lineNumber))
      .orElse(parseWithMillisFormat(line, lineNumber))
      .orElse(parseWithMillisBracketFormat(line, lineNumber))
      .orElse(parseClusterPrefixFormat(line, lineNumber))
      .orElse(parseDriverLogFormat(line, lineNumber))
  }

  /**
   * Checks if a line looks like a continuation (non-log line).
   * Continuation lines typically start with whitespace or are stack traces.
   */
  def isContinuationLine(line: String): Boolean = {
    line.startsWith("\t") ||
      line.startsWith("    ") ||
      line.startsWith("at ") ||
      line.startsWith("Caused by:") ||
      line.trim.startsWith("...")
  }

  // ============================================================================
  // Pattern-Specific Parsers
  // ============================================================================

  private def parseStandardFormat(line: String, lineNum: Int): Option[LogEntry] = {
    line match {
      case Patterns.Standard(ts, level, service, _, _, message) =>
        parseTimestamp(ts, DateFormats.Standard)
          .map(t => LogEntry(lineNum, t, level, service, message))
      case _ => None
    }
  }

  private def parseComplexServiceFormat(line: String, lineNum: Int): Option[LogEntry] = {
    line match {
      case Patterns.ComplexService(ts, level, service, _, _, _, message) =>
        parseTimestamp(ts, DateFormats.Standard)
          .map(t => LogEntry(lineNum, t, level, service, message))
      case _ => None
    }
  }

  private def parseWithMillisFormat(line: String, lineNum: Int): Option[LogEntry] = {
    line match {
      case Patterns.WithMillis(ts, level, service, _, _, _, message) =>
        parseTimestamp(ts, DateFormats.WithMillis)
          .map(t => LogEntry(lineNum, t, level, service, message))
      case _ => None
    }
  }

  private def parseWithMillisBracketFormat(line: String, lineNum: Int): Option[LogEntry] = {
    line match {
      case Patterns.WithMillisBracket(ts, level, service, _, message) =>
        parseTimestamp(ts, DateFormats.WithMillis)
          .map(t => LogEntry(lineNum, t, level, service, message))
      case _ => None
    }
  }

  private def parseClusterPrefixFormat(line: String, lineNum: Int): Option[LogEntry] = {
    line match {
      case Patterns.ClusterPrefix(cluster, ts, level, service, message) =>
        parseTimestamp(ts, DateFormats.Short)
          .map(t => LogEntry(lineNum, t, level, s"$cluster $service", message))
      case _ => None
    }
  }

  private def parseDriverLogFormat(line: String, lineNum: Int): Option[LogEntry] = {
    line match {
      case Patterns.DriverLog(ts, level, service, message) =>
        parseTimestamp(ts, DateFormats.Short)
          .map(t => LogEntry(lineNum, t, level, service, message))
      case _ => None
    }
  }

  // ============================================================================
  // Utility Methods
  // ============================================================================

  /**
   * Safely parses a timestamp string using the given formatter.
   *
   * @param str       The timestamp string to parse
   * @param formatter The DateTimeFormatter to use
   * @return Some(LocalDateTime) if parsing succeeded, None otherwise
   */
  private def parseTimestamp(str: String, formatter: DateTimeFormatter): Option[LocalDateTime] = {
    Try(LocalDateTime.parse(str, formatter)).toOption
  }
}
