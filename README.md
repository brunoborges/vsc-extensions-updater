# VS Code Extension Updater

<div align="center">

[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)](https://github.com/brunoborges/vsc-extensions-updater)

**A lightweight system tray application that automatically manages VS Code extension updates for multiple instances**

[Features](#-features) • [Quick Start](#-quick-start) • [Installation](#-installation) • [Usage](#-usage) • [Configuration](#%EF%B8%8F-configuration)

</div>

## ✨ Features

### 🎯 **Multi-Instance Support**
- **Auto-Detection**: Automatically discovers VS Code Stable and Insiders installations
- **Individual Control**: Enable/disable updates per instance
- **Smart Management**: Handles multiple VS Code versions simultaneously

### 🚀 **Update Operations**
- **Standard Updates**: Update extensions for any VS Code instance
- **Update & Launch**: Update extensions and automatically open VS Code
- **Scheduled Updates**: Configure automatic updates with flexible scheduling
- **Concurrent Control**: Configurable maximum concurrent updates

### 🖥️ **System Integration**
- **System Tray**: Lightweight background operation with tray icon
- **Cross-Platform**: Native support for Windows, macOS, and Linux
- **VS Code Logo**: Uses official VS Code icon in system tray
- **Context Menus**: Rich right-click menus for each VS Code instance

### 📊 **Monitoring & Logging**
- **Real-Time Logs**: Live command output with session-based logging
- **Update History**: Track last update time and status for each instance
- **Log Viewer**: Dedicated window for viewing detailed logs
- **Export Capability**: Save logs to files for troubleshooting

### ⚙️ **Advanced Configuration**
- **Flexible Scheduling**: Interval-based or cron-based scheduling
- **Smart Updates**: Skip recently updated instances
- **Idle Detection**: Only update when system is idle (optional)
- **Notification Control**: Customizable success/error notifications

## 🚀 Quick Start

### Prerequisites
- **Java 21+** ([Download](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html))
- **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))
- **VS Code** installed (Stable and/or Insiders)

### Build & Run
```bash
# Clone the repository
git clone https://github.com/brunoborges/vsc-extensions-updater.git
cd vsc-extensions-updater

# Build the application
mvn clean package

# Run the application
java -jar target/extension-updater-1.0.jar
```

That's it! Look for the VS Code icon in your system tray.

## 📦 Installation

### Option 1: Build from Source
```bash
git clone https://github.com/brunoborges/vsc-extensions-updater.git
cd vsc-extensions-updater
mvn clean package
```

### Option 2: Download JAR
1. Download the latest `extension-updater-1.0.jar` from releases
2. Run: `java -jar extension-updater-1.0.jar`

### Option 3: Auto-Start Setup
**Windows:**
1. Place the JAR in a permanent location
2. Create a batch file or add to Windows Startup folder

**macOS:**
1. Create a Launch Agent plist file
2. Place in `~/Library/LaunchAgents/`

**Linux:**
1. Create a desktop entry or systemd user service
2. Add to autostart applications

## 🎮 Usage

### First Launch
1. **System Tray Icon**: Look for the VS Code logo in your system tray
2. **Auto-Detection**: The app automatically detects installed VS Code instances
3. **Right-Click Menu**: Access all features via right-click context menu

### Basic Operations

#### Manual Updates
```
Right-click tray icon → [Instance Name] → 🚀 Update Extensions
```

#### Update & Launch
```
Right-click tray icon → [Instance Name] → 🚀🗂️ Update and Open VS Code
```

#### View Logs
```
Right-click tray icon → 📋 View Logs...
```

#### Configure Settings
```
Right-click tray icon → ⚙️ Settings...
```

### Menu Structure
```
● VS Code Extension Updater
├── 🚀 Update All Extensions                    (if multiple instances)
├── ──────────────────────────────────────────
├── ✅ VS Code 1.106.2                          (detected instance)
│   ├── 🚀 Update Extensions
│   ├── 🚀🗂️ Update and Open VS Code           ← NEW!
│   ├── ──────────────────
│   ├── ❌ Disable
│   ├── ──────────────────
│   ├── 📁 /Applications/Visual Studio Code.app/...
│   ├── 🕒 Last: Nov 25, 16:33
│   └── 📊 Success + Opened
├── ✅ VS Code Insiders 1.107.0-insider
│   └── ... (same options)
├── ──────────────────────────────────────────
├── 🔄 Refresh Detection
├── ──────────────────────────────────────────
├── 📋 View Logs...
├── ⚙️ Settings...
├── About
├── ──────────────────────────────────────────
└── Quit
```

## ⚙️ Configuration

### Automatic Configuration
The application automatically creates and manages configuration files in OS-appropriate locations:

- **Windows**: `%APPDATA%\\VSCodeExtensionUpdater\\config.json`
- **macOS**: `~/Library/Application Support/VSCodeExtensionUpdater/config.json`
- **Linux**: `~/.config/vscode-extension-updater/config.json`

