# VS Code Extension Updater - Specification

## Overview

A Java-based, cross-platform background application that automatically detects and updates Visual Studio Code extensions without requiring the IDE to be open. The application will support both VS Code stable and VS Code Insiders editions.

## Objectives

- **System Tray Integration**: Always-accessible application via system tray/menu bar
- **Scheduled Updates**: Configurable interval execution of `code --update-extensions`
- **Command Execution**: Run VS Code CLI commands and capture output
- **Log Display**: Real-time log viewing within the application interface
- **Cross-Platform GUI**: Native system tray support for Windows and macOS
- **Multi-Version Support**: Handle both VS Code stable and VS Code Insiders
- **User-Controlled Scheduling**: Flexible timing configuration (minutes/hours/days)
- **Safe Operation**: Ensure updates don't interfere with running VS Code instances

## Target Platforms

- **Windows**: Windows 10/11 (x64) with system tray integration
- **macOS**: macOS 10.15+ (Intel and Apple Silicon) with menu bar integration
- **Linux**: Ubuntu 18.04+, RHEL 8+, and other major distributions (optional support)

## Core Requirements

### 1. VS Code Detection

#### 1.1 Installation Discovery
- Automatically detect VS Code installations on the system
- Support standard installation paths for each platform:
  - **Windows**: 
    - `%LOCALAPPDATA%\Programs\Microsoft VS Code\bin\code.cmd`
    - `%PROGRAMFILES%\Microsoft VS Code\bin\code.cmd`
    - `%LOCALAPPDATA%\Programs\Microsoft VS Code Insiders\bin\code-insiders.cmd`
  - **macOS**:
    - `/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code`
    - `/Applications/Visual Studio Code - Insiders.app/Contents/Resources/app/bin/code-insiders`
  - **Linux**:
    - `/usr/bin/code`
    - `/usr/local/bin/code`
    - `/snap/bin/code`
    - `/usr/bin/code-insiders`
    - Flatpak installations

#### 1.2 Version Detection
- Identify VS Code version and edition (stable vs. insiders)
- Detect multiple VS Code installations on the same system
- Validate VS Code executable functionality

### 2. Extension Management via VS Code CLI

#### 2.1 Command Execution
- Execute `code --update-extensions` command for VS Code stable
- Execute `code-insiders --update-extensions` command for VS Code Insiders
- Support for custom VS Code executable paths
- Capture stdout, stderr, and exit codes from command execution
- Real-time output streaming for live log display

#### 2.2 Command Output Processing
- Parse VS Code CLI output for extension update information
- Extract extension names, versions, and update status
- Identify successful updates, failures, and skipped extensions
- Format output for user-friendly display in log viewer

#### 2.3 Multi-Instance Support
- Detect and handle multiple VS Code installations
- Sequential execution for multiple VS Code versions
- Individual scheduling per VS Code installation (optional)
- Aggregated logging for all VS Code instances

### 3. Configuration System

#### 3.1 Configuration File
```json
{
  "scheduler": {
    "enabled": true,
    "intervalType": "minutes|hours|daily|weekly|custom",
    "intervalValue": 60,
    "dailyTime": "10:00",
    "weeklyDays": ["monday", "wednesday", "friday"],
    "customCron": "0 */2 * * *",
    "runOnStartup": true,
    "startupDelay": 5,
    "skipIfVSCodeRunning": true
  },
  "vsCodePaths": [
    {
      "path": "/path/to/code",
      "edition": "stable|insiders",
      "enabled": true,
      "arguments": "--update-extensions"
    }
  ],
  "execution": {
    "timeoutSeconds": 300,
    "retryAttempts": 2,
    "retryDelay": 30,
    "workingDirectory": null
  },
  "ui": {
    "startMinimized": true,
    "autoStart": true,
    "showInTaskbar": false,
    "trayIconTheme": "auto|light|dark",
    "confirmBeforeManualUpdate": false,
    "autoOpenLogsOnError": true
  },
  "logging": {
    "level": "info|debug|warn|error",
    "file": "/path/to/logfile.log",
    "maxSize": "10MB",
    "retention": 30,
    "realTimeDisplay": true,
    "structuredLogging": true,
    "includeCommandOutput": true
  },
  "notifications": {
    "enabled": true,
    "onStart": false,
    "onSuccess": true,
    "onFailure": true,
    "onSchedulerChange": true,
    "soundEnabled": false
  }
}
```

