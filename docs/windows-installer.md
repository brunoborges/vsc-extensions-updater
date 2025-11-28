# Windows Installer Package

This document provides comprehensive information about building and distributing the Windows installer for VS Code Extension Updater.

## Overview

The Windows installer uses Microsoft's `jpackage` tool (introduced in Java 14) to create a native MSI package with an embedded, optimized Java Runtime Environment (JRE). This eliminates the need for users to have Java pre-installed on their systems.

## Key Features

### 🎯 User Experience
- **One-click installation**: Standard Windows MSI installer
- **No dependencies**: Embedded custom JRE (~90MB)
- **Native integration**: Desktop shortcut, Start Menu entry, system tray
- **Automatic startup**: Optional configuration during/after installation
- **Professional uninstall**: Standard Windows Add/Remove Programs support

### 🔧 Technical Features
- **Optimized runtime**: Custom JRE with only required modules
- **Code signing**: Support for certificate-based signing (release builds)
- **MSI package**: Industry standard Windows installer format
- **jlink optimization**: 55% size reduction compared to full JRE
- **G1GC tuning**: Optimized for low-memory desktop applications

## Build Requirements

### System Requirements
- **Windows 10/11** (64-bit)
- **Java Development Kit 21+** with jpackage support
- **PowerShell 5.1+** or PowerShell Core
- **Maven 3.6+**
- **Windows SDK** (for advanced features, optional)

### Optional Requirements
- **Code signing certificate** (for production releases)
- **Windows Application Certification Kit** (for validation)
- **Git** (for source control integration)

## Building the Installer

### Quick Start
```powershell
# Clone and build
git clone https://github.com/brunoborges/vsc-extensions-updater.git
cd vsc-extensions-updater

# Build installer (unsigned)
.\installers\windows\build.ps1
```

### GitHub Actions (Recommended)
The project includes a comprehensive GitHub Actions workflow for automated builds:

```yaml
# Trigger manually or on releases
name: Windows Installer
on:
  push:
    tags: ['v*']
  workflow_dispatch:
```

### Manual Build Process
1. **Prepare environment**:
   ```powershell
   # Verify Java and tools
   java -version        # Should be 21+
   jlink --version      # Should be available
   jpackage --version   # Should be available
   ```

2. **Build application**:
   ```powershell
   mvn clean package -DskipTests
   ```

3. **Create installer**:
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
   .\installers\windows\build.ps1
   ```

4. **Find output**:
   ```
   target/installer/VSCodeExtensionUpdater-1.0.0.msi
   ```

## Package Structure

### Installation Layout
```
%ProgramFiles%\VSCodeExtensionUpdater\
├── bin/
│   └── VSCodeExtensionUpdater.exe          # Main executable
├── lib/
│   ├── app/
│   │   └── extension-updater-1.0.jar       # Application JAR
│   └── runtime/                            # Custom JRE
│       ├── bin/
│       ├── lib/
│       └── conf/
└── VSCodeExtensionUpdater.cfg              # Launch configuration
```

### User Data
```
%APPDATA%\VSCodeExtensionUpdater\
├── config.properties                       # User configuration
├── logs/
│   └── updater.log                         # Application logs
└── cache/                                  # Extension cache
```

## Configuration

### Installer Customization

#### Application Metadata
```powershell
# In build-windows-installer.ps1
$jpackageArgs += @(
    "--name", "VSCodeExtensionUpdater",
    "--app-version", "1.0.0",
    "--vendor", "Bruno Borges",
    "--copyright", "2024 Bruno Borges",
    "--description", "Background application for updating VS Code extensions"
)
```

#### Windows Integration
```powershell
# Enable Windows features
$jpackageArgs += @(
    "--win-menu",              # Start Menu entry
    "--win-shortcut",          # Desktop shortcut
    "--win-dir-chooser",       # Custom install directory option
    "--win-package-identifier", "io.github.brunoborges.vscode-extension-updater"
)
```

#### JVM Optimization
```powershell
# Memory and performance tuning
$jpackageArgs += @(
    "--java-options", "-Xms16m",                    # Initial heap: 16MB
    "--java-options", "-Xmx64m",                    # Maximum heap: 64MB
    "--java-options", "-XX:+UseG1GC",              # G1 garbage collector
    "--java-options", "-XX:+UseStringDeduplication" # String optimization
)
```

### Custom Resources

#### Icons
- Place `app.ico` in `src/main/windows/icons/`
- Multiple resolutions: 16×16, 32×32, 48×48, 256×256
- 32-bit color with alpha channel

#### Installer Resources
```
src/main/windows/resources/
├── license.rtf          # License agreement (RTF format)
├── banner.bmp           # Installer banner (493×58px)
└── dialog.bmp           # Installer dialog background (493×312px)
```

#### Post-Installation Scripts
```
src/main/windows/installer-scripts/
├── post-install.bat     # Run after installation
└── pre-uninstall.bat   # Run before uninstallation
```

## Code Signing

### Certificate Requirements
- **Authenticode certificate** from a trusted CA
- **Extended Validation (EV) certificate** recommended for immediate trust
- **Base64 encoded** for GitHub Actions secrets

### Configuration
```powershell
# Environment variables for signing
$env:WIN_SIGNING_CERTIFICATE = "base64-encoded-certificate-data"
$env:WIN_SIGNING_PASSWORD = "certificate-password"
```

### GitHub Secrets
```yaml
# Required secrets for signed releases
WIN_SIGNING_CERTIFICATE: ${{ secrets.WIN_SIGNING_CERTIFICATE }}
WIN_SIGNING_PASSWORD: ${{ secrets.WIN_SIGNING_PASSWORD }}
```

### Verification
```powershell
# Check package signature
Get-AuthenticodeSignature target\installer\*.msi

