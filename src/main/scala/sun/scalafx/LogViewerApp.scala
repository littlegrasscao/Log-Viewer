package sun.scalafx

/**
 * Main entry point for the Log Viewer application.
 *
 * This is a simple bootstrap class that delegates to the main
 * LogViewer application object.
 */
object LogViewerApp {

  def main(args: Array[String]): Unit = {
    LogViewer.main(args)
  }
}