#### 3.2 Configuration Management
- Default configuration creation on first run with setup wizard
- Configuration validation and error handling with GUI feedback
- Runtime configuration reloading without restart
- GUI-based configuration editor accessible from tray menu
- Platform-specific configuration paths:
  - **Windows**: `%APPDATA%\VSCodeExtensionUpdater\config.json`
  - **macOS**: `~/Library/Application Support/VSCodeExtensionUpdater/config.json`
- Auto-start configuration:
  - **Windows**: Registry entry in `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`
  - **macOS**: Login Items via macOS Security Framework

### 4. System Tray Application Architecture

#### 4.1 System Tray Integration
- **Primary Interface**: System tray icon as the main user interaction point
- **Windows**: System tray with context menu integration
- **macOS**: Menu bar application with native menu integration
- **Background Process**: Silent operation without visible windows
- **Auto-start**: Launch automatically on system startup
- **Persistent**: Remain running until explicitly quit by user

#### 4.2 Service Management
- **Windows**: 
  - User-space application (no Windows Service required)
  - Auto-start via Windows Registry (Run key) or Start Menu folder
  - System tray icon with Windows-native context menu
- **macOS**: 
  - Login item for automatic startup
  - Menu bar extra with native macOS menu styling
  - Integration with macOS notification center

#### 4.3 Scheduler
- **Interval-Based Execution**: Configurable timing in minutes, hours, or days
- **Flexible Scheduling Options**:
  - Every X minutes (minimum: 5 minutes)
  - Every X hours (1-24 hours)
  - Daily at specific time
  - Weekly on specific days
  - Custom cron-like expressions
- **Virtual Threads**: Non-blocking scheduler using Java 21 Virtual Threads
- **System Events**: Handle system sleep/wake scenarios and missed schedules
- **Manual Triggers**: Immediate execution via tray menu
- **Scheduler State**: Pause/resume functionality
- **Next Run Display**: Show next scheduled execution time in tray menu

#### 4.4 Process Safety
- Detect running VS Code instances before updating
- Implement file locking to prevent concurrent operations
- Handle VS Code workspace locks
- Safe cleanup on application termination
- Tray icon status indication (idle, updating, error states)

### 5. Logging and Real-Time Display

#### 5.1 Command Output Logging
- **Real-time capture**: Stream output from `code --update-extensions` command
- **Structured logging**: Timestamp, command, output, and result status
- **Multi-format support**: Plain text and structured JSON logs
- **Log levels**: INFO, WARN, ERROR for different command outcomes
- **Session logs**: Separate log entries for each scheduled execution

#### 5.2 Log Viewer Interface
- **Dedicated log window**: Accessible from tray menu "View Logs..."
- **Real-time updates**: Live streaming of command output during execution
- **Search and filter**: Find specific extensions or error messages
- **Log levels**: Filter by severity (Info, Warning, Error)
- **Export functionality**: Save logs to file for troubleshooting
- **Auto-scroll**: Follow mode for real-time command execution
- **Syntax highlighting**: Colorized output for better readability

