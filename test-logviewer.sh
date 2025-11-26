#!/bin/bash

# Test script to verify log viewer functionality
echo "Testing Log Viewer functionality..."

# Create a simple test that opens just the log viewer window
java -cp target/extension-updater-1.0.jar \
     -Djava.awt.headless=false \
     com.vscode.updater.gui.LogViewerWindow

echo "Log viewer test completed."