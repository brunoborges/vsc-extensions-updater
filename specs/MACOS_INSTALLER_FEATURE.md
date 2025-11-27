# macOS Installer Packages with Embedded Java Runtime

## Overview

This feature adds support for creating native macOS installer packages (.pkg) that include a custom Java Runtime Environment (JRE) using `jlink`, eliminating the need for users to have Java pre-installed on their systems.

## Objectives

- **Self-Contained Distribution**: Create standalone macOS packages with embedded Java runtime
- **Minimal Runtime Size**: Use `jlink` to create optimized JRE with only required modules
- **Native macOS Experience**: Professional installer with proper macOS integration
- **Universal Binary Support**: Support both Intel and Apple Silicon architectures
- **Code Signing & Notarization**: Apple-approved distribution for security compliance
- **Auto-Start Integration**: Seamless Login Items configuration during installation

## Technical Architecture

### 1. Build System Integration

#### 1.1 Maven Plugin Configuration
```xml
<!-- Add to pom.xml -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-toolchains-plugin</artifactId>
    <version>3.1.0</version>
    <configuration>
        <toolchains>
            <jdk>
                <version>21</version>
            </jdk>
        </toolchains>
    </configuration>
</plugin>

<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>1.6.0</version>
    <executions>
        <execution>
            <id>create-mac-installer</id>
            <phase>package</phase>
            <goals>
                <goal>jpackage</goal>
            </goals>
            <configuration>
                <type>PKG</type>
                <name>VSCodeExtensionUpdater</name>
                <appVersion>${project.version}</appVersion>
                <vendor>Bruno Borges</vendor>
                <copyright>2024 Bruno Borges</copyright>
                
                <!-- Application Configuration -->
                <mainClass>com.vscode.updater.Application</mainClass>
                <mainJar>extension-updater-${project.version}.jar</mainJar>
                <input>target</input>
                <destination>target/installer</destination>
                
                <!-- macOS Specific -->
                <macPackageIdentifier>io.github.brunoborges.vscode-extension-updater</macPackageIdentifier>
                <macPackageName>VS Code Extension Updater</macPackageName>
                <macSign>true</macSign>
                <macSigningKeychain>${mac.signing.keychain}</macSigningKeychain>
                <macSigningKeyUserName>${mac.signing.identity}</macSigningKeyUserName>
                
                <!-- Custom JRE Configuration -->
                <runtimeImage>target/custom-jre</runtimeImage>
                
                <!-- Launch Configuration -->
                <arguments>
                    <argument>--system-tray</argument>
                </arguments>
                <javaOptions>
                    <javaOption>-Djava.awt.headless=false</javaOption>
                    <javaOption>-Xms32m</javaOption>
                    <javaOption>-Xmx128m</javaOption>
                </javaOptions>
                
                <!-- macOS Bundle Configuration -->
                <options>
                    <option>--mac-package-signing-prefix</option>
                    <option>io.github.brunoborges</option>
                    <option>--mac-entitlements</option>
                    <option>src/main/macos/entitlements.plist</option>
                </options>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### 1.2 Custom JRE Creation with jlink
```xml
<!-- jlink execution for custom runtime -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-exec-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>create-custom-jre</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>jlink</executable>
                <arguments>
                    <argument>--module-path</argument>
                    <argument>${java.home}/jmods</argument>
                    <argument>--add-modules</argument>
                    <argument>java.base,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.instrument</argument>
                    <argument>--output</argument>
                    <argument>target/custom-jre</argument>
                    <argument>--compress</argument>
                    <argument>2</argument>
                    <argument>--no-header-files</argument>
                    <argument>--no-man-pages</argument>
                    <argument>--strip-debug</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 2. macOS-Specific Resources

