package sun.scalafx.ui

import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Orientation, Pos}
import scalafx.scene.control._
import scalafx.scene.input.{Clipboard, ClipboardContent}
import scalafx.scene.layout._
import sun.scalafx.config.AppConfig
import sun.scalafx.config.AppConfig.{ColumnWidths, HighlightColors, LevelStyles, Styles}
import sun.scalafx.model.{LogEntry, LogTabState}

/**
 * Factory for creating UI components.
 *
 * Centralizes UI component creation for consistency and reusability.
 * All components are created with proper styling and bindings.
 */
object UIComponents {

  // ============================================================================
  // Filter Bar Components
  // ============================================================================

  /**
   * Creates the filter/search bar for a tab.
   *
   * @param tabState The tab state to bind filters to
   * @return HBox containing filter controls
   */
  def createFilterBar(tabState: LogTabState): HBox = {
    val levelLabel = new Label("Filter Level:")

    val levelFilter = new ComboBox[String] {
      items = ObservableBuffer.from(AppConfig.LogLevels)
      value = "ALL"
      prefWidth = 100
      onAction = (_: ActionEvent) => {
        tabState.levelFilter = value.value
        tabState.applyFilters()
      }
    }

    val searchField = new TextField {
      promptText = "Search logs..."
      prefWidth = 300
      onKeyReleased = _ => {
        tabState.searchText = text.value
        tabState.applyFilters()
      }
    }

    val clearButton = new Button("Clear") {
      onAction = (_: ActionEvent) => {
        searchField.text = ""
        tabState.searchText = ""
        tabState.applyFilters()
      }
    }

    new HBox {
      padding = Insets(8, 10, 8, 10)
      spacing = 10
      alignment = Pos.CenterLeft
      style = Styles.FilterBar
      children = Seq(
        levelLabel,
        levelFilter,
        new Separator { orientation = Orientation.Vertical },
        new Label("Search:"),
        searchField,
        clearButton
      )
    }
  }

  // ============================================================================
  // Highlight Bar Components
  // ============================================================================

  /**
   * Creates the highlight bar showing current highlight words as colored chips.
   * Each chip can be clicked to remove the highlight.
   *
   * @param tabState The tab state containing highlight words
   * @param refreshTable Callback to refresh table display after highlight changes
   * @return HBox containing highlight controls
   */
  def createHighlightBar(tabState: LogTabState, refreshTable: () => Unit): HBox = {
    val highlightLabel = new Label("Highlights:")

    // Container for highlight chips (rebuilt when highlights change)
    val chipsContainer = new HBox {
      spacing = 6
      alignment = Pos.CenterLeft
    }

    // Text field to manually add highlight words
    val addHighlightField = new TextField {
      promptText = "Add word..."
      prefWidth = 120
      onAction = (_: ActionEvent) => {
        if (text.value.trim.nonEmpty) {
          tabState.addHighlight(text.value)
          text = ""
          refreshTable()
        }
      }
    }

    val addButton = new Button("+") {
      onAction = (_: ActionEvent) => {
        if (addHighlightField.text.value.trim.nonEmpty) {
          tabState.addHighlight(addHighlightField.text.value)
          addHighlightField.text = ""
          refreshTable()
        }
      }
    }

    // Toggle button for showing only highlighted entries
    val showOnlyButton: ToggleButton = new ToggleButton("Highlighted Only") {
      selected = false
      style = "-fx-font-size: 11px;"
      tooltip = new Tooltip("Toggle to show only highlighted rows")
      onAction = (_: ActionEvent) => {
        tabState.showHighlightedOnly = selected.value
        tabState.applyFilters()
        refreshTable()
      }
    }

    val clearAllButton = new Button("Clear All") {
      onAction = (_: ActionEvent) => {
        tabState.clearHighlights()
        tabState.showHighlightedOnly = false
        showOnlyButton.selected = false
        tabState.applyFilters()
        refreshTable()
      }
    }

    // Function to rebuild chip display
    def rebuildChips(): Unit = {
      chipsContainer.children.clear()
      tabState.highlightWords.zipWithIndex.foreach { case (word, idx) =>
        val chip = new Label(s"$word  \u00D7") {  // × symbol for remove
          style = HighlightColors.chipStyle(idx)
          cursor = scalafx.scene.Cursor.Hand
          onMouseClicked = _ => {
            tabState.removeHighlight(word)
            // If no more highlights, turn off "highlighted only" mode
            if (tabState.highlightWords.isEmpty) {
              tabState.showHighlightedOnly = false
              showOnlyButton.selected = false
            }
            tabState.applyFilters()
            refreshTable()
          }
        }
        // Add tooltip explaining how to remove
        chip.tooltip = new Tooltip(s"Click to remove '$word'")
        chipsContainer.children.add(chip)
      }
    }

    // Rebuild chips when highlight words change
    tabState.highlightWords.onChange { (_, _) =>
      rebuildChips()
    }

    // Initial build
    rebuildChips()

    new HBox {
      padding = Insets(6, 10, 6, 10)
      spacing = 10
      alignment = Pos.CenterLeft
      style = "-fx-background-color: #f0f5ff; -fx-border-color: #d0d8e8; -fx-border-width: 0 0 1 0;"
      children = Seq(
        highlightLabel,
        chipsContainer,
        new Region { hgrow = Priority.Always },
        showOnlyButton,
        new Separator { orientation = Orientation.Vertical },
        addHighlightField,
        addButton,
        new Separator { orientation = Orientation.Vertical },
        clearAllButton
      )
    }
  }

