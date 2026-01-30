package sun.scalafx.model

import scalafx.beans.property.IntegerProperty
import scalafx.collections.ObservableBuffer

/**
 * Holds all state for a single log file tab.
 *
 * Each tab is completely independent with its own entries, filters,
 * highlights, and UI bindings. This enables viewing multiple log files
 * simultaneously with separate settings for each.
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
  // Highlight State (per-tab keyword highlighting)
  // ============================================================================

  /** Words to highlight in this tab (case-insensitive matching) */
  val highlightWords: ObservableBuffer[String] = ObservableBuffer[String]()

  /** When true, only show entries that match highlight words */
  var showHighlightedOnly: Boolean = false

  // ============================================================================
  // Methods
  // ============================================================================

  /**
   * Applies current filters to log entries.
   * Filters by level, search text, and optionally highlight words (case-insensitive).
   */
  def applyFilters(): Unit = {
    filteredEntries.clear()
    val searchLower = searchText.toLowerCase

    logEntries.foreach { entry =>
      val matchesLevel = levelFilter == "ALL" || entry.level == levelFilter
      val matchesSearch = searchLower.isEmpty ||
        entry.message.toLowerCase.contains(searchLower) ||
        entry.source.toLowerCase.contains(searchLower)
      val matchesHighlight = !showHighlightedOnly || isHighlighted(entry)

      if (matchesLevel && matchesSearch && matchesHighlight) {
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
    // Note: highlights are preserved when reloading a file
  }

  /**
   * Adds a log entry and increments the line counter.
   */
  def addEntry(entry: LogEntry): Unit = {
    logEntries += entry
  }

  // ============================================================================
  // Highlight Methods
  // ============================================================================

  /**
   * Adds a word to the highlight list (case-insensitive, no duplicates).
   * @param word The word to highlight
   * @return true if added, false if already exists
   */
  def addHighlight(word: String): Boolean = {
    val trimmed = word.trim
    if (trimmed.nonEmpty && !highlightWords.exists(_.equalsIgnoreCase(trimmed))) {
      highlightWords += trimmed
      true
    } else {
      false
    }
  }

  /**
   * Removes a word from the highlight list.
   * @param word The word to remove
   */
  def removeHighlight(word: String): Unit = {
    highlightWords.filterInPlace(!_.equalsIgnoreCase(word.trim))
  }

  /**
   * Clears all highlight words.
   */
  def clearHighlights(): Unit = {
    highlightWords.clear()
  }

  /**
   * Checks if a log entry matches any highlight word.
   * Returns the index of the first matching highlight word (for color selection),
   * or None if no match.
   *
   * @param entry The log entry to check
   * @return Option containing the highlight index, or None
   */
  def getHighlightIndex(entry: LogEntry): Option[Int] = {
    val sourceLower = entry.source.toLowerCase
    val messageLower = entry.message.toLowerCase

    highlightWords.zipWithIndex.collectFirst {
      case (word, idx) if sourceLower.contains(word.toLowerCase) ||
                          messageLower.contains(word.toLowerCase) => idx
    }
  }

  /**
   * Checks if a log entry matches any highlight word.
   */
  def isHighlighted(entry: LogEntry): Boolean = getHighlightIndex(entry).isDefined
}