#### 2.1 Directory Structure
```
src/main/macos/
├── entitlements.plist              # macOS entitlements for code signing
├── Info.plist.template             # Application bundle info
├── installer-scripts/
│   ├── preinstall.sh              # Pre-installation script
│   ├── postinstall.sh             # Post-installation script (Login Items)
│   └── Distribution.xml           # Custom installer behavior
├── icons/
│   ├── VSCodeUpdater.icns         # macOS application icon
│   └── installer-background.png   # Installer background image
└── launch-agents/
    └── io.github.brunoborges.vscode-extension-updater.plist
```

#### 2.2 Application Entitlements (entitlements.plist)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Required for system tray/menu bar access -->
    <key>com.apple.security.automation.apple-events</key>
    <true/>
    
    <!-- Required for launching VS Code -->
    <key>com.apple.security.automation.execute</key>
    <true/>
    
    <!-- Required for file system access to VS Code extensions -->
    <key>com.apple.security.files.user-selected.read-write</key>
    <true/>
    
    <!-- Required for reading VS Code configuration -->
    <key>com.apple.security.files.downloads.read-write</key>
    <true/>
    
    <!-- Required for network access (extension updates) -->
    <key>com.apple.security.network.client</key>
    <true/>
    
    <!-- Required for Login Items management -->
    <key>com.apple.security.automation.login-item</key>
    <true/>
</dict>
</plist>
```

#### 2.3 Post-Installation Script (postinstall.sh)
```bash
#!/bin/bash

# Post-installation script for VS Code Extension Updater
# This script sets up Login Items for automatic startup

INSTALL_PATH="/Applications/VS Code Extension Updater.app"
USER_NAME="${USER}"
LAUNCH_AGENT_PLIST="io.github.brunoborges.vscode-extension-updater.plist"

# Function to log messages
log_message() {
    echo "$(date): $1" >> /var/log/vscode-extension-updater-install.log
}

log_message "Starting post-installation setup"

# Create launch agent plist for current user
create_launch_agent() {
    local user_home="/Users/$USER_NAME"
    local launch_agents_dir="$user_home/Library/LaunchAgents"
    local plist_file="$launch_agents_dir/$LAUNCH_AGENT_PLIST"
    
    # Create LaunchAgents directory if it doesn't exist
    mkdir -p "$launch_agents_dir"
    
    # Create the plist file
    cat > "$plist_file" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>io.github.brunoborges.vscode-extension-updater</string>
    
    <key>ProgramArguments</key>
    <array>
        <string>/Applications/VS Code Extension Updater.app/Contents/MacOS/VS Code Extension Updater</string>
        <string>--system-tray</string>
    </array>
    
    <key>RunAtLoad</key>
    <true/>
    
    <key>KeepAlive</key>
    <false/>
    
    <key>StandardErrorPath</key>
    <string>$user_home/Library/Logs/VSCodeExtensionUpdater/startup.log</string>
    
    <key>StandardOutPath</key>
    <string>$user_home/Library/Logs/VSCodeExtensionUpdater/startup.log</string>
</dict>
</plist>
EOF

    # Set proper ownership and permissions
    chown "$USER_NAME:staff" "$plist_file"
    chmod 644 "$plist_file"
    
    log_message "Launch Agent plist created: $plist_file"
}

# Create log directory
LOG_DIR="/Users/$USER_NAME/Library/Logs/VSCodeExtensionUpdater"
mkdir -p "$LOG_DIR"
chown "$USER_NAME:staff" "$LOG_DIR"

# Create launch agent for auto-start
if [ -n "$USER_NAME" ] && [ "$USER_NAME" != "root" ]; then
    create_launch_agent
    log_message "Launch Agent configured for user: $USER_NAME"
else
    log_message "Warning: Could not determine current user for Launch Agent setup"
fi

# Set proper permissions for application
chown -R root:admin "$INSTALL_PATH"
chmod -R 755 "$INSTALL_PATH"

log_message "Post-installation setup completed successfully"