#### 5.3 Log Management
- **File rotation**: Automatic log file rotation with size limits
- **Retention policy**: Configurable number of days to keep logs
- **Archive compression**: Compress old log files to save space
- **Log locations**:
  - **Windows**: `%APPDATA%\VSCodeExtensionUpdater\logs\`
  - **macOS**: `~/Library/Logs/VSCodeExtensionUpdater/`

### 6. User Interface

#### 6.1 System Tray Interface (Primary)
**System Tray Menu Structure:**
```
VS Code Extension Updater
├── ● Status: Idle / Running Update / Error
├── ──────────────────
├── Run Update Now
├── ⏸ Pause Scheduler / ▶ Resume Scheduler
├── ──────────────────
├── Last Update: 2024-01-15 10:30 AM
├── Next Update: 2024-01-15 11:30 AM (1 hour)
├── Extensions Updated: 3 successful, 0 failed
├── ──────────────────
├── VS Code Installations
│   ├── ✓ VS Code (Stable) - C:\Users\...\Code\bin\code.cmd
│   └── ✓ VS Code Insiders - Available
├── ──────────────────
├── 📋 View Logs...
├── ⚙️ Settings...
├── About
├── ──────────────────
└── Quit
```

**Visual States:**
- **Idle**: Green icon - waiting for next scheduled run
- **Running**: Animated/pulsing icon - `code --update-extensions` executing
- **Success**: Green checkmark overlay - last update completed successfully
- **Error**: Red icon - last update failed
- **Paused**: Gray icon - scheduler temporarily disabled

#### 6.2 Settings Window
**Scheduler Configuration:**
- **Update Interval**: 
  - Radio buttons: Every X Minutes / Every X Hours / Daily / Weekly / Custom
  - Numeric input for interval value (5-1440 minutes, 1-24 hours)
  - Time picker for daily updates
  - Day selection for weekly updates
- **VS Code Paths**: Auto-detected and manual path configuration
- **Execution Options**: 
  - Run on startup (delay setting)
  - Skip if VS Code is running
  - Timeout settings for command execution
- **Logging Preferences**: Log level, retention period, auto-open logs
- **Notification Settings**: Success/failure notifications

#### 6.3 Log Viewer Window
**Real-Time Log Display:**
```
[2024-01-15 10:30:15] INFO: Starting scheduled update
[2024-01-15 10:30:15] INFO: Executing: code --update-extensions
[2024-01-15 10:30:16] INFO: Checking for extension updates...
[2024-01-15 10:30:18] INFO: Updating ms-python.python from 2024.0.1 to 2024.1.0
[2024-01-15 10:30:22] INFO: Updating ms-vscode.cpptools from 1.18.5 to 1.19.0
[2024-01-15 10:30:25] INFO: Extension update completed successfully
[2024-01-15 10:30:25] INFO: Command finished with exit code: 0
[2024-01-15 10:30:25] INFO: Next update scheduled for: 2024-01-15 11:30 AM
```

**Log Viewer Features:**
- **Auto-scroll**: Follow mode during active command execution
- **Search box**: Filter logs by extension name, date, or message
- **Level filter**: Show/hide INFO, WARN, ERROR messages
- **Export button**: Save current session or all logs to file
- **Clear button**: Clear current display (not files)
- **Refresh button**: Reload logs from file

#### 6.4 Notifications
**Native System Notifications:**
- **Update Started**: "VS Code extension update in progress..."
- **Update Completed**: "3 extensions updated successfully" 
- **Update Failed**: "Extension update failed - click to view logs"
- **Scheduler Status**: "Update scheduler paused" / "Update scheduler resumed"

#### 6.5 Command Line Interface (Secondary)
```bash
# Run as GUI application with system tray (default)
vscode-updater

# Run update immediately and show output
vscode-updater --run-now [--show-output]

# Show current scheduler status
vscode-updater --status

# Set update interval from command line
vscode-updater --set-interval 30m
vscode-updater --set-interval 2h  
vscode-updater --set-interval daily

# Pause/resume scheduler
vscode-updater --pause
vscode-updater --resume

# Show logs
vscode-updater --show-logs [--tail 50]

