@echo off
REM pre-uninstall.bat - Pre-uninstallation script for VS Code Extension Updater

echo Preparing to uninstall VS Code Extension Updater...

REM Stop any running instances
taskkill /F /IM "VSCodeExtensionUpdater.exe" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Stopped running VS Code Extension Updater process
)

REM Remove from Windows startup registry
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "VSCodeExtensionUpdater" /f >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Removed startup registry entry
)

REM Optional: Ask user if they want to keep configuration and logs
echo.
echo Do you want to keep your configuration and log files?
echo They are stored in: %APPDATA%\VSCodeExtensionUpdater
echo.
echo Press Y to keep files, N to remove everything, or any other key to skip
choice /c YN /n /t 30 /d Y >nul

if %ERRORLEVEL% EQU 2 (
    REM User chose to delete everything
    if exist "%APPDATA%\VSCodeExtensionUpdater" (
        rd /s /q "%APPDATA%\VSCodeExtensionUpdater"
        echo Removed application data directory
    )
) else (
    echo Keeping configuration and log files in %APPDATA%\VSCodeExtensionUpdater
)

echo Pre-uninstallation cleanup completed.

exit /b 0