# macOS Installer Package with Embedded Java Runtime - Enhancement Specification

## Overview

This specification outlines the enhanced macOS installer feature that creates self-contained application bundles with optimized Java Runtime using `jlink` technology. The goal is to provide users with a seamless installation experience without requiring Java to be pre-installed on their systems.

## Objectives

### Primary Goals
- **Zero Java Dependencies**: Package application with embedded, optimized JRE
- **Minimal Size**: Create smallest possible runtime using jlink module selection
- **Professional Distribution**: Apple-approved, signed, and notarized packages
- **Native macOS Experience**: Proper installer behavior and system integration
- **Universal Support**: Compatible with both Intel and Apple Silicon Macs
- **Memory Efficiency**: Optimized JVM settings for background operation

### Secondary Goals
- **Automated Build Process**: CI/CD integration for release automation
- **Security Compliance**: Code signing and notarization for Gatekeeper approval
- **Easy Updates**: Support for future application updates through the installer
- **System Integration**: Automatic startup configuration and menu bar integration

## Technical Architecture

### 1. Build System Enhancement

#### 1.1 JLink Optimization
```bash
# Enhanced jlink command with advanced optimizations
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
```

**Module Selection Rationale:**
- `java.base`: Core Java functionality (required)
- `java.desktop`: Swing/AWT for GUI components and system tray
- `java.logging`: Application logging support
- `java.management`: JMX monitoring capabilities
- `java.naming`: JNDI support for potential network operations
- `java.security.jgss`: Security extensions
- `java.instrument`: JVM instrumentation support
- `java.net.http`: HTTP client for VS Code API communication

#### 1.2 Memory Optimization
```bash
# Optimized JVM arguments for background operation
--java-options "-Xms16m"                    # Minimal initial heap
--java-options "-Xmx64m"                    # Conservative max heap
--java-options "-XX:+UseG1GC"               # Low-latency garbage collector
--java-options "-XX:+UseStringDeduplication" # Memory optimization
```

#### 1.3 Maven Profile Integration
```xml
<profile>
    <id>macos-installer</id>
    <activation>
        <os><family>mac</family></os>
    </activation>
    <properties>
        <jlink.modules>java.base,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.instrument,java.net.http</jlink.modules>
    </properties>
    <!-- Plugin configurations for jlink and jpackage -->
</profile>
```

### 2. Size Optimization Analysis

#### 2.1 Expected Runtime Sizes
- **Full JRE 21**: ~200MB
- **Optimized Custom JRE**: ~40-60MB
- **Application JAR**: ~5-10MB
- **Total Package Size**: ~50-70MB

#### 2.2 Compression Benefits
```
Compression Level    | Size Reduction | Build Time Impact
zip-0 (none)        | 0%            | Fastest
zip-6 (default)     | ~15%          | Moderate
zip-9 (maximum)     | ~25%          | Slower but acceptable
```

### 3. Package Structure

#### 3.1 Application Bundle Layout
```
VS Code Extension Updater.app/
├── Contents/
│   ├── Info.plist              # macOS application metadata
│   ├── MacOS/
│   │   └── VS Code Extension Updater  # Launch script
│   ├── Resources/
│   │   ├── app/
│   │   │   └── extension-updater-1.0.jar
│   │   └── app-icon.icns       # Application icon
│   └── runtime/                # Custom JRE from jlink
│       ├── bin/
│       ├── conf/
│       ├── legal/
│       └── lib/
```

#### 3.2 Installer Package Contents
```
VS Code Extension Updater-1.0.pkg
├── Distribution.xml            # Installer configuration
├── Resources/
│   ├── preinstall.sh          # Pre-installation script
│   ├── postinstall.sh         # Post-installation script
│   └── background.png         # Installer background
└── Payload/                   # Application bundle
```

### 4. Installation Process

#### 4.1 Pre-Installation Checks
- Verify macOS compatibility (10.15+)
- Check available disk space (minimum 100MB)
- Terminate existing application instances

#### 4.2 Installation Steps
1. **Application Deployment**: Install to `/Applications/`
2. **Permissions Setup**: Configure file permissions
3. **Launch Agent Creation**: Set up automatic startup
4. **Initial Configuration**: Create user preferences directory