# Test VS Code command execution
vscode-updater --test-command [--vscode-path /path/to/code]
```

### 7. Error Handling and Recovery

#### 7.1 Command Execution Errors
- **VS Code not found**: Display error and offer path configuration
- **Command timeout**: Configurable timeout with retry mechanism  
- **Permission errors**: Clear error messages with resolution suggestions
- **VS Code process conflicts**: Detection and waiting mechanisms
- **Invalid command output**: Parsing error handling and fallback

#### 7.2 Scheduler Error Scenarios  
- **System sleep/wake**: Reschedule missed executions intelligently
- **Application crash recovery**: Persistent scheduler state
- **Configuration errors**: Validation with user-friendly error messages
- **Network connectivity**: Graceful handling of offline scenarios

#### 7.3 Recovery Mechanisms
- **Automatic retry**: Exponential backoff for failed command executions
- **Fallback scheduling**: Alternative intervals on repeated failures
- **Log preservation**: Ensure logs are saved even during crashes
- **State recovery**: Resume scheduler state after application restart
- **User notification**: Clear error reporting with actionable suggestions

### 8. Security Considerations

#### 8.1 Permissions
- Minimal required permissions
- Secure configuration file handling
- Safe temporary file management

#### 8.2 Extension Validation
- Verify extension signatures where possible
- Validate extension sources
- Handle untrusted extension scenarios

## Technical Architecture

### 8.1 Technology Stack
- **Language**: Java 21 LTS (Virtual Threads, Pattern Matching, Records)
- **Build System**: Maven 3.9+ or Gradle 8+
- **GUI Framework**: Java Swing/AWT for system tray integration
- **Dependencies**:
  - Java AWT SystemTray (Built-in system tray support)
  - SLF4J + Logback (Logging)
  - Jackson (JSON processing)
  - Java 21 Virtual Threads (Built-in concurrency)
  - JUnit 5 (Testing)
  - Native compilation support via GraalVM (optional)
  - Platform-specific libraries:
    - **Windows**: JNA for advanced Windows integration
    - **macOS**: JNA for native menu bar integration

### 8.2 Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/vscode/updater/
│   │       ├── Application.java (Main GUI entry point)
│   │       ├── tray/
│   │       │   ├── SystemTrayManager.java
│   │       │   ├── TrayMenuBuilder.java
│   │       │   └── NotificationManager.java
│   │       ├── gui/
│   │       │   ├── SettingsWindow.java
│   │       │   ├── LogViewerWindow.java
│   │       │   ├── SchedulerConfigPanel.java
│   │       │   └── AboutDialog.java
│   │       ├── scheduler/
│   │       │   ├── TaskScheduler.java
│   │       │   ├── IntervalCalculator.java
│   │       │   └── SchedulerState.java
│   │       ├── executor/
│   │       │   ├── CommandExecutor.java
│   │       │   ├── OutputStreamCapture.java
│   │       │   └── VSCodeCommandBuilder.java
│   │       ├── logging/
│   │       │   ├── LogManager.java
│   │       │   ├── LogEntry.java (Record)
│   │       │   ├── LogViewer.java
│   │       │   └── LogFormatter.java
│   │       ├── config/
│   │       │   ├── ConfigRecord.java (Java Records)
│   │       │   ├── ConfigManager.java
│   │       │   └── SchedulerConfig.java (Record)
│   │       ├── discovery/
│   │       │   ├── VSCodeDetector.java
│   │       │   └── PlatformDetector.java
│   │       ├── platform/
│   │       │   ├── WindowsIntegration.java
│   │       │   ├── MacOSIntegration.java
│   │       │   └── AutoStartManager.java
│   │       └── util/
│   │           ├── ProcessUtils.java (Process API)
│   │           ├── TimeUtils.java
│   │           └── PathMatcher.java (Pattern Matching)
│   └── resources/
│       ├── application.properties
│       ├── logback.xml
│       ├── icons/
│       │   ├── tray-icon.png
│       │   ├── tray-icon-running.png
│       │   ├── tray-icon-success.png
│       │   ├── tray-icon-error.png
│       │   ├── tray-icon-paused.png
│       │   └── app-icon.ico/.icns
│       └── META-INF/native-image/
├── test/
│   ├── java/
│   │   └── com/vscode/updater/
│   │       ├── scheduler/
│   │       ├── executor/
│   │       └── config/
│   └── resources/
│       └── test-config.json
└── assembly/
    ├── windows/
    │   ├── installer.nsi
    │   └── autostart.reg
    ├── macos/
    │   ├── Info.plist
    │   └── create-dmg.sh
    └── native/
```