exit 0
```

### 3. Build Profile Configuration

#### 3.1 Maven Profile for macOS Installer
```xml
<!-- Add to pom.xml profiles section -->
<profile>
    <id>macos-installer</id>
    <activation>
        <os>
            <family>mac</family>
        </os>
    </activation>
    
    <properties>
        <!-- macOS specific properties -->
        <mac.signing.keychain>login.keychain</mac.signing.keychain>
        <mac.signing.identity>Developer ID Application: Bruno Borges</mac.signing.identity>
        <mac.notarization.profile>vscode-updater-notarization</mac.notarization.profile>
    </properties>
    
    <build>
        <plugins>
            <!-- Copy macOS resources -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <id>copy-macos-resources</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>copy-resources</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>target/macos-resources</outputDirectory>
                            <resources>
                                <resource>
                                    <directory>src/main/macos</directory>
                                    <filtering>true</filtering>
                                </resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Generate application icon -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-exec-plugin</artifactId>
                <executions>
                    <execution>
                        <id>generate-icns</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>exec</goal>
                        </goals>
                        <configuration>
                            <executable>iconutil</executable>
                            <arguments>
                                <argument>-c</argument>
                                <argument>icns</argument>
                                <argument>src/main/macos/icons/VSCodeUpdater.iconset</argument>
                                <argument>-o</argument>
                                <argument>target/macos-resources/VSCodeUpdater.icns</argument>
                            </arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

### 4. Universal Binary Support

#### 4.1 Architecture Detection and Building
```xml
<!-- Architecture-specific JRE creation -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-exec-plugin</artifactId>
    <executions>
        <!-- Intel x64 JRE -->
        <execution>
            <id>create-jre-x64</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>jlink</executable>
                <arguments>
                    <argument>--module-path</argument>
                    <argument>${java.home}/jmods</argument>
                    <argument>--add-modules</argument>
                    <argument>java.base,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.instrument</argument>
                    <argument>--output</argument>
                    <argument>target/custom-jre-x64</argument>
                    <argument>--compress</argument>
                    <argument>2</argument>
                    <argument>--no-header-files</argument>
                    <argument>--no-man-pages</argument>
                    <argument>--strip-debug</argument>
                </arguments>
            </configuration>
        </execution>
        
        <!-- ARM64 JRE (if available) -->
        <execution>
            <id>create-jre-arm64</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>jlink</executable>
                <arguments>
                    <argument>--module-path</argument>
                    <argument>${java.home}/jmods</argument>
                    <argument>--add-modules</argument>
                    <argument>java.base,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.instrument</argument>
                    <argument>--output</argument>
                    <argument>target/custom-jre-arm64</argument>
                    <argument>--compress</argument>
                    <argument>2</argument>
                    <argument>--no-header-files</argument>
                    <argument>--no-man-pages</argument>
                    <argument>--strip-debug</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 5. Code Signing and Notarization

#### 5.1 Code Signing Configuration
```bash
#!/bin/bash
# src/main/scripts/sign-and-notarize.sh

APP_PATH="target/installer/VS Code Extension Updater.app"
PKG_PATH="target/installer/VSCodeExtensionUpdater-${project.version}.pkg"
SIGNING_IDENTITY="Developer ID Application: Bruno Borges"
INSTALLER_IDENTITY="Developer ID Installer: Bruno Borges"
NOTARIZATION_PROFILE="vscode-updater-notarization"

echo "Signing application bundle..."
codesign --force --verify --verbose --sign "$SIGNING_IDENTITY" \
    --options runtime \
    --entitlements src/main/macos/entitlements.plist \
    "$APP_PATH"

echo "Creating signed installer package..."
productbuild --component "$APP_PATH" /Applications \
    --sign "$INSTALLER_IDENTITY" \
    "$PKG_PATH"

echo "Submitting for notarization..."
xcrun notarytool submit "$PKG_PATH" \
    --keychain-profile "$NOTARIZATION_PROFILE" \
    --wait

echo "Stapling notarization ticket..."
xcrun stapler staple "$PKG_PATH"

echo "Verifying final package..."
pkgutil --check-signature "$PKG_PATH"
spctl --assess --verbose=2 --type install "$PKG_PATH"

