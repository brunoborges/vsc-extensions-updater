#!/bin/bash

# Post-installation script for VS Code Extension Updater
# This script sets up Login Items for automatic startup

INSTALL_PATH="/Applications/VS Code Extension Updater.app"
USER_NAME="${USER}"
LAUNCH_AGENT_PLIST="io.github.brunoborges.vscode-extension-updater.plist"

# Function to log messages
log_message() {
    echo "$(date): $1" >> /var/log/vscode-extension-updater-install.log
}

log_message "Starting post-installation setup"

# Create launch agent plist for current user
create_launch_agent() {
    local user_home="/Users/$USER_NAME"
    local launch_agents_dir="$user_home/Library/LaunchAgents"
    local plist_file="$launch_agents_dir/$LAUNCH_AGENT_PLIST"
    
    # Create LaunchAgents directory if it doesn't exist
    mkdir -p "$launch_agents_dir"
    
    # Create the plist file
    cat > "$plist_file" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>io.github.brunoborges.vscode-extension-updater</string>
    
    <key>ProgramArguments</key>
    <array>
        <string>/Applications/VS Code Extension Updater.app/Contents/MacOS/VS Code Extension Updater</string>
        <string>--system-tray</string>
    </array>
    
    <key>RunAtLoad</key>
    <true/>
    
    <key>KeepAlive</key>
    <false/>
    
    <key>StandardErrorPath</key>
    <string>$user_home/Library/Logs/VSCodeExtensionUpdater/startup.log</string>
    
    <key>StandardOutPath</key>
    <string>$user_home/Library/Logs/VSCodeExtensionUpdater/startup.log</string>
</dict>
</plist>
EOF

    # Set proper ownership and permissions
    chown "$USER_NAME:staff" "$plist_file"
    chmod 644 "$plist_file"
    
    log_message "Launch Agent plist created: $plist_file"
}

# Create log directory
LOG_DIR="/Users/$USER_NAME/Library/Logs/VSCodeExtensionUpdater"
mkdir -p "$LOG_DIR"
chown "$USER_NAME:staff" "$LOG_DIR"

# Create launch agent for auto-start
if [ -n "$USER_NAME" ] && [ "$USER_NAME" != "root" ]; then
    create_launch_agent
    log_message "Launch Agent configured for user: $USER_NAME"
else
    log_message "Warning: Could not determine current user for Launch Agent setup"
fi

# Set proper permissions for application
chown -R root:admin "$INSTALL_PATH"
chmod -R 755 "$INSTALL_PATH"

log_message "Post-installation setup completed successfully"

exit 0