# Windows verification
signtool verify /pa target\installer\*.msi
```

## Distribution

### Release Workflow
1. **Tag release**: Create Git tag (e.g., `v1.0.0`)
2. **Automated build**: GitHub Actions builds and signs installer
3. **GitHub Release**: Automatically creates release with MSI attachment
4. **Download**: Users download MSI from GitHub Releases

### Direct Distribution
```powershell
# Upload to file hosting
# - GitHub Releases (recommended)
# - Microsoft Store (requires certification)
# - Corporate internal distribution
# - Direct download from website
```

## Troubleshooting

### Common Build Issues

#### Missing jpackage
```
Error: jpackage is not available
```
**Solution**: Ensure Java 14+ JDK (not JRE) is installed

#### Missing jmods
```
Error: jmods directory not found
```
**Solution**: Install full JDK with module system files

#### PowerShell Execution Policy
```
Error: Scripts are disabled on this system
```
**Solution**: 
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
```

#### Code Signing Failures
```
Error: Certificate import failed
```
**Solution**: Verify certificate format and password

### Installation Issues

#### Windows Defender Warning
**Cause**: Unsigned installer or unknown publisher
**Solution**: Use code signing certificate or add exclusion

#### Installation Blocked
**Cause**: Windows SmartScreen protection
**Solution**: Use EV certificate or "Run anyway" option

#### Permission Denied
**Cause**: Insufficient privileges
**Solution**: Run installer as administrator

## Performance Optimization

### JRE Size Optimization
The custom JRE includes only essential modules:
- `java.base` - Core functionality
- `java.desktop` - GUI components  
- `java.logging` - Logging framework
- `java.management` - JMX support
- `java.naming` - JNDI services
- `java.security.jgss` - Security services
- `java.instrument` - Instrumentation
- `java.net.http` - HTTP client

### Compression Settings
```powershell
# jlink compression
--compress zip-9           # Maximum compression
--strip-debug             # Remove debug information  
--strip-native-commands   # Remove native tools
--no-header-files         # Remove C header files
--no-man-pages           # Remove manual pages
```

### Memory Tuning
- **Initial heap**: 16MB (sufficient for startup)
- **Maximum heap**: 64MB (adequate for VS Code extension management)
- **Garbage collector**: G1GC (optimized for low-latency)
- **String deduplication**: Reduces memory footprint

## Security Considerations

### Code Signing Benefits
- **Windows Defender compatibility**: Reduced false positives
- **SmartScreen trust**: Fewer user warnings
- **Professional appearance**: Verified publisher information
- **Integrity verification**: Tamper detection

### Installation Security
- **MSI validation**: Windows validates package integrity
- **Administrative privileges**: Required for Program Files installation
- **Uninstall tracking**: Proper Windows registration
- **File association**: Optional extension registration

## Testing

### Validation Checklist
- [ ] **Installation**: MSI installs without errors
- [ ] **Functionality**: Application starts and works correctly  
- [ ] **System tray**: Icon appears and menu functions
- [ ] **Shortcuts**: Desktop and Start Menu shortcuts work
- [ ] **Uninstall**: Clean removal via Add/Remove Programs
- [ ] **Upgrade**: New version installs over old version
- [ ] **Signing**: Certificate validation (if signed)

### Test Environments
- **Windows 10** (minimum supported version)
- **Windows 11** (latest features)
- **Virtual machines** (clean environment testing)
- **Different user privileges** (admin vs. standard user)

## Future Enhancements

### Planned Features
- **Automatic updates**: Self-updating installer mechanism
- **Silent installation**: Command-line installation options
- **Multi-language support**: Localized installer resources
- **Custom themes**: Installer branding customization
- **Microsoft Store**: Store distribution option

### Technical Improvements
- **Install4j integration**: Professional installer framework
- **MSI custom actions**: Advanced installation logic
- **Windows Service option**: Background service mode
- **Registry integration**: Deep Windows integration
- **Event log support**: Windows Event Log integration

---

## Summary

The Windows installer provides a professional, user-friendly installation experience with:
- **Zero dependencies**: Embedded Java runtime
- **Native Windows integration**: MSI, shortcuts, system tray
- **Security**: Code signing support for trusted installation
- **Performance**: Optimized JRE and memory usage
- **Maintainability**: Automated GitHub Actions build pipeline

This implementation ensures that Windows users can install and use the VS Code Extension Updater with minimal friction while maintaining security and professional standards.