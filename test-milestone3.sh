#!/bin/bash

# Test Milestone 3 - Complete VS Code Extension Updater
echo "🧪 Testing VS Code Extension Updater - Milestone 3"
echo "=================================================="

# Build and run the complete application
echo "📦 Building application..."
mvn clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
    echo ""
    
    echo "📊 Application Statistics:"
    echo "  JAR Size: $(du -h target/extension-updater-1.0.jar | cut -f1)"
    echo "  Java Files: $(find src/main -name "*.java" | wc -l)"
    echo "  Test Files: $(find src/test -name "*.java" | wc -l)"
    echo "  Lines of Code: $(find src/main -name "*.java" -exec wc -l {} + | tail -1)"
    echo ""
    
    echo "🚀 Starting VS Code Extension Updater..."
    echo "Features to test:"
    echo "  ✅ Multi-instance detection (VS Code + Insiders)"
    echo "  ✅ Dynamic tray menu with scheduling controls"
    echo "  ✅ Settings window with full configuration"
    echo "  ✅ Real-time logging and status updates"
    echo "  ✅ Background scheduling automation"
    echo ""
    
    echo "Right-click the tray icon to explore all features!"
    echo "Press Ctrl+C to exit when testing is complete."
    echo ""
    
    # Run the application
    ./run.sh
else
    echo "❌ Build failed"
    exit 1
fi