  // ============================================================================
  // Log Table Components
  // ============================================================================

  /**
   * Creates the log entries table with all columns and row highlighting support.
   *
   * @param tabState The tab state containing entries to display
   * @return Configured TableView
   */
  def createLogTable(tabState: LogTabState): TableView[LogEntry] = {
    val table = new TableView[LogEntry] {
      items = tabState.filteredEntries
      placeholder = new Label("No log entries loaded.")

      columns ++= List(
        createIdColumn(),
        createTimestampColumn(),
        createLevelColumn(),
        createSourceColumn(tabState),
        createMessageColumn(tabState)
      )

      selectionModel().selectionMode = SelectionMode.Single

      // Row factory for applying highlight colors
      rowFactory = { _ =>
        new TableRow[LogEntry] {
          item.onChange { (_, _, entry) =>
            updateRowStyle(entry)
          }

          // Also update when highlight words change
          tabState.highlightWords.onChange { (_, _) =>
            updateRowStyle(item.value)
          }

          // Update style when selection changes to maintain readability
          selected.onChange { (_, _, _) =>
            updateRowStyle(item.value)
          }

          private def updateRowStyle(entry: LogEntry): Unit = {
            if (entry != null) {
              tabState.getHighlightIndex(entry) match {
                case Some(idx) => style = HighlightColors.rowStyle(idx, selected.value)
                case None      => style = ""
              }
            } else {
              style = ""
            }
          }
        }
      }
    }

    table
  }

  /**
   * Refreshes the table display by triggering a re-render.
   * Call this after highlight changes to update row colors.
   */
  def refreshTableHighlights(table: TableView[LogEntry]): Unit = {
    // Force refresh by toggling items
    val currentItems = table.items.value
    table.items = null
    table.items = currentItems
  }

  private def createIdColumn(): TableColumn[LogEntry, Number] = {
    new TableColumn[LogEntry, Number] {
      text = "Line"
      prefWidth = ColumnWidths.Id
      sortable = false
      cellValueFactory = { data =>
        data.value.idProperty.asInstanceOf[ObservableValue[Number, Number]]
      }
    }
  }

  private def createTimestampColumn(): TableColumn[LogEntry, String] = {
    new TableColumn[LogEntry, String] {
      text = "Timestamp"
      prefWidth = ColumnWidths.Timestamp
      sortable = true
      cellValueFactory = _.value.timestampProperty
    }
  }

  private def createLevelColumn(): TableColumn[LogEntry, String] = {
    new TableColumn[LogEntry, String] {
      text = "Level"
      prefWidth = ColumnWidths.Level
      sortable = false
      cellValueFactory = _.value.levelProperty

      // Apply color styling based on log level
      cellFactory = { _: TableColumn[LogEntry, String] =>
        new TableCell[LogEntry, String] {
          item.onChange { (_, _, newValue) =>
            if (newValue != null && newValue.nonEmpty) {
              text = newValue
              style = LevelStyles.forLevel(newValue)
            } else {
              text = ""
              style = ""
            }
          }
        }
      }
    }
  }

  private def createSourceColumn(tabState: LogTabState): TableColumn[LogEntry, String] = {
    new TableColumn[LogEntry, String] {
      text = "Source"
      prefWidth = ColumnWidths.Source
      sortable = false
      cellValueFactory = _.value.sourceProperty

      // Cell factory with context menu for highlighting
      cellFactory = { _: TableColumn[LogEntry, String] =>
        createHighlightableCell(tabState, _.source)
      }
    }
  }

