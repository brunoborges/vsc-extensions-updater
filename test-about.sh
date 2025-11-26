#!/bin/bash

# Test script to verify About dialog functionality
echo "Testing About Dialog functionality..."

# Create a simple test that opens just the About dialog
java -cp target/extension-updater-1.0.jar \
     -Djava.awt.headless=false \
     com.vscode.updater.gui.AboutDialog

echo "About dialog test completed."