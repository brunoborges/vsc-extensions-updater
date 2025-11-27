@echo off
REM post-install.bat - Post-installation script for VS Code Extension Updater

echo Setting up VS Code Extension Updater...

REM Get installation directory
set "INSTALL_DIR=%~dp0"

REM Create application data directory
set "APPDATA_DIR=%APPDATA%\VSCodeExtensionUpdater"
if not exist "%APPDATA_DIR%" (
    mkdir "%APPDATA_DIR%"
    echo Created application data directory: %APPDATA_DIR%
)

REM Create default configuration
set "CONFIG_FILE=%APPDATA_DIR%\config.properties"
if not exist "%CONFIG_FILE%" (
    echo # VS Code Extension Updater Configuration > "%CONFIG_FILE%"
    echo # Auto-generated during installation >> "%CONFIG_FILE%"
    echo. >> "%CONFIG_FILE%"
    echo # Scheduling configuration >> "%CONFIG_FILE%"
    echo update.schedule.enabled=true >> "%CONFIG_FILE%"
    echo update.schedule.interval=daily >> "%CONFIG_FILE%"
    echo update.schedule.time=09:00 >> "%CONFIG_FILE%"
    echo. >> "%CONFIG_FILE%"
    echo # Notification settings >> "%CONFIG_FILE%"
    echo notifications.enabled=true >> "%CONFIG_FILE%"
    echo notifications.sound=false >> "%CONFIG_FILE%"
    echo. >> "%CONFIG_FILE%"
    echo # Logging configuration >> "%CONFIG_FILE%"
    echo logging.level=INFO >> "%CONFIG_FILE%"
    echo logging.file=%APPDATA_DIR%\updater.log >> "%CONFIG_FILE%"
    echo. >> "%CONFIG_FILE%"
    echo # Performance settings >> "%CONFIG_FILE%"
    echo performance.max.concurrent.updates=2 >> "%CONFIG_FILE%"
    echo performance.network.timeout=30 >> "%CONFIG_FILE%"
    echo. >> "%CONFIG_FILE%"
    echo Created default configuration file: %CONFIG_FILE%
)

REM Create logs directory
set "LOGS_DIR=%APPDATA_DIR%\logs"
if not exist "%LOGS_DIR%" (
    mkdir "%LOGS_DIR%"
    echo Created logs directory: %LOGS_DIR%
)

REM Add to Windows startup (optional - user can configure this in the app)
echo Installation completed successfully!
echo.
echo The VS Code Extension Updater has been installed and configured.
echo You can configure startup behavior from the application's system tray menu.

exit /b 0