echo "Package ready: $PKG_PATH"
```

### 6. Installation Experience

#### 6.1 Custom Installer Distribution (Distribution.xml)
```xml
<?xml version="1.0" encoding="utf-8"?>
<installer-gui-script minSpecVersion="2">
    <title>VS Code Extension Updater</title>
    <organization>io.github.brunoborges</organization>
    
    <welcome file="welcome.html"/>
    <readme file="readme.html"/>
    <license file="license.html"/>
    <conclusion file="conclusion.html"/>
    
    <options customize="never" require-scripts="true" rootVolumeOnly="true"/>
    
    <volume-check>
        <allowed-os-versions>
            <os-version min="10.15"/>
        </allowed-os-versions>
    </volume-check>
    
    <choices-outline>
        <line choice="default">
            <line choice="vscode.extension.updater"/>
        </line>
    </choices-outline>
    
    <choice id="default"/>
    
    <choice id="vscode.extension.updater" visible="false">
        <pkg-ref id="io.github.brunoborges.vscode-extension-updater"/>
    </choice>
    
    <pkg-ref id="io.github.brunoborges.vscode-extension-updater">
        <bundle-version>
            <bundle id="io.github.brunoborges.vscode-extension-updater"/>
        </bundle-version>
    </pkg-ref>
    
    <product id="io.github.brunoborges.vscode-extension-updater" version="${project.version}"/>
</installer-gui-script>
```

### 7. Build Automation

#### 7.1 Maven Build Command
```bash
# Build macOS installer package
mvn clean package -Pmacos-installer

# Build with code signing and notarization
mvn clean package -Pmacos-installer \
    -Dmac.signing.identity="Developer ID Application: Bruno Borges" \
    -Dmac.notarization.profile="vscode-updater-notarization"
```

#### 7.2 GitHub Actions Workflow
```yaml
# .github/workflows/macos-installer.yml
name: macOS Installer

on:
  push:
    tags: ['v*']
  workflow_dispatch:

jobs:
  build-macos-installer:
    runs-on: macos-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        
    - name: Import signing certificates
      env:
        SIGNING_CERTIFICATE_P12_DATA: ${{ secrets.SIGNING_CERTIFICATE_P12_DATA }}
        SIGNING_CERTIFICATE_PASSWORD: ${{ secrets.SIGNING_CERTIFICATE_PASSWORD }}
      run: |
        echo $SIGNING_CERTIFICATE_P12_DATA | base64 --decode > certificate.p12
        security create-keychain -p temp_password temp.keychain
        security default-keychain -s temp.keychain
        security unlock-keychain -p temp_password temp.keychain
        security import certificate.p12 -k temp.keychain -P $SIGNING_CERTIFICATE_PASSWORD -T /usr/bin/codesign
        security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k temp_password temp.keychain
        
    - name: Set up notarization credentials
      env:
        NOTARIZATION_USERNAME: ${{ secrets.NOTARIZATION_USERNAME }}
        NOTARIZATION_PASSWORD: ${{ secrets.NOTARIZATION_PASSWORD }}
        NOTARIZATION_TEAM_ID: ${{ secrets.NOTARIZATION_TEAM_ID }}
      run: |
        xcrun notarytool store-credentials "vscode-updater-notarization" \
          --apple-id "$NOTARIZATION_USERNAME" \
          --password "$NOTARIZATION_PASSWORD" \
          --team-id "$NOTARIZATION_TEAM_ID"
          
    - name: Build macOS installer
      run: |
        mvn clean package -Pmacos-installer \
          -Dmac.signing.identity="Developer ID Application: Bruno Borges" \
          -Dmac.notarization.profile="vscode-updater-notarization"
          
    - name: Upload installer artifact
      uses: actions/upload-artifact@v3
      with:
        name: macos-installer
        path: target/installer/*.pkg
        
    - name: Release
      if: startsWith(github.ref, 'refs/tags/')
      uses: softprops/action-gh-release@v1
      with:
        files: target/installer/*.pkg
```

### 8. User Experience

#### 8.1 Installation Flow
1. **Download**: User downloads `.pkg` file from GitHub releases
2. **Security**: macOS Gatekeeper validates code signature and notarization
3. **Installation**: Native macOS installer with custom welcome/readme screens
4. **Auto-Start**: Post-install script configures Login Items automatically
5. **Launch**: Application appears in menu bar immediately after installation
6. **No Java Required**: Embedded JRE means no additional dependencies

#### 8.2 Uninstallation Support
```bash
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
```

### 9. Runtime Optimization

#### 9.1 Minimal JRE Module Analysis
The `jlink` command will include only the minimal required modules:

- **java.base**: Core Java runtime
- **java.desktop**: AWT/Swing for system tray
- **java.logging**: SLF4J backend support
- **java.management**: JMX for monitoring
- **java.naming**: JNDI if needed
- **java.security.jgss**: Security operations
- **java.instrument**: Potential for future monitoring

**Estimated Size Reduction**:
- Full JDK 21: ~350MB
- Custom JRE: ~45-60MB (85% reduction)

#### 9.2 Application Size Breakdown
```
VS Code Extension Updater.app/
├── Contents/
│   ├── Info.plist                    # ~1KB
│   ├── MacOS/
│   │   └── VS Code Extension Updater # ~2MB (launcher)
│   ├── runtime/                      # ~50MB (custom JRE)
│   │   ├── bin/java
│   │   ├── lib/
│   │   └── legal/
│   ├── app/
│   │   └── extension-updater.jar     # ~5MB (application + dependencies)
│   └── Resources/
│       └── VSCodeUpdater.icns        # ~200KB
Total: ~57MB
```

### 10. Quality Assurance

#### 10.1 Testing Checklist
- [ ] **Installation**: Verify installer works on clean macOS system
- [ ] **Code Signing**: Confirm valid signature and notarization
- [ ] **Auto-Start**: Test Login Items configuration
- [ ] **System Tray**: Verify menu bar integration
- [ ] **VS Code Detection**: Test with various VS Code installations
- [ ] **Updates**: Confirm extension update functionality
- [ ] **Uninstallation**: Test complete removal process
- [ ] **Architecture**: Test on both Intel and Apple Silicon Macs

#### 10.2 Performance Validation
- **Startup Time**: < 3 seconds to menu bar appearance
- **Memory Usage**: < 60MB during normal operation  
- **Bundle Size**: < 60MB total application size
- **Installation Time**: < 30 seconds for complete installation

### 11. Distribution Strategy

#### 11.1 Release Channels
1. **GitHub Releases**: Primary distribution via GitHub releases page
2. **Direct Download**: Hosted `.pkg` files with SHA256 checksums
3. **Future**: Potential Homebrew Cask for advanced users

#### 11.2 Version Management
- **Semantic Versioning**: Follow SemVer for package versions
- **Auto-Updates**: Future integration with Sparkle framework
- **Rollback**: Ability to install previous versions if needed

## Implementation Plan

### Phase 1: Basic Installer (1-2 weeks)
- Set up jpackage Maven plugin
- Create basic jlink configuration
- Implement minimal macOS package creation
- Test installation on clean system

### Phase 2: Enhanced Integration (1-2 weeks)
- Add code signing and notarization
- Implement Login Items auto-start
- Create proper macOS resources (icons, scripts)
- Test on multiple macOS versions

### Phase 3: Production Ready (1 week)
- Set up CI/CD pipeline
- Add comprehensive testing
- Create user documentation
- Optimize JRE size and performance

### Total Timeline: 4-5 weeks

## Success Criteria

- ✅ Self-contained `.pkg` installer with embedded Java runtime
- ✅ Proper macOS code signing and notarization
- ✅ Automatic Login Items configuration
- ✅ Native macOS installation experience
- ✅ < 60MB total package size
- ✅ Works on both Intel and Apple Silicon Macs
- ✅ No external Java dependency required
- ✅ Professional installation and user experience

This feature will significantly improve the user experience for macOS users by providing a professional, self-contained installer that requires no technical setup or Java installation.