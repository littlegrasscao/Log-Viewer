package sun.scalafx

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.geometry.{Insets, Pos}
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scalafx.beans.property.{IntegerProperty, StringProperty}
import scalafx.stage.FileChooser
import scalafx.event.ActionEvent
import scalafx.beans.binding.Bindings
import scalafx.beans.value.ObservableValue

import java.io.File
import scala.io.Source
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

object LogViewer extends JFXApp3 {

  // Model for log entries
  case class LogEntry(id: Int, timestamp: LocalDateTime, level: String, source: String, message: String) {
    val idProperty = IntegerProperty(id)
    val timestampProperty = StringProperty(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")))
    val levelProperty = StringProperty(level)
    val sourceProperty = StringProperty(source)
    val messageProperty = StringProperty(message)
  }

  // Log levels for filtering
  val LogLevels = List("ALL", "INFO", "WARN", "ERROR", "DEBUG")

  // Observable buffer to hold log entries
  val logEntries = ObservableBuffer[LogEntry]()
  val filteredEntries = ObservableBuffer[LogEntry]()

  // Properties to track counts for binding
  val logEntriesCount = IntegerProperty(0)
  val filteredEntriesCount = IntegerProperty(0)

  // Current filter settings
  var currentLevelFilter: String = "ALL"
  var currentSearchText: String = ""

  override def start(): Unit = {
    stage = new PrimaryStage {
      title = "Log Viewer"
      width = 900
      height = 700

      scene = new Scene {
        root = createUI()
      }
    }
  }

  def createUI(): BorderPane = {
    // Main layout
    val borderPane = new BorderPane()

    // Top toolbar
    val toolbar = new ToolBar()

    val openButton = new Button("Open Log File") {
      onAction = (_: ActionEvent) => openLogFile()
    }

    val levelFilter = new ComboBox[String] {
      items = ObservableBuffer.from(LogLevels)
      value = "ALL"
      onAction = (_: ActionEvent) => {
        currentLevelFilter = value.value
        applyFilters()
      }
    }

    val searchField = new TextField {
      promptText = "Search in logs"
      prefWidth = 200
      onKeyReleased = _ => {
        currentSearchText = text.value
        applyFilters()
      }
    }

    toolbar.items ++= Seq(openButton, new Separator(),
      new Label("Filter Level:"), levelFilter,
      new Separator(), searchField)

    // TableView for log entries
    val logTable = new TableView[LogEntry] {
      items = filteredEntries
      columns ++= List(
        new TableColumn[LogEntry, Number] {
          text = "ID"
          prefWidth = 40
          cellValueFactory = { cellData =>
            cellData.value.idProperty.asInstanceOf[ObservableValue[Number, Number]]
          }
        },
        new TableColumn[LogEntry, String] {
          text = "Timestamp"
          prefWidth = 170
          cellValueFactory = { cellData =>
            cellData.value.timestampProperty
          }
        },
        new TableColumn[LogEntry, String] {
          text = "Level"
          prefWidth = 50
          cellValueFactory = { cellData =>
            cellData.value.levelProperty
          }

          // Style each row based on log level
          cellFactory = { _: TableColumn[LogEntry, String] => new TableCell[LogEntry, String] {
            item.onChange { (_, _, newValue) =>
              if (newValue != null) {
                text = newValue
                style = newValue match {
                  case "ERROR" => "-fx-background-color: #ffcccc; -fx-text-fill: #990000;"
                  case "WARN" => "-fx-background-color: #ffffcc; -fx-text-fill: #999900;"
                  case "INFO" => "-fx-background-color: #ccffcc; -fx-text-fill: #009900;"
                  case "DEBUG" => "-fx-background-color: #ccccff; -fx-text-fill: #000099;"
                  case _ => ""
                }
              } else {
                text = ""
                style = ""
              }
            }
          }
          }
        },
        new TableColumn[LogEntry, String] {
          text = "Source"
          prefWidth = 200
          cellValueFactory = { cellData =>
            cellData.value.sourceProperty
          }
        },
        new TableColumn[LogEntry, String] {
          text = "Message"
          prefWidth = 450
          cellValueFactory = { cellData =>
            cellData.value.messageProperty
          }
        }
      )
    }

    // Status bar with manual count management
    val entriesCountLabel = new Label()

    // Update counts whenever they change
    logEntries.onChange { (_, _) =>
      logEntriesCount.value = logEntries.size
    }

    filteredEntries.onChange { (_, _) =>
      filteredEntriesCount.value = filteredEntries.size
    }

    // Bind the label to the count properties
    entriesCountLabel.text <== Bindings.createStringBinding(
      () => s"${filteredEntriesCount.value} / ${logEntriesCount.value} entries shown",
      filteredEntriesCount, logEntriesCount
    )

    val statusBar = new HBox {
      padding = Insets(5)
      spacing = 10
      alignment = Pos.CenterLeft

      children = Seq(
        new Label("Status: ") {
          styleClass += "status-label"
        },
        entriesCountLabel
      )
    }

    // Details view for selected log entry
    val detailsView = new TextArea {
      editable = false
      wrapText = true
      prefHeight = 150
    }

    // Update details view when selection changes
    logTable.selectionModel().selectedItemProperty().onChange {
      (_, _, selectedEntry) =>
        detailsView.text = if (selectedEntry != null) {
          s"""
             |Timestamp: ${selectedEntry.timestampProperty.value}
             |Level: ${selectedEntry.levelProperty.value}
             |Source: ${selectedEntry.sourceProperty.value}
             |Message:
             |${selectedEntry.messageProperty.value}
             |""".stripMargin
        } else {
          ""
        }
    }

    // Arrange all components
    val splitPane = new SplitPane {
      items ++= Seq(
        logTable,
        detailsView
      )
      dividerPositions = 0.7 // Fixed the Array issue
    }

    borderPane.top = toolbar
    borderPane.center = splitPane
    borderPane.bottom = statusBar

    borderPane
  }

  def openLogFile(): Unit = {
    val fileChooser = new FileChooser {
      title = "Open Log File"
      extensionFilters ++= Seq(
        new FileChooser.ExtensionFilter("Log Files", Seq("*.log", "*.txt")),
        new FileChooser.ExtensionFilter("All Files", Seq("*.*"))
      )
    }

    val selectedFile = Option(fileChooser.showOpenDialog(stage))
    selectedFile.foreach(loadLogFile)
  }

  def loadLogFile(file: File): Unit = {
    Try {
      // Filter logs based on a pattern: timestamp level service file(optional) [attributes](optional) : message
      val logLineRegex = """(\d{4}/\d{2}/\d{2}\s\d{2}:\d{2}:\d{2})\s+([A-Z]+)\s+([a-zA-Z0-9$-:]+)\s+([a-zA-Z0-9._:-]+)?\s*+(\[.*?\][a-zA-Z0-9._:-]?)?\s*:\s+(.*?)""".r
      logEntries.clear()

      var lineNumber = 1 // Row counter
      Source.fromFile(file).getLines().foreach { line =>
        line match {
          case logLineRegex(timestampStr, level, service, file, attributes, message) =>
            Try {
              val timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))

              // Parse attributes only if they exist
              val attributeMap = if (attributes != null && attributes.nonEmpty) {
                attributes.split("\\s+")
                  .map(attr => {
                    val parts = attr.split("=", 2)
                    if (parts.length == 2) (parts(0), parts(1)) else (attr, "")
                  })
                  .toMap
              } else {
                Map.empty[String, String]
              }

              // TODO: Add Ids into UI tabs
              val tenant = attributeMap.getOrElse("tenant", "")
              val traceId = attributeMap.getOrElse("traceId", "")
              val requestId = attributeMap.getOrElse("requestId", "")

              logEntries += LogEntry(lineNumber, timestamp, level, service, message)
              lineNumber += 1 // Increment row count
            }.recover {
              case e: Exception =>
                // Handle timestamp parsing errors
                println(s"Error parsing timestamp: $timestampStr - ${e.getMessage}")
            }
          case _ =>
            // For lines that don't match the pattern, we could either ignore them or add them as continuation
            // Usually these are continuation lines like stack traces.
            if (logEntries.nonEmpty) {
              val lastEntry = logEntries.last
              val updatedEntry = LogEntry(
                lastEntry.id,
                lastEntry.timestamp,
                lastEntry.level,
                lastEntry.source,
                lastEntry.message + "\n" + line
              )
              logEntries.update(logEntries.size - 1, updatedEntry)
            }
        }
      }

      applyFilters()
    } match {
      case Success(_) =>
        // File loaded successfully
        println(s"Successfully loaded ${logEntries.size} log entries from ${file.getName}")
      case Failure(e) =>
        println(s"Error loading log file: ${e.getMessage}")
        new Alert(Alert.AlertType.Error) {
          title = "Error Loading Log File"
          headerText = "Failed to load log file"
          contentText = e.getMessage
        }.showAndWait()
    }
  }

  def applyFilters(): Unit = {
    filteredEntries.clear()

    // Apply level filter and text search
    logEntries.foreach { entry =>
      val matchesLevel = currentLevelFilter == "ALL" || entry.level == currentLevelFilter
      val matchesSearch = currentSearchText.isEmpty ||
        entry.message.toLowerCase.contains(currentSearchText.toLowerCase) ||
        entry.source.toLowerCase.contains(currentSearchText.toLowerCase)

      if (matchesLevel && matchesSearch) {
        filteredEntries += entry
      }
    }
  }
}

// Entry point for running the application
object LogViewerApp {
  def main(args: Array[String]): Unit = {
    LogViewer.main(args)
  }
}