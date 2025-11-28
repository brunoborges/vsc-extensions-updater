#!/bin/bash
# build-linux-installer.sh - Build Linux DEB installer package with embedded JRE

set -e  # Exit on any error

# Ensure we are in the project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "🐧 Building Linux DEB Installer with Embedded Java Runtime"
echo "======================================================="

# Validate prerequisites
echo "📋 Validating prerequisites..."

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed or not in PATH"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -lt 21 ]]; then
    echo "❌ Error: Java 21+ is required (found: $JAVA_VERSION)"
    exit 1
fi

# Check if jpackage is available
if ! command -v jpackage &> /dev/null; then
    echo "❌ Error: jpackage is not available. Make sure you have a full JDK (not just JRE)"
    exit 1
fi

# Check if jlink is available
if ! command -v jlink &> /dev/null; then
    echo "❌ Error: jlink is not available."
    exit 1
fi

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven (mvn) is not available."
    exit 1
fi

# Check if dpkg-deb is available (required for DEB)
if ! command -v dpkg-deb &> /dev/null; then
    echo "❌ Error: dpkg-deb is not available. Install 'dpkg' package."
    exit 1
fi

# Check if fakeroot is available (often needed by jpackage for deb)
if ! command -v fakeroot &> /dev/null; then
    echo "⚠️  Warning: fakeroot is not available. It might be required for correct permission setting."
fi

echo "✅ Java $JAVA_VERSION with jpackage/jlink available"

# Step 1: Clean and build application JAR
echo ""
echo "🔨 Building application JAR..."
mvn clean package -DskipTests

JAR_FILE=$(find target -name "extension-updater-*.jar" | head -n1)
if [[ -z "$JAR_FILE" ]]; then
    echo "❌ Error: Application JAR was not created"
    exit 1
fi

echo "✅ Application JAR built successfully: $JAR_FILE"

# Step 2: Create custom JRE with jlink
echo ""
echo "⚙️ Creating custom JRE with jlink..."

# Use a temporary directory for the runtime image to avoid filesystem issues on WSL/mounted drives
RUNTIME_IMAGE="/tmp/vscode-updater-runtime-$$"
if [[ -d "$RUNTIME_IMAGE" ]]; then
    echo "🧹 Removing existing custom JRE..."
    rm -rf "$RUNTIME_IMAGE"
fi

# Ensure cleanup on exit
trap 'rm -rf "$RUNTIME_IMAGE"' EXIT

# Detect modules (simplified approach - add basic modules)
# For a real app, use jdeps to find exact modules: jdeps --print-module-deps ...
MODULES="java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.scripting,java.xml"

jlink \
  --add-modules "$MODULES" \
  --output "$RUNTIME_IMAGE" \
  --compress zip-6 \
  --no-header-files \
  --no-man-pages \
  --strip-debug

if [[ ! -d "$RUNTIME_IMAGE" ]]; then
    echo "❌ Error: Custom JRE was not created"
    exit 1
fi

echo "✅ Custom JRE created successfully at $RUNTIME_IMAGE"

# Step 3: Create installer package using jpackage
echo ""
echo "📦 Creating Linux DEB installer package..."

# Remove existing installer output
if [[ -d "target/installer" ]]; then
    rm -rf target/installer
fi
mkdir -p target/installer

# Prepare input directory
if [[ -d "target/installer-input" ]]; then
    rm -rf target/installer-input
fi
mkdir -p target/installer-input
cp "$JAR_FILE" target/installer-input/

JAR_NAME=$(basename "$JAR_FILE")
APP_NAME="VSCodeExtensionUpdater"
APP_VERSION="1.0.0"
VENDOR="Bruno Borges"
DESCRIPTION="Background application for updating VS Code extensions"
ICON_PATH="src/main/linux/icon.png"

JPACKAGE_CMD="jpackage \
  --type deb \
  --dest target/installer \
  --input target/installer-input \
  --name $APP_NAME \
  --app-version $APP_VERSION \
  --vendor \"$VENDOR\" \
  --copyright \"Copyright 2025 $VENDOR\" \
  --description \"$DESCRIPTION\" \
  --main-jar $JAR_NAME \
  --main-class com.vscode.updater.Application \
  --runtime-image \"$RUNTIME_IMAGE\" \
  --linux-shortcut \
  --linux-menu-group \"VSCode Tools\" \
  --linux-deb-maintainer \"$VENDOR <bruno.borges@example.com>\" \
  --linux-app-category \"utils\" \
  --java-options \"-Dfile.encoding=UTF-8\" \
  --verbose"

if [[ -f "$ICON_PATH" ]]; then
    echo "✅ Using icon: $ICON_PATH"
    JPACKAGE_CMD="$JPACKAGE_CMD --icon \"$ICON_PATH\""
else
    echo "⚠️  Icon not found at $ICON_PATH"
fi

echo "Running jpackage..."
eval $JPACKAGE_CMD

if [[ $? -ne 0 ]]; then
    echo "❌ Error: jpackage failed"
    exit 1
fi

DEB_FILE=$(find target/installer -name "*.deb" | head -n1)
if [[ -z "$DEB_FILE" ]]; then
    echo "❌ Error: No .deb file was created"
    exit 1
fi

echo ""
echo "🎉 Build Summary"
echo "================="
echo "📦 Package: $DEB_FILE"
echo "✅ Linux DEB installer build completed successfully!"
echo ""
echo "📋 Installation Instructions:"
echo "   sudo dpkg -i $DEB_FILE"