  private def createMessageColumn(tabState: LogTabState): TableColumn[LogEntry, String] = {
    new TableColumn[LogEntry, String] {
      text = "Message"
      prefWidth = ColumnWidths.Message
      sortable = false
      cellValueFactory = _.value.messageProperty

      // Cell factory with context menu for highlighting
      cellFactory = { _: TableColumn[LogEntry, String] =>
        createHighlightableCell(tabState, _.message)
      }
    }
  }

  /**
   * Creates a table cell with context menu for adding highlights.
   * Supports selecting text and adding it to highlights.
   */
  private def createHighlightableCell(
    tabState: LogTabState,
    textExtractor: LogEntry => String
  ): TableCell[LogEntry, String] = {
    new TableCell[LogEntry, String] {
      item.onChange { (_, _, newValue) =>
        if (newValue != null) {
          text = newValue
        } else {
          text = ""
        }
      }

      // Context menu for highlight actions
      contextMenu = new ContextMenu {
        items ++= Seq(
          new MenuItem("Add to Highlights") {
            onAction = (_: ActionEvent) => {
              val selectedText = getSelectedTextOrCellText()
              if (selectedText.nonEmpty) {
                tabState.addHighlight(selectedText)
                // Table will auto-refresh via observable binding
              }
            }
          },
          new MenuItem("Copy") {
            onAction = (_: ActionEvent) => {
              val selectedText = getSelectedTextOrCellText()
              if (selectedText.nonEmpty) {
                val clipboard = Clipboard.systemClipboard
                val content = new ClipboardContent()
                content.putString(selectedText)
                clipboard.setContent(content)
              }
            }
          }
        )
      }

      private def getSelectedTextOrCellText(): String = {
        // If there's selected text in the scene, use that; otherwise use cell text
        Option(text.value).getOrElse("")
      }
    }
  }

  // ============================================================================
  // Details Pane
  // ============================================================================

  /**
   * Creates the details pane for viewing full log entry information.
   *
   * @param logTable The table to watch for selection changes
   * @return VBox containing the details view
   */
  def createDetailsPane(logTable: TableView[LogEntry]): VBox = {
    val detailsLabel = new Label("Log Details") {
      style = Styles.DetailsLabel
    }

    val detailsArea = new TextArea {
      editable = false
      wrapText = true
      prefHeight = 150
      promptText = "Select a log entry to view details..."
    }

    // Update details when selection changes
    logTable.selectionModel().selectedItemProperty().onChange { (_, _, selected) =>
      detailsArea.text = LogEntry.formatDetails(Option(selected))
    }

    new VBox {
      padding = Insets(10)
      spacing = 5
      children = Seq(detailsLabel, detailsArea)
      VBox.setVgrow(detailsArea, Priority.Always)
    }
  }

  // ============================================================================
  // Status Bar
  // ============================================================================

  /**
   * Creates the status bar showing entry counts and file path.
   *
   * @param tabState The tab state to display counts from
   * @return HBox containing status information
   */
  def createStatusBar(tabState: LogTabState): HBox = {
    val countLabel = new Label()

    countLabel.text <== Bindings.createStringBinding(
      () => s"Showing ${tabState.filteredCount.value} of ${tabState.totalCount.value} entries",
      tabState.filteredCount,
      tabState.totalCount
    )

    val filePathLabel = new Label(tabState.filePath) {
      style = Styles.FilePathLabel
    }

    new HBox {
      padding = Insets(6, 15, 6, 15)
      spacing = 20
      alignment = Pos.CenterLeft
      style = Styles.StatusBar
      children = Seq(countLabel, new Region { hgrow = Priority.Always }, filePathLabel)
    }
  }

  // ============================================================================
  // Welcome Tab Content
  // ============================================================================

  /**
   * Creates the welcome tab content shown when no files are open.
   *
   * @param onOpenFile Callback to invoke when open button is clicked
   * @return VBox containing welcome content
   */
  def createWelcomeContent(onOpenFile: () => Unit): VBox = {
    new VBox {
      alignment = Pos.Center
      spacing = 20
      padding = Insets(50)
      children = Seq(
        new Label("Next-Gen Log Viewer") {
          style = Styles.WelcomeTitle
        },
        new Label("Open a log file to get started") {
          style = Styles.WelcomeSubtitle
        },
        new Button("Open Log File") {
          style = "-fx-font-size: 14px;"
          onAction = (_: ActionEvent) => onOpenFile()
        }
      )
    }
  }
}
