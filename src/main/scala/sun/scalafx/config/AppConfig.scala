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

  /** Highlight colors for keyword highlighting (up to 8 distinct colors) */
  object HighlightColors {
    // Background colors for highlighted rows (soft muted shades)
    val Colors: List[String] = List(
      "#FFF9C4",  // Soft Yellow
      "#E3F2FD",  // Soft Blue
      "#E8F5E9",  // Soft Green
      "#FBE9E7",  // Soft Peach
      "#F3E5F5",  // Soft Lavender
      "#E0F7FA",  // Soft Cyan
      "#FCE4EC",  // Soft Pink
      "#F1F8E9"   // Soft Lime
    )

    // Muted colors for chip/badge display
    val ChipColors: List[String] = List(
      "#F9A825",  // Muted Yellow
      "#1976D2",  // Muted Blue
      "#43A047",  // Muted Green
      "#E65100",  // Muted Orange
      "#8E24AA",  // Muted Purple
      "#00838F",  // Muted Cyan
      "#D81B60",  // Muted Pink
      "#7CB342"   // Muted Lime
    )

    /** Get row highlight style for a given highlight index */
    def rowStyle(index: Int, isSelected: Boolean = false): String = {
      val bgColor = Colors(index % Colors.size)
      if (isSelected) {
        // When selected: use a slightly darker shade and ensure dark text
        val selectedBg = SelectedColors(index % SelectedColors.size)
        s"-fx-background-color: $selectedBg; -fx-control-inner-background: $selectedBg; -fx-selection-bar: $selectedBg; -fx-selection-bar-non-focused: $selectedBg;"
      } else {
        s"-fx-background-color: $bgColor;"
      }
    }

    // Slightly darker colors for selected highlighted rows
    val SelectedColors: List[String] = List(
      "#FFF59D",  // Darker Yellow
      "#BBDEFB",  // Darker Blue
      "#C8E6C9",  // Darker Green
      "#FFCCBC",  // Darker Peach
      "#E1BEE7",  // Darker Lavender
      "#B2EBF2",  // Darker Cyan
      "#F8BBD9",  // Darker Pink
      "#DCEDC8"   // Darker Lime
    )

    /** Get chip style for a given highlight index */
    def chipStyle(index: Int): String = {
      val color = ChipColors(index % ChipColors.size)
      s"-fx-background-color: $color; -fx-text-fill: white; -fx-padding: 3 8 3 8; -fx-background-radius: 10;"
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