### 8.3 Deployment Strategy
- Platform-specific GUI applications with auto-start integration
- **Windows**: 
  - NSIS installer (.exe) with auto-start registry configuration
  - Self-contained JAR with embedded JRE (Java 21)
  - Native executable via GraalVM (no JRE dependency)
  - System tray icon with Windows-native styling
- **macOS**: 
  - DMG package with .app bundle
  - Universal binary (Intel + Apple Silicon support)
  - Login Items integration for auto-start
  - Menu bar icon with native macOS appearance
  - Code signing and notarization for security
- **Portable**: Cross-platform JAR for manual installation and testing

## Performance Requirements

- **Startup time**: < 2 seconds to tray (Java 21 optimizations)
- **Memory usage**: < 35MB during idle, < 50MB during command execution
- **Command execution**: < 5 seconds overhead (excluding VS Code command time)
- **GUI responsiveness**: Tray menu opens in < 150ms, log viewer in < 300ms
- **Log display**: Real-time streaming with < 100ms update latency  
- **Scheduler precision**: ±30 seconds accuracy for interval execution
- **Native compilation**: < 1 second startup, < 20MB memory footprint

## Testing Strategy

### Unit Testing
- **Scheduler Logic**: Interval calculation, next run determination
- **Command Execution**: Mock VS Code CLI execution and output parsing
- **Configuration Management**: Config validation and persistence
- **Log Management**: Log formatting, rotation, and retention

### Integration Testing
- **End-to-end Scheduling**: Full scheduler cycle with actual command execution
- **Multi-platform GUI**: System tray integration on Windows and macOS
- **VS Code Detection**: Auto-discovery across different installation types
- **Error Scenarios**: Command failures, timeout handling, recovery mechanisms

### UI Testing
- **Tray Menu Functionality**: All menu items and state changes
- **Settings Window**: Configuration changes and validation
- **Log Viewer**: Real-time updates, filtering, and export
- **Notification System**: Platform-specific notification display

### Performance Testing
- **Memory Usage**: Long-running scheduler with multiple executions
- **Command Execution**: Performance under various VS Code configurations
- **GUI Responsiveness**: Tray and window response times under load
- **Log File Growth**: Large log management and rotation efficiency

## Documentation Requirements

- User installation guide
- Configuration reference
- Troubleshooting guide
- Developer documentation
- API documentation
- Platform-specific setup instructions

## Java 21 Specific Features Utilized

### Virtual Threads
- **Concurrent Extension Updates**: Process multiple extension updates simultaneously without traditional thread overhead
- **Non-blocking I/O**: Network requests to VS Code marketplace use Virtual Threads for better resource utilization
- **Scalable Scheduling**: Handle multiple VS Code installations concurrently

### Pattern Matching and Switch Expressions
- **Platform Detection**: Use pattern matching for OS and architecture detection
- **Configuration Parsing**: Simplified configuration validation using pattern matching
- **Error Handling**: Enhanced error categorization with pattern matching

### Records
- **Configuration Models**: Use records for immutable configuration objects
- **Extension Metadata**: Represent extension information with records
- **Event Data**: Service events and notifications as records

### Text Blocks
- **Command Templates**: Multi-line command templates for different platforms
- **Configuration Examples**: Embedded documentation with text blocks
- **Log Formatting**: Structured log message templates

### GraalVM Native Image Support
- **Fast Startup**: Native compilation for < 2 second startup times
- **Low Memory**: Reduced memory footprint for containerized deployments
- **No JRE Dependency**: Self-contained executables for easier distribution

