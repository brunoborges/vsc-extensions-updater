#!/bin/bash

# Pre-installation script for VS Code Extension Updater
# This script prepares the system for installation

log_message() {
    echo "$(date): $1" >> /var/log/vscode-extension-updater-install.log
}

log_message "Starting pre-installation setup"

# Check if the application is currently running
if pgrep -f "VS Code Extension Updater" > /dev/null; then
    log_message "Stopping running VS Code Extension Updater application"
    pkill -f "VS Code Extension Updater"
    sleep 2
fi

# Remove any existing launch agent to ensure clean installation
USER_NAME="${USER}"
if [ -n "$USER_NAME" ] && [ "$USER_NAME" != "root" ]; then
    LAUNCH_AGENT_PLIST="/Users/$USER_NAME/Library/LaunchAgents/io.github.brunoborges.vscode-extension-updater.plist"
    if [ -f "$LAUNCH_AGENT_PLIST" ]; then
        log_message "Removing existing launch agent: $LAUNCH_AGENT_PLIST"
        rm -f "$LAUNCH_AGENT_PLIST"
    fi
fi

# Remove previous installation if it exists
if [ -d "/Applications/VS Code Extension Updater.app" ]; then
    log_message "Removing previous installation"
    rm -rf "/Applications/VS Code Extension Updater.app"
fi

log_message "Pre-installation setup completed successfully"

exit 0