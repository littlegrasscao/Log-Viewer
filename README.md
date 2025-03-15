# Next-Gen Log Viewer

## Overview
Next-Gen Log Viewer is a powerful, user-friendly desktop application designed to simplify log analysis for engineers. Built with **Scala** and **ScalaFX**, it transforms raw log files into a structured, easy-to-read format, significantly improving debugging efficiency.

## Features
- **Log Parsing & Structuring**: Automatically extracts key components (timestamp, log level, source, message body) from raw logs.
- **Advanced Filtering & Search**:
    - Filter by log level (INFO, WARN, ERROR, DEBUG, etc.).
    - Keyword search for quickly locating relevant logs.
    - Sort logs by timestamp.
- **User-Friendly UI**:
    - Logs displayed in a structured table format.
    - Color-coded log levels for better visibility.
    - Expandable log details for in-depth analysis.
- **File Support**:
    - Open and parse log files (.log, .txt).
    - Auto-detects log patterns and formats.

## Installation
### MacOS Application
Download the "Log Viewer-1.0.dmg" file.

### Prerequisites
- JDK 11 or later
- Scala & SBT installed

### Run the Application
```sh
sbt run
```

### Build an Application Executable
```sh
sbt assembly

jpackage --name "Log Viewer" \
         --input target/scala-2.13/ \
         --main-jar Log-Viewer-assembly-0.1-SNAPSHOT.jar \
         --main-class sun.scalafx.LogViewerApp \
         --type dmg  # Use "msi" for Windows, "deb" for Linux
```

## How to Use
1. **Open a Log File**: Click the "Open Log File" button and select a log file.
2. **View Logs**: Logs will be displayed in a structured table format.
3. **Filter & Search**: Use the dropdown to filter logs by level or search for keywords.
4. **View Details**: Click on a log entry to see detailed information.

## Contribution
We welcome contributions! Feel free to open an issue or submit a pull request.

## License
Copyright (c) 2025 littlegrasscao



