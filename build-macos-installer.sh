#!/bin/bash
# build-macos-installer.sh - Build macOS installer package with embedded JRE

set -e  # Exit on any error

echo "🍎 Building macOS Installer with Embedded Java Runtime"
echo "================================================="

# Validate prerequisites
echo "📋 Validating prerequisites..."

# Check if we're on macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo "❌ Error: This script must be run on macOS"
    exit 1
fi

# Check if Java 21+ is available
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed or not in PATH"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -lt 21 ]]; then
    echo "❌ Error: Java 21+ is required (found: $JAVA_VERSION)"
    exit 1
fi

# Check if jlink is available
if ! command -v jlink &> /dev/null; then
    echo "❌ Error: jlink is not available. Make sure you have a full JDK (not just JRE)"
    exit 1
fi

# Check if jmods directory exists
if [[ ! -d "$JAVA_HOME/jmods" ]]; then
    echo "❌ Error: $JAVA_HOME/jmods directory not found"
    echo "Make sure you have a full JDK installation with jmods"
    exit 1
fi

echo "✅ Java $JAVA_VERSION with jlink available"
echo "✅ jmods directory: $JAVA_HOME/jmods"

# Step 1: Clean and build application JAR
echo ""
echo "🔨 Building application JAR..."
mvn clean package -DskipTests

if [[ ! -f "target/extension-updater-1.0.jar" ]]; then
    echo "❌ Error: Application JAR was not created"
    exit 1
fi

echo "✅ Application JAR built successfully"

# Step 2: Create custom JRE with jlink
echo ""
echo "⚙️ Creating custom JRE with jlink..."

if [[ -d "target/custom-jre" ]]; then
    echo "🧹 Removing existing custom JRE..."
    rm -rf target/custom-jre
fi

jlink \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.instrument,java.net.http \
  --output target/custom-jre \
  --compress zip-9 \
  --no-header-files \
  --no-man-pages \
  --strip-debug \
  --strip-native-commands \
  --bind-services \
  --ignore-signing-information

if [[ ! -d "target/custom-jre" ]]; then
    echo "❌ Error: Custom JRE was not created"
    exit 1
fi

# Show JRE size
JRE_SIZE=$(du -sh target/custom-jre | cut -f1)
echo "✅ Custom JRE created successfully (Size: $JRE_SIZE)"

# Step 3: Create installer package using jpackage directly
echo ""
echo "📦 Creating macOS installer package..."

# Remove existing installer output
if [[ -d "target/installer" ]]; then
    rm -rf target/installer
fi

mkdir -p target/installer

# Parse signing parameters
SIGNING_IDENTITY="${MAC_SIGNING_IDENTITY:-}"
NOTARIZATION_PROFILE="${MAC_NOTARIZATION_PROFILE:-}"

# Base jpackage command
JPACKAGE_CMD="jpackage \
  --type PKG \
  --name \"VS Code Extension Updater\" \
  --app-version 1.0 \
  --vendor \"Bruno Borges\" \
  --copyright \"2024 Bruno Borges\" \
  --description \"Background application for updating VS Code extensions with embedded Java runtime\" \
  --main-class com.vscode.updater.Application \
  --main-jar extension-updater-1.0.jar \
  --input target \
  --dest target/installer \
  --runtime-image target/custom-jre \
  --java-options \"-Djava.awt.headless=false\" \
  --java-options \"-Xms16m\" \
  --java-options \"-Xmx64m\" \
  --java-options \"-XX:+UseG1GC\" \
  --java-options \"-XX:+UseStringDeduplication\" \
  --arguments \"--system-tray\" \
  --mac-package-identifier io.github.brunoborges.vscode-extension-updater \
  --mac-package-name \"VS Code Extension Updater\""

# Add code signing if identity is provided
if [[ -n "$SIGNING_IDENTITY" ]]; then
    echo "🔐 Code signing will be applied with identity: $SIGNING_IDENTITY"
    JPACKAGE_CMD="$JPACKAGE_CMD \
      --mac-sign \
      --mac-signing-key-user-name \"$SIGNING_IDENTITY\""
    
    # Add entitlements if file exists
    if [[ -f "src/main/macos/entitlements.plist" ]]; then
        JPACKAGE_CMD="$JPACKAGE_CMD \
          --mac-entitlements src/main/macos/entitlements.plist"
    fi
    
    # Add package signing prefix
    JPACKAGE_CMD="$JPACKAGE_CMD \
      --mac-package-signing-prefix io.github.brunoborges"
else
    echo "ℹ️  No signing identity provided - creating unsigned package"
fi

# Execute jpackage
echo "🏗️  Running jpackage..."
eval $JPACKAGE_CMD

# Verify the package was created
PKG_FILE=$(find target/installer -name "*.pkg" | head -n1)
if [[ -z "$PKG_FILE" ]]; then
    echo "❌ Error: No .pkg file was created"
    exit 1
fi

PKG_SIZE=$(du -sh "$PKG_FILE" | cut -f1)
echo "✅ Installer package created: $PKG_FILE (Size: $PKG_SIZE)"

# Step 4: Notarization (if credentials are provided)
if [[ -n "$SIGNING_IDENTITY" && -n "$NOTARIZATION_PROFILE" ]]; then
    echo ""
    echo "📝 Submitting for notarization..."
    
    if xcrun notarytool submit "$PKG_FILE" --keychain-profile "$NOTARIZATION_PROFILE" --wait; then
        echo "✅ Notarization successful"
        
        echo "🏷️  Stapling notarization ticket..."
        xcrun stapler staple "$PKG_FILE"
        echo "✅ Notarization ticket stapled"
    else
        echo "⚠️  Notarization failed, but package is still usable for testing"
    fi
fi

# Step 5: Final verification
echo ""
echo "🔍 Final verification..."

# Check package signature (if signed)
if [[ -n "$SIGNING_IDENTITY" ]]; then
    if pkgutil --check-signature "$PKG_FILE"; then
        echo "✅ Package signature is valid"
    else
        echo "⚠️  Package signature verification failed"
    fi
    
    if spctl --assess --verbose=2 --type install "$PKG_FILE"; then
        echo "✅ Package passes Gatekeeper assessment"
    else
        echo "⚠️  Package does not pass Gatekeeper assessment"
    fi
fi

# Show final summary
echo ""
echo "🎉 Build Summary"
echo "================="
echo "📦 Package: $PKG_FILE"
echo "📏 Size: $PKG_SIZE"
echo "🔧 Custom JRE: $JRE_SIZE"
echo "🔐 Signed: $(if [[ -n "$SIGNING_IDENTITY" ]]; then echo "Yes ($SIGNING_IDENTITY)"; else echo "No"; fi)"
echo "📝 Notarized: $(if [[ -n "$NOTARIZATION_PROFILE" ]]; then echo "Yes"; else echo "No"; fi)"

echo ""
echo "✅ macOS installer build completed successfully!"
echo ""
echo "📋 Installation Instructions:"
echo "   1. Double-click the .pkg file to install"
echo "   2. The application will be installed to /Applications/"
echo "   3. It will automatically appear in your menu bar"
echo "   4. A Login Item will be configured for automatic startup"
echo ""
echo "🗑️  Uninstallation:"
echo "   Run: bash src/main/scripts/uninstall.sh"