#### 4.3 Post-Installation Configuration
```bash
# postinstall.sh responsibilities
- Create ~/Library/Application Support/VSCodeExtensionUpdater/
- Set up LaunchAgent for automatic startup
- Register application with system
- Initial configuration file creation
```

### 5. Code Signing and Notarization

#### 5.1 Signing Requirements
- **Developer ID Application**: For application bundle signing
- **Developer ID Installer**: For package signing
- **Entitlements**: Required capabilities definition

#### 5.2 Notarization Process
```bash
# Submit for notarization
xcrun notarytool submit package.pkg \
  --keychain-profile "profile-name" \
  --wait

# Staple notarization ticket
xcrun stapler staple package.pkg
```

#### 5.3 Entitlements Configuration
```xml
<!-- src/main/macos/entitlements.plist -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" 
                       "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.app-sandbox</key>
    <false/>
    <key>com.apple.security.network.client</key>
    <true/>
    <key>com.apple.security.automation.apple-events</key>
    <true/>
</dict>
</plist>
```

### 6. Build Automation

#### 6.1 GitHub Actions Integration
```yaml
- name: Build macOS installer
  run: |
    mvn clean package -Pmacos-installer -DskipTests
    ./build-macos-installer.sh
```

#### 6.2 Build Script Enhancement
```bash
#!/bin/bash
# build-macos-installer.sh - Enhanced version
set -e

# Prerequisite validation
# JLink runtime creation
# JPackage installer generation
# Code signing (if configured)
# Notarization (if configured)
# Validation and verification
```

### 7. Testing Strategy

#### 7.1 Automated Testing
- **Build Verification**: Ensure installer creates successfully
- **Package Structure**: Validate bundle contents
- **Installation Testing**: Automated installation on clean VM
- **Functionality Testing**: Verify application starts and runs

#### 7.2 Manual Testing Scenarios
- Fresh macOS installation
- Existing Java installation present
- Upgrade from previous version
- Uninstallation and cleanup

### 8. Performance Considerations

#### 8.1 Startup Optimization
- **Class Data Sharing**: Pre-compiled class archives
- **Tiered Compilation**: Optimized JIT compilation
- **Memory Layout**: Efficient heap organization

#### 8.2 Runtime Efficiency
- **G1 Garbage Collector**: Low-latency collection
- **String Deduplication**: Memory optimization
- **Background Thread Priorities**: System-friendly operation

### 9. Error Handling and Diagnostics

#### 9.1 Installation Failure Recovery
- Clear error messages for common issues
- Cleanup of partial installations
- Diagnostic information collection

#### 9.2 Runtime Diagnostics
- Application logging configuration
- Performance monitoring capabilities
- Remote diagnostic support

### 10. Future Enhancements

#### 10.1 Short-term Improvements
- **Universal Binaries**: Native ARM64/x86_64 support
- **Incremental Updates**: Delta update mechanism
- **Configuration Migration**: Seamless preference migration

#### 10.2 Long-term Goals
- **Auto-Update Framework**: Self-updating capability
- **Multi-Language Support**: Internationalization
- **Advanced Analytics**: Usage metrics collection

## Implementation Timeline

### Phase 1: Enhanced JLink Integration (Completed)
- ✅ Optimized module selection
- ✅ Maximum compression configuration
- ✅ Memory-optimized JVM settings
- ✅ Maven profile enhancement

### Phase 2: Build Process Refinement (Current)
- 🔄 Enhanced build script
- 🔄 Automated testing integration
- 🔄 Error handling improvements

### Phase 3: Advanced Features (Future)
- ⏳ Universal binary support
- ⏳ Auto-update mechanism
- ⏳ Advanced monitoring

## Success Metrics

### Technical Metrics
- **Package Size**: < 70MB total
- **Installation Time**: < 30 seconds
- **Memory Footprint**: < 64MB runtime
- **Startup Time**: < 3 seconds

### User Experience Metrics
- **Installation Success Rate**: > 99%
- **Post-Installation Issues**: < 1%
- **User Satisfaction**: Positive feedback
- **Support Requests**: Minimal

## Conclusion

This enhanced macOS installer feature provides a professional, efficient, and user-friendly distribution mechanism for the VS Code Extension Updater. By leveraging jlink optimization and proper macOS integration, we deliver a seamless installation experience while maintaining small package sizes and optimal performance.

The implementation balances technical excellence with practical usability, ensuring both developers and end-users benefit from a well-engineered solution.