package sun.scalafx

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Orientation, Side}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.stage.FileChooser

import java.io.File
import scala.io.Source
import scala.util.{Failure, Success, Try, Using}

import sun.scalafx.config.AppConfig
import sun.scalafx.model.{LogEntry, LogTabState}
import sun.scalafx.parser.LogParser
import sun.scalafx.ui.UIComponents

/**
 * Next-Gen Log Viewer Application
 *
 * A powerful, user-friendly desktop application for analyzing log files.
 *
 * Features:
 *   - Multiple log files in tabs
 *   - Multiple log format support
 *   - Level-based filtering (INFO, WARN, ERROR, DEBUG)
 *   - Full-text search
 *   - Color-coded log levels
 *   - Expandable log details
 *
 * @author littlegrasscao
 * @version 2.0
 */
object LogViewer extends JFXApp3 {

  // ============================================================================
  // Application State
  // ============================================================================

  /** The main tab pane holding all log file tabs */
  private var tabPane: TabPane = _

  /** Map of tab -> state for quick lookup */
  private val tabStates: scala.collection.mutable.Map[Tab, LogTabState] =
    scala.collection.mutable.Map.empty

  // ============================================================================
  // Application Entry Point
  // ============================================================================

  override def start(): Unit = {
    // Set macOS Dock icon (must be done before stage is shown)
    setDockIcon()

    stage = new PrimaryStage {
      title = AppConfig.Window.Title
      width = AppConfig.Window.Width
      height = AppConfig.Window.Height

      // Set window icons (for title bar)
      icons ++= loadAppIcons()

      scene = new Scene {
        root = buildMainLayout()
      }
    }
  }

  /**
   * Sets the macOS Dock icon using java.awt.Taskbar API.
   * This affects the Dock and App Switcher (Cmd+Tab).
   */
  private def setDockIcon(): Unit = {
    Try {
      if (java.awt.Taskbar.isTaskbarSupported) {
        val taskbar = java.awt.Taskbar.getTaskbar
        if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
          val iconStream = getClass.getResourceAsStream("/icons/app-icon-256.png")
          if (iconStream != null) {
            val image = javax.imageio.ImageIO.read(iconStream)
            taskbar.setIconImage(image)
            iconStream.close()
          }
        }
      }
    }.recover {
      case e: Exception =>
        // Silently ignore - Dock icon is optional
        println(s"Could not set Dock icon: ${e.getMessage}")
    }
  }

  /**
   * Loads application icons from resources.
   * Provides multiple sizes for different display contexts (taskbar, window, dock).
   */
  private def loadAppIcons(): Seq[javafx.scene.image.Image] = {
    val iconSizes = Seq("16", "32", "64", "128", "256")
    iconSizes.flatMap { size =>
      Try {
        val stream = getClass.getResourceAsStream(s"/icons/app-icon-$size.png")
        if (stream != null) Some(new javafx.scene.image.Image(stream)) else None
      }.toOption.flatten
    }
  }

  // ============================================================================
  // Main Layout
  // ============================================================================

  /**
   * Builds the main application layout.
   * Structure: Toolbar (top) | TabPane (center)
   */
  private def buildMainLayout(): BorderPane = {
    tabPane = buildTabPane()

    new BorderPane {
      top = buildGlobalToolbar()
      center = tabPane
    }
  }

  /**
   * Creates the global toolbar with file operations.
   */
  private def buildGlobalToolbar(): ToolBar = {
    val openButton = new Button("Open Log File") {
      style = "-fx-font-weight: bold;"
      onAction = (_: ActionEvent) => handleOpenFile()
    }

    val openMultipleButton = new Button("Open Multiple Files") {
      onAction = (_: ActionEvent) => handleOpenMultipleFiles()
    }

    val closeTabButton = new Button("Close Current Tab") {
      onAction = (_: ActionEvent) => closeCurrentTab()
    }

    val closeAllButton = new Button("Close All Tabs") {
      onAction = (_: ActionEvent) => closeAllTabs()
    }

    new ToolBar {
      items ++= Seq(
        openButton,
        openMultipleButton,
        new Separator { orientation = Orientation.Vertical },
        closeTabButton,
        closeAllButton
      )
    }
  }

  /**
   * Creates the main TabPane for holding log file tabs.
   */
  private def buildTabPane(): TabPane = new TabPane {
    side = Side.Top
    tabClosingPolicy = TabPane.TabClosingPolicy.AllTabs
    tabs += createWelcomeTab()
  }

  // ============================================================================
  // Tab Creation
  // ============================================================================

  /**
   * Creates a welcome tab shown when no log files are open.
   */
  private def createWelcomeTab(): Tab = new Tab {
    text = "Welcome"
    closable = false
    content = UIComponents.createWelcomeContent(() => handleOpenFile())
  }

  /**
   * Creates a new tab for a log file with its own state and UI.
   *
   * @param tabState The state object for this tab
   * @return A fully configured Tab
   */
  private def createLogTab(tabState: LogTabState): Tab = {
    val logTable = UIComponents.createLogTable(tabState)
    val detailsPane = UIComponents.createDetailsPane(logTable)
    val filterBar = UIComponents.createFilterBar(tabState)
    val statusBar = UIComponents.createStatusBar(tabState)

    // Highlight bar with refresh callback
    val highlightBar = UIComponents.createHighlightBar(tabState, () => {
      UIComponents.refreshTableHighlights(logTable)
    })

    // Top section: filter bar + highlight bar
    val topSection = new VBox {
      children = Seq(filterBar, highlightBar)
    }

    val mainContent = new SplitPane {
      orientation = Orientation.Vertical
      items ++= Seq(logTable, detailsPane)
      dividerPositions = 0.75
    }

    val tabContent = new BorderPane {
      top = topSection
      center = mainContent
      bottom = statusBar
    }

    val tab = new Tab {
      text = tabState.fileName
      tooltip = new Tooltip(tabState.filePath)
      content = tabContent
      closable = true

      // Cleanup when tab is closed
      onClosed = _ => {
        tabStates.remove(delegate)
        if (tabStates.isEmpty) {
          showWelcomeTab()
        }
      }
    }

    // Store state reference
    tabStates(tab.delegate) = tabState

    tab
  }

  // ============================================================================
  // Tab Management
  // ============================================================================

  /**
   * Shows the welcome tab (used when all log tabs are closed).
   */
  private def showWelcomeTab(): Unit = {
    val welcomeExists = tabPane.tabs.exists(_.getText == "Welcome")
    if (!welcomeExists) {
      tabPane.tabs.clear()
      tabPane.tabs += createWelcomeTab()
    }
  }

  /**
   * Removes the welcome tab if present.
   */
  private def removeWelcomeTab(): Unit = {
    tabPane.tabs.find(_.getText == "Welcome").foreach { tab =>
      tabPane.tabs -= tab
    }
  }

  /**
   * Closes the currently selected tab.
   */
  private def closeCurrentTab(): Unit = {
    Option(tabPane.selectionModel().getSelectedItem).foreach { selectedTab =>
      if (selectedTab.getText != "Welcome" && selectedTab.isClosable) {
        tabPane.tabs -= selectedTab
        tabStates.remove(selectedTab)

        if (tabStates.isEmpty) {
          showWelcomeTab()
        }
      }
    }
  }

  /**
   * Closes all log tabs and shows the welcome tab.
   */
  private def closeAllTabs(): Unit = {
    tabPane.tabs.clear()
    tabStates.clear()
    showWelcomeTab()
  }

  // ============================================================================
  // File Handling
  // ============================================================================

  /**
   * Opens a file chooser dialog for a single file.
   */
  private def handleOpenFile(): Unit = {
    val fileChooser = createFileChooser("Open Log File")
    Option(fileChooser.showOpenDialog(stage)).foreach(openFileInNewTab)
  }

  /**
   * Opens a file chooser dialog for multiple files.
   */
  private def handleOpenMultipleFiles(): Unit = {
    val fileChooser = createFileChooser("Open Multiple Log Files")
    val files = fileChooser.showOpenMultipleDialog(stage)
    if (files != null) {
      files.foreach(openFileInNewTab)
    }
  }

  /**
   * Creates a configured file chooser.
   */
  private def createFileChooser(dialogTitle: String): FileChooser = new FileChooser {
    title = dialogTitle
    extensionFilters ++= Seq(
      new FileChooser.ExtensionFilter("Log Files", Seq("*.log", "*.txt")),
      new FileChooser.ExtensionFilter("All Files", Seq("*.*"))
    )
  }

  /**
   * Opens a file in a new tab.
   *
   * @param file The file to open
   */
  private def openFileInNewTab(file: File): Unit = {
    // Check if file is already open
    val existingTab = tabStates.find(_._2.filePath == file.getAbsolutePath).map(_._1)
    if (existingTab.isDefined) {
      tabPane.selectionModel().select(existingTab.get)
      return
    }

    // Create new tab state and load file
    val tabState = new LogTabState(file.getName, file.getAbsolutePath)
    val result = loadLogFile(file, tabState)

    result match {
      case Success(_) =>
        removeWelcomeTab()
        val newTab = createLogTab(tabState)
        tabPane.tabs += newTab
        tabPane.selectionModel().select(newTab)
        println(s"Loaded ${tabState.logEntries.size} entries from ${file.getName}")

      case Failure(e) =>
        showErrorAlert(
          "Error Loading Log File",
          s"Failed to load: ${file.getName}",
          e.getMessage
        )
    }
  }

  /**
   * Loads and parses a log file into a tab state.
   *
   * @param file     The log file to load
   * @param tabState The tab state to populate
   * @return Success or Failure
   */
  private def loadLogFile(file: File, tabState: LogTabState): Try[Unit] = {
    Try {
      tabState.clear()
      var lineNumber = 1

      Using(Source.fromFile(file)) { source =>
        source.getLines().foreach { line =>
          LogParser.parseLine(line, lineNumber) match {
            case Some(entry) =>
              tabState.addEntry(entry)
              lineNumber += 1
            case None if tabState.logEntries.nonEmpty =>
              tabState.appendToPreviousEntry(line)
            case None =>
              tabState.addEntry(LogEntry.unparsed(lineNumber, line))
              lineNumber += 1
          }
        }
      }

      tabState.applyFilters()
    }
  }

  // ============================================================================
  // Utility Methods
  // ============================================================================

  /**
   * Displays an error alert dialog.
   */
  private def showErrorAlert(alertTitle: String, header: String, content: String): Unit = {
    new Alert(Alert.AlertType.Error) {
      initOwner(stage)
      title = alertTitle
      headerText = header
      contentText = content
    }.showAndWait()
  }
}