## Future Enhancements

- **Project Loom Integration**: Advanced Virtual Thread patterns for even better concurrency
- **Web-based configuration interface** using lightweight embedded server
- **Extension usage analytics** with privacy-first approach
- **Custom extension repositories** support
- **Team/organization extension policies**
- **Integration with VS Code settings sync**
- **Extension backup and restore functionality**
- **Notification integrations** (Slack, Teams, etc.)
- **Foreign Function Interface (FFI)**: Direct system integration without JNI

## Implementation Milestones

### Milestone 1: MVP - Basic System Tray with Manual Update (2-3 weeks)

**Goal**: Core system tray application with manual VS Code extension update capability and real-time log viewing.

#### Features:
- **System Tray Integration**:
  - Basic tray icon with context menu
  - Windows: System tray with native Windows menu
  - macOS: Menu bar extra with native macOS styling
  - Auto-start functionality (basic registry/login items)

- **Manual Update Action**:
  - "Update VS Code Extensions" menu item
  - Execute `code --update-extensions` command
  - Background process execution using Virtual Threads
  - Command timeout handling (5 minutes default)

- **Real-Time Log Viewer**:
  - Dedicated log window accessible from tray menu
  - Live streaming of command stdout/stderr
  - Auto-scroll functionality during execution
  - Basic log export (save to file)
  - Simple status indicators (Running, Success, Error)

- **Basic Configuration**:
  - Minimal JSON config file for VS Code path override
  - Default VS Code detection (standard installation paths)
  - Log file location configuration

#### Technical Components:
```
src/main/java/com/vscode/updater/
├── Application.java                    // Main entry point
├── tray/
│   ├── SystemTrayManager.java         // Core tray functionality
│   └── TrayMenuBuilder.java           // Menu construction
├── executor/
│   ├── CommandExecutor.java           // VS Code command execution
│   └── OutputStreamCapture.java       // Real-time output capture
├── gui/
│   └── LogViewerWindow.java           // Log display window
├── config/
│   ├── BasicConfig.java (Record)      // Minimal configuration
│   └── ConfigManager.java             // Config persistence
└── util/
    └── ProcessUtils.java              // Process execution utilities
```

#### MVP Success Criteria:
- ✅ Tray icon appears on Windows and macOS
- ✅ Menu shows "Update VS Code Extensions" option
- ✅ Command executes in background without blocking UI
- ✅ Log viewer shows real-time command output
- ✅ Application can be quit from tray menu
- ✅ Basic error handling for command failures

---

### Milestone 2: Multi-Instance VS Code Support (2-3 weeks)

**Goal**: Detect and support multiple VS Code installations (stable + insiders) with individual control.

#### Features:
- **VS Code Discovery Engine**:
  - Automatic detection of VS Code installations
  - Support for VS Code stable and VS Code Insiders
  - Custom path configuration for non-standard installations
  - Installation validation (verify executable exists and works)

- **Multi-Instance Management**:
  - Tray submenu showing detected VS Code instances
  - Individual update actions per VS Code installation
  - Separate log sessions for each VS Code instance
  - Status tracking per installation

- **Enhanced Configuration**:
  - VS Code instance enable/disable toggles
  - Custom executable paths
  - Per-instance command arguments
  - Installation preference settings

- **Improved Log Management**:
  - Session-based logging (separate logs per execution)
  - Log viewer with instance filtering
  - Enhanced log formatting with timestamps
  - Log retention and rotation

#### Technical Components:
```
src/main/java/com/vscode/updater/
├── discovery/
│   ├── VSCodeDetector.java            // Multi-instance detection
│   ├── VSCodeInstance.java (Record)   // Instance representation
│   └── PlatformDetector.java          // Platform-specific paths
├── tray/
│   └── MultiInstanceMenuBuilder.java  // Dynamic menu for instances
├── logging/
│   ├── LogManager.java                // Session management
│   ├── LogEntry.java (Record)         // Structured log entries
│   └── LogViewer.java                 // Enhanced log display
├── config/
│   └── VSCodeConfig.java (Record)     // Instance configuration
└── platform/
    ├── WindowsDetection.java          // Windows-specific detection
    └── MacOSDetection.java            // macOS-specific detection
```

