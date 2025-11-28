# macOS Installer Build Instructions

This document provides step-by-step instructions for building the macOS installer package for VS Code Extension Updater.

## Prerequisites

- **macOS 10.15+** (for building)
- **JDK 21+** with jmods (OpenJDK or Oracle JDK)
- **Maven 3.9+**
- **Xcode Command Line Tools** (for code signing)

## Quick Start

### 1. Install Dependencies

```bash
# Install JDK 21 via Homebrew
brew install openjdk@21

# Verify jlink is available
jlink --version

# Verify jmods directory exists
ls $JAVA_HOME/jmods
```

### 2. Build Unsigned Installer (for testing)

```bash
# Build the application first
mvn clean package

# Build macOS installer package (unsigned)
bash installers/macos/build.sh
```

The installer will be created at: `target/installer/VSCodeExtensionUpdater-1.0.pkg`

### 3. Build Signed Installer (for distribution)

```bash
# Set up code signing identity and notarization profile
export MAC_SIGNING_IDENTITY="Developer ID Application: Your Name"
export MAC_NOTARIZATION_PROFILE="your-notarization-profile"

# Build signed installer
bash installers/macos/build.sh
```

## Detailed Configuration

### JRE Modules Included

The custom JRE includes only the necessary modules:

- `java.base` - Core Java runtime
- `java.desktop` - AWT/Swing for system tray
- `java.logging` - SLF4J backend support  
- `java.management` - JMX monitoring
- `java.naming` - JNDI operations
- `java.security.jgss` - Security operations
- `java.instrument` - Instrumentation support

### Size Optimization

| Component | Size |
|-----------|------|
| Full JDK 21 | ~350MB |
| Custom JRE | ~50MB |
| Application JAR | ~5MB |
| **Total Package** | **~57MB** |

### Code Signing Setup

1. **Developer ID Certificate**: Obtain from Apple Developer Portal
2. **Import Certificate**: Add to macOS Keychain
3. **Notarization Credentials**: Set up Apple ID app-specific password

```bash
# Store notarization credentials
xcrun notarytool store-credentials "vscode-updater-notarization" \
  --apple-id "your-apple-id@example.com" \
  --password "app-specific-password" \
  --team-id "YOUR_TEAM_ID"
```

## Manual Build Process

### Step 1: Create Custom JRE

```bash
jlink \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.instrument \
  --output target/custom-jre \
  --compress 2 \
  --no-header-files \
  --no-man-pages \
  --strip-debug
```

### Step 2: Package Application

```bash
# Build the application JAR
mvn clean package

# The shaded JAR will be at target/extension-updater-1.0.jar
```

### Step 3: Create macOS App Bundle

```bash
jpackage \
  --type PKG \
  --name "VS Code Extension Updater" \
  --app-version 1.0 \
  --vendor "Bruno Borges" \
  --copyright "2024 Bruno Borges" \
  --main-class com.vscode.updater.Application \
  --main-jar extension-updater-1.0.jar \
  --input target \
  --dest target/installer \
  --runtime-image target/custom-jre \
  --mac-package-identifier io.github.brunoborges.vscode-extension-updater \
  --mac-package-name "VS Code Extension Updater" \
  --mac-entitlements src/main/macos/entitlements.plist \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: Your Name"
```

## Testing

### Installation Testing

```bash
# Install the package
sudo installer -pkg target/installer/VSCodeExtensionUpdater-1.0.pkg -target /

# Verify installation
ls -la "/Applications/VS Code Extension Updater.app"

# Check Launch Agent was created
ls -la ~/Library/LaunchAgents/io.github.brunoborges.vscode-extension-updater.plist
```

### Uninstallation Testing

```bash
# Use the provided uninstall script
bash src/main/scripts/uninstall.sh

# Or manually remove
sudo rm -rf "/Applications/VS Code Extension Updater.app"
rm -f ~/Library/LaunchAgents/io.github.brunoborges.vscode-extension-updater.plist
```

## Troubleshooting

### Common Issues

**1. jlink command not found**
```bash
# Ensure JAVA_HOME is set correctly
export JAVA_HOME=/usr/local/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
```

**2. Missing jmods directory**
```bash
# Verify JDK installation includes jmods
ls "$JAVA_HOME/jmods" | head -5
```

**3. Code signing failures**
```bash
# Check available signing identities
security find-identity -v -p codesigning

# Verify keychain access
security list-keychains
```

**4. Notarization issues**
```bash
# Check stored credentials
xcrun notarytool history --keychain-profile "vscode-updater-notarization"
```

### Build Environment

**Required Environment Variables:**
```bash
export JAVA_HOME=/usr/local/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
export MAC_SIGNING_IDENTITY="Developer ID Application: Your Name"
export MAC_NOTARIZATION_PROFILE="vscode-updater-notarization"
```

## CI/CD Integration

The project includes GitHub Actions workflow (`.github/workflows/macos-installer.yml`) that:

1. ✅ Sets up JDK 21 with jmods
2. ✅ Caches Maven dependencies
3. ✅ Creates custom JRE with jlink
4. ✅ Builds signed installer package
5. ✅ Uploads artifacts and creates releases

### Required GitHub Secrets

For signed releases, configure these secrets:

- `SIGNING_CERTIFICATE_P12_DATA` - Base64 encoded .p12 certificate
- `SIGNING_CERTIFICATE_PASSWORD` - Certificate password
- `NOTARIZATION_USERNAME` - Apple ID email
- `NOTARIZATION_PASSWORD` - App-specific password
- `NOTARIZATION_TEAM_ID` - Apple Developer Team ID

## Distribution

### Direct Download
- Host `.pkg` files on GitHub Releases
- Provide SHA256 checksums for verification

### Future Options
- **Homebrew Cask**: For advanced users who prefer command-line installation
- **Mac App Store**: Requires sandbox compliance and different entitlements

## Best Practices

1. **Test on Clean System**: Always test installation on a fresh macOS VM
2. **Verify Code Signing**: Check signature validity with `codesign -v`
3. **Monitor Size**: Keep JRE module list minimal to reduce package size
4. **Update Dependencies**: Regularly update Maven plugins and dependencies
5. **Documentation**: Keep build instructions updated with any changes

This installer provides a professional, self-contained distribution method that eliminates Java installation requirements for end users.