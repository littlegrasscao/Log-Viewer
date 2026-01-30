package sun.scalafx.ui

import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Orientation, Pos}
import scalafx.scene.control._
import scalafx.scene.layout._
import sun.scalafx.config.AppConfig
import sun.scalafx.config.AppConfig.{ColumnWidths, LevelStyles, Styles}
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
  // Log Table Components
  // ============================================================================

  /**
   * Creates the log entries table with all columns.
   *
   * @param tabState The tab state containing entries to display
   * @return Configured TableView
   */
  def createLogTable(tabState: LogTabState): TableView[LogEntry] = new TableView[LogEntry] {
    items = tabState.filteredEntries
    placeholder = new Label("No log entries loaded.")

    columns ++= List(
      createIdColumn(),
      createTimestampColumn(),
      createLevelColumn(),
      createSourceColumn(),
      createMessageColumn()
    )

    selectionModel().selectionMode = SelectionMode.Single
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

  private def createSourceColumn(): TableColumn[LogEntry, String] = {
    new TableColumn[LogEntry, String] {
      text = "Source"
      prefWidth = ColumnWidths.Source
      sortable = false
      cellValueFactory = _.value.sourceProperty
    }
  }

  private def createMessageColumn(): TableColumn[LogEntry, String] = {
    new TableColumn[LogEntry, String] {
      text = "Message"
      prefWidth = ColumnWidths.Message
      sortable = false
      cellValueFactory = _.value.messageProperty
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
