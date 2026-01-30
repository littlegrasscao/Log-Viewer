package sun.scalafx.config

import java.time.format.DateTimeFormatter

/**
 * Application configuration and constants.
 * Centralized location for all configurable values.
 */
object AppConfig {

  /** Application window dimensions and metadata */
  object Window {
    val Width: Int = 1100
    val Height: Int = 800
    val Title: String = "Next-Gen Log Viewer"
  }

  /** Available log levels for filtering */
  val LogLevels: List[String] = List("ALL", "INFO", "WARN", "ERROR", "DEBUG")

  /** Date/time formatters for parsing and display */
  object DateFormats {
    val Display: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val Standard: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    val WithMillis: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS")
    val Short: DateTimeFormatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm:ss")
  }

  /** CSS styles for log level coloring */
  object LevelStyles {
    val Error: String = "-fx-background-color: #ffcccc; -fx-text-fill: #cc0000; -fx-font-weight: bold;"
    val Warn: String = "-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-font-weight: bold;"
    val Info: String = "-fx-background-color: #d4edda; -fx-text-fill: #155724;"
    val Debug: String = "-fx-background-color: #cce5ff; -fx-text-fill: #004085;"
    val Default: String = ""

    /**
     * Returns the CSS style for a given log level.
     */
    def forLevel(level: String): String = level match {
      case "ERROR" => Error
      case "WARN"  => Warn
      case "INFO"  => Info
      case "DEBUG" => Debug
      case _       => Default
    }
  }

  /** Table column widths */
  object ColumnWidths {
    val Id: Int = 60
    val Timestamp: Int = 180
    val Level: Int = 70
    val Source: Int = 220
    val Message: Int = 450
  }

  /** UI styling constants */
  object Styles {
    val FilterBar: String = "-fx-background-color: #f8f8f8; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;"
    val StatusBar: String = "-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;"
    val DetailsLabel: String = "-fx-font-weight: bold; -fx-font-size: 14px;"
    val FilePathLabel: String = "-fx-text-fill: #888888; -fx-font-size: 11px;"
    val WelcomeTitle: String = "-fx-font-size: 24px; -fx-font-weight: bold;"
    val WelcomeSubtitle: String = "-fx-font-size: 14px; -fx-text-fill: #666666;"
  }
}
