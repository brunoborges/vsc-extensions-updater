#!/bin/bash
# src/main/scripts/uninstall.sh - User-facing uninstall script

echo "Uninstalling VS Code Extension Updater..."

# Remove application
sudo rm -rf "/Applications/VS Code Extension Updater.app"

# Remove Launch Agent
rm -f "$HOME/Library/LaunchAgents/io.github.brunoborges.vscode-extension-updater.plist"

# Remove configuration and logs
rm -rf "$HOME/Library/Application Support/VSCodeExtensionUpdater"
rm -rf "$HOME/Library/Logs/VSCodeExtensionUpdater"

echo "Uninstallation completed."
echo "You may need to log out and back in to fully remove the Launch Agent."