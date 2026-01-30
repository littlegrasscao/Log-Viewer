# Next-Gen Log Viewer

A powerful, user-friendly desktop application for log analysis built with **Scala** and **ScalaFX**.

## Features

- **Multi-Tab Support**: Open multiple log files simultaneously in separate tabs
- **Log Parsing & Structuring**: Automatically extracts timestamp, log level, source, and message from raw logs
- **Advanced Filtering & Search**:
  - Filter by log level (INFO, WARN, ERROR, DEBUG)
  - Keyword search for quickly locating relevant logs
  - Real-time filtering as you type
- **Keyword Highlighting** (New in v2.1):
  - Highlight multiple keywords with distinct colors (per-tab)
  - Right-click any cell to add words to highlights
  - "Highlighted Only" toggle to filter and show only matching rows
  - Up to 8 color-coded highlight groups
- **User-Friendly UI**:
  - Logs displayed in a structured table format
  - Color-coded log levels for better visibility
  - Expandable log details panel
  - Welcome screen with quick-start guide
- **macOS Integration**:
  - Custom application icon in Dock and App Switcher
  - Native menu bar integration
- **File Support**:
  - Open single or multiple log files at once
  - Supports .log and .txt files
  - Auto-detects various log patterns and formats

## Installation

### macOS Application Download

Download the prebuilt DMG installer:

**[Log Viewer-2.1.0.dmg](Log%20Viewer-2.1.0.dmg)**

1. Download and open the DMG file
2. Drag "Log Viewer" to your Applications folder
3. If macOS blocks the app, go to **System Settings → Privacy & Security** and click "Open Anyway"

### Running from Source (Development)

**Prerequisites:**
- JDK 11 or later
- Scala 2.13
- SBT

```sh
# Clone the repository
git clone https://github.com/littlegrasscao/Log-Viewer.git
cd Log-Viewer

# Run the application
sbt run
```

### Building the Application

```sh
# Generate a standalone JAR file
sbt assembly

# Package as macOS .app bundle with custom icon
jpackage --name "Log Viewer" \
  --input target/scala-2.13 \
  --main-jar Log-Viewer-assembly-0.1-SNAPSHOT.jar \
  --main-class sun.scalafx.LogViewerApp \
  --icon src/main/resources/icons/LogViewer.icns \
  --type dmg \
  --mac-package-name "Log Viewer" \
  --app-version "2.1.0" \
  --java-options "-Xdock:name='Log Viewer'" \
  --java-options "-Dapple.awt.application.name='Log Viewer'"
```

For other platforms:
- Windows: Use `--type msi` and provide a `.ico` icon
- Linux: Use `--type deb` and provide a `.png` icon

## How to Use

1. **Open Log Files**: Click "Open File" for a single file or "Open Multiple" for batch loading
2. **Navigate Tabs**: Switch between open files using the tab bar
3. **Filter Logs**: Use the level dropdown or search box to filter entries
4. **View Details**: Click any log entry to see full details in the bottom panel
5. **Close Tabs**: Click the X on individual tabs or use "Close All"

## Supported Log Formats

The viewer auto-detects common log patterns:

| Format | Example |
|--------|---------|
| Standard | `2025/02/25 06:28:24 WARN ExampleLogger$ SomeClass.scala:144 : Message` |
| ISO Timestamp | `2025-02-25T06:28:24.123 INFO [main] ClassName - Message` |
| Simple | `INFO 2025-02-25 06:28:24 - Message` |
| Bracket Style | `[2025-02-25 06:28:24] [ERROR] Message` |

## Project Structure

```
src/main/scala/sun/scalafx/
├── LogViewerApp.scala      # Application entry point
├── LogViewer.scala         # Main application logic
├── config/
│   └── AppConfig.scala     # Application constants and styling
├── model/
│   ├── LogEntry.scala      # Log entry data model
│   └── LogTabState.scala   # Per-tab state management
├── parser/
│   └── LogParser.scala     # Log parsing logic
└── ui/
    └── UIComponents.scala  # Reusable UI components
```

## Contribution

Contributions are welcome! Feel free to open an issue or submit a pull request.

## License

Copyright (c) 2025-2026 littlegrasscao