### Settings Window
Access via: `Right-click tray → ⚙️ Settings...`

#### Scheduling Tab
- **Enable Automatic Updates**: Toggle scheduled updates
- **Update Interval**: Set update frequency (minutes/hours/days)
- **Schedule Type**: Choose interval or cron-based scheduling
- **Update on Startup**: Run updates when application starts
- **Skip Recent Updates**: Avoid updating recently updated instances
- **Idle Detection**: Only update when system is idle
- **Concurrency**: Maximum parallel updates

#### VS Code Instances Tab
- **Enable/Disable**: Control which instances to update
- **Instance Information**: View paths, versions, and status
- **Refresh Detection**: Re-scan for VS Code installations

#### General Tab
- **Notifications**: Control success/error notifications
- **Logging**: Configure log levels and behavior
- **UI Preferences**: Customize interface options

### Configuration File Structure
```json
{
  "vsCodeInstances": [
    {
      "path": "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code",
      "edition": "STABLE",
      "version": "1.106.2",
      "displayName": "VS Code (1.106.2)",
      "enabled": true,
      "lastUpdate": "Nov 25, 16:33",
      "updateStatus": "Success + Opened"
    }
  ],
  "autoDetectInstances": true,
  "commandTimeout": 300,
  "showNotifications": true,
  "schedule": {
    "enabled": false,
    "intervalMinutes": 60,
    "type": "INTERVAL",
    "updateOnStartup": false,
    "skipIfRecentlyUpdated": true,
    "recentlyUpdatedThresholdMinutes": 30,
    "onlyWhenIdle": false,
    "maxConcurrentUpdates": 2
  }
}
```

## 🏗️ Architecture

### Project Structure
```
src/main/java/com/vscode/updater/
├── Application.java                   # Main entry point
├── config/
│   ├── ConfigManager.java            # Configuration management
│   ├── VSCodeConfig.java             # Configuration data model
│   └── BasicConfig.java              # Basic configuration record
├── discovery/
│   ├── VSCodeDetector.java           # VS Code installation detection
│   └── VSCodeInstance.java           # VS Code instance representation
├── executor/
│   └── CommandExecutor.java          # Extension update execution
├── gui/
│   ├── SettingsWindow.java           # Settings interface
│   ├── LogViewerWindow.java          # Log display window
│   ├── AboutDialog.java              # About dialog
│   └── InstanceTableModel.java       # Table model for instances
├── logging/
│   └── LogManager.java               # Session-based logging
├── scheduler/
│   └── UpdateScheduler.java          # Automatic update scheduling
├── tray/
│   ├── SystemTrayManager.java        # System tray management
│   └── MultiInstanceMenuBuilder.java # Dynamic menu construction
└── util/
    ├── AppInfo.java                  # Application information
    └── ProcessUtils.java             # Process and detection utilities

src/main/resources/
└── vscode-logo.png                   # VS Code logo for system tray
```

### Key Technologies
- **Java 21**: Virtual Threads for non-blocking operations
- **Jackson**: JSON configuration serialization
- **Swing**: Cross-platform GUI components
- **System Tray**: Native OS integration
- **CompletableFuture**: Asynchronous command execution

## 🧪 Testing

### Run Tests
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ConfigManagerTest

# Run tests with coverage
mvn test jacoco:report
```

### Test Structure
```
src/test/java/com/vscode/updater/
├── ApplicationTest.java
├── config/
│   ├── ConfigManagerTest.java
│   └── VSCodeConfigTest.java
├── discovery/
│   ├── VSCodeDetectorTest.java
│   └── VSCodeInstanceTest.java
└── util/
    └── ProcessUtilsTest.java
```

## 🐛 Troubleshooting

### Common Issues

**VS Code not detected:**
- Ensure VS Code is installed in standard locations
- Check if VS Code is in your system PATH
- Use "Refresh Detection" from the tray menu

**Update commands fail:**
- Verify VS Code executable permissions
- Check if VS Code is currently running
- Review logs in the Log Viewer

**Application won't start:**
- Ensure Java 21+ is installed and in PATH
- Check system tray support on your platform
- Run from command line to see error messages

### Getting Help
1. **Check Logs**: Use the built-in log viewer
2. **Console Output**: Run from terminal for detailed errors
3. **Configuration**: Verify config file structure
4. **VS Code Validation**: Test VS Code commands manually

### Log Locations
Logs are displayed in the application's Log Viewer window. Session-based logging provides detailed information about each update operation.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Microsoft**: For VS Code and extension ecosystem
- **Java Community**: For Virtual Threads and modern Java features
- **Contributors**: Everyone who helped improve this tool

---

<div align="center">

**Made with ❤️ for the VS Code community**

[⭐ Star this repo](https://github.com/brunoborges/vsc-extensions-updater) if you find it useful!

</div>