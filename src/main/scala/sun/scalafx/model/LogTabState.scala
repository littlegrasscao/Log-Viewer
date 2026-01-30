package sun.scalafx.model

import scalafx.beans.property.IntegerProperty
import scalafx.collections.ObservableBuffer

/**
 * Holds all state for a single log file tab.
 *
 * Each tab is completely independent with its own entries, filters,
 * and UI bindings. This enables viewing multiple log files simultaneously
 * with separate filter settings for each.
 *
 * @param fileName The name of the loaded file (used for tab title)
 * @param filePath Full path to the file
 */
class LogTabState(val fileName: String, val filePath: String) {

  // ============================================================================
  // Log Entry Collections
  // ============================================================================

  /** All parsed log entries for this tab */
  val logEntries: ObservableBuffer[LogEntry] = ObservableBuffer[LogEntry]()

  /** Filtered entries displayed in the table */
  val filteredEntries: ObservableBuffer[LogEntry] = ObservableBuffer[LogEntry]()

  // ============================================================================
  // Observable Counts (for status bar binding)
  // ============================================================================

  val totalCount: IntegerProperty = IntegerProperty(0)
  val filteredCount: IntegerProperty = IntegerProperty(0)

  // Setup count bindings - update when entries change
  logEntries.onChange { (_, _) => totalCount.value = logEntries.size }
  filteredEntries.onChange { (_, _) => filteredCount.value = filteredEntries.size }

  // ============================================================================
  // Filter State
  // ============================================================================

  /** Current log level filter ("ALL" shows everything) */
  var levelFilter: String = "ALL"

  /** Current search text (empty shows everything) */
  var searchText: String = ""

  // ============================================================================
  // Methods
  // ============================================================================

  /**
   * Applies current filters to log entries.
   * Filters by level and search text (case-insensitive).
   */
  def applyFilters(): Unit = {
    filteredEntries.clear()
    val searchLower = searchText.toLowerCase

    logEntries.foreach { entry =>
      val matchesLevel = levelFilter == "ALL" || entry.level == levelFilter
      val matchesSearch = searchLower.isEmpty ||
        entry.message.toLowerCase.contains(searchLower) ||
        entry.source.toLowerCase.contains(searchLower)

      if (matchesLevel && matchesSearch) {
        filteredEntries += entry
      }
    }
  }

  /**
   * Appends a continuation line to the previous log entry.
   * Used for multi-line log messages and stack traces.
   */
  def appendToPreviousEntry(line: String): Unit = {
    if (logEntries.nonEmpty) {
      val last = logEntries.last
      val updated = last.appendMessage(line)
      logEntries.update(logEntries.size - 1, updated)
    }
  }

  /**
   * Clears all entries (called before loading a new file).
   */
  def clear(): Unit = {
    logEntries.clear()
    filteredEntries.clear()
    levelFilter = "ALL"
    searchText = ""
  }

  /**
   * Adds a log entry and increments the line counter.
   */
  def addEntry(entry: LogEntry): Unit = {
    logEntries += entry
  }
}