#### Milestone 2 Success Criteria:
- ✅ Auto-detects VS Code stable and Insiders installations
- ✅ Tray menu shows submenu with detected instances
- ✅ Can update extensions for specific VS Code instance
- ✅ Separate log sessions for each instance
- ✅ Configuration UI for managing detected instances
- ✅ Handles edge cases (missing installations, permissions)

---

### Milestone 3: Scheduling and Automation (3-4 weeks)

**Goal**: Add configurable scheduling system for automated extension updates.

#### Features:
- **Flexible Scheduler**:
  - Interval-based scheduling (minutes, hours, daily, weekly)
  - User-friendly scheduling configuration UI
  - Pause/resume scheduler functionality
  - Next execution time display in tray menu

- **Advanced Configuration UI**:
  - Dedicated Settings window accessible from tray
  - Scheduler configuration panel with intuitive controls
  - Per-instance scheduling options (optional)
  - Notification preferences

- **Enhanced User Experience**:
  - Scheduler status in tray menu (next run, last run)
  - Visual indicators for different states
  - System notifications for update completion
  - Run on startup with configurable delay

- **Robust Error Handling**:
  - Automatic retry with exponential backoff
  - Graceful handling of system sleep/wake
  - Recovery from missed schedules
  - Comprehensive error logging and user notification

- **Production Features**:
  - Application auto-updater (optional)
  - Crash recovery and state persistence
  - Performance optimization
  - Memory leak prevention

#### Technical Components:
```
src/main/java/com/vscode/updater/
├── scheduler/
│   ├── TaskScheduler.java             // Core scheduling engine
│   ├── IntervalCalculator.java        // Schedule computation
│   ├── SchedulerState.java (Record)   // Persistent scheduler state
│   └── ExecutionHistory.java          // Track execution history
├── gui/
│   ├── SettingsWindow.java            // Main settings interface
│   ├── SchedulerConfigPanel.java      // Scheduler configuration
│   └── AboutDialog.java               // Application info
├── notifications/
│   ├── NotificationManager.java       // Cross-platform notifications
│   ├── WindowsNotifications.java      // Windows Toast notifications
│   └── MacOSNotifications.java        // macOS Notification Center
├── persistence/
│   ├── StateManager.java              // Application state persistence
│   └── SchedulerPersistence.java      // Scheduler state storage
└── platform/
    └── AutoStartManager.java          // Startup integration
```

#### Milestone 3 Success Criteria:
- ✅ Configurable scheduling (5min to weekly intervals)
- ✅ Scheduler persists across application restarts
- ✅ Handles system sleep/wake scenarios correctly
- ✅ Rich settings UI for all configuration options
- ✅ Native system notifications
- ✅ Production-ready error handling and recovery
- ✅ Memory efficient long-running operation

---

## Development Timeline Summary

| Milestone | Duration | Core Features | Deliverable |
|-----------|----------|---------------|-------------|
| **M1: MVP** | 2-3 weeks | System tray + manual update + log viewer | Functional tray app with command execution |
| **M2: Multi-Instance** | 2-3 weeks | VS Code detection + multi-instance support | Support for stable + insiders |
| **M3: Scheduling** | 3-4 weeks | Automated scheduling + full configuration UI | Production-ready application |
| **Total** | **7-10 weeks** | | **Complete VS Code Extension Updater** |

## Success Criteria

- **Milestone 1**: Basic functionality working on Windows and macOS
- **Milestone 2**: Seamlessly handles multiple VS Code installations  
- **Milestone 3**: Production-ready with scheduling and full automation
- **Overall**: Reliable, user-friendly VS Code extension management tool