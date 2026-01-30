package sun.scalafx

/**
 * Main entry point for the Log Viewer application.
 *
 * This is a simple bootstrap class that delegates to the main
 * LogViewer application object.
 */
object LogViewerApp {

  // Application name shown in macOS Dock and menu bar
  private val AppName = "Log Viewer"

  def main(args: Array[String]): Unit = {
    // Set macOS application name (must be before AWT/JavaFX initialization)
    System.setProperty("apple.awt.application.name", AppName)
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    
    LogViewer.main(args)
  }
}
