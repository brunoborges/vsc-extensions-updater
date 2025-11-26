#!/bin/bash

# VS Code Extension Updater - Run Script
# This script runs the VS Code Extension Updater with the correct Java settings

echo "Starting VS Code Extension Updater..."
echo "Java Version: $(java -version 2>&1 | head -1)"
echo "JAR Size: $(du -h target/extension-updater-1.0.jar | cut -f1)"
echo ""

# Run the application with optimal settings for GUI
java -Djava.awt.headless=false \
     -Dapple.laf.useScreenMenuBar=true \
     -Dcom.apple.mrj.application.apple.menu.about.name="VS Code Extension Updater" \
     -jar target/extension-updater-1.0.jar "$@"