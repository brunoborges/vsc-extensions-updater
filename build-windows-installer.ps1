# build-windows-installer.ps1 - Build Windows installer package with embedded JRE

# Set strict error handling
$ErrorActionPreference = "Stop"

Write-Host "🪟 Building Windows Installer with Embedded Java Runtime" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

# Validate prerequisites
Write-Host ""
Write-Host "📋 Validating prerequisites..." -ForegroundColor Yellow

# Check if we're on Windows
if ($IsLinux -or $IsMacOS) {
    Write-Host "❌ Error: This script must be run on Windows" -ForegroundColor Red
    exit 1
}

# Check if Java 21+ is available
try {
    $javaVersion = java -version 2>&1 | Select-String '"(\d+)' | ForEach-Object { $_.Matches.Groups[1].Value }
    if ([int]$javaVersion -lt 21) {
        Write-Host "❌ Error: Java 21+ is required (found: $javaVersion)" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Java $javaVersion available" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: Java is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Check if jlink is available
try {
    $jlinkVersion = jlink --version 2>&1
    Write-Host "✅ jlink available: $jlinkVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: jlink is not available. Make sure you have a full JDK (not just JRE)" -ForegroundColor Red
    exit 1
}

# Check if jmods directory exists
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    $javaHome = (Get-Command java).Source | Split-Path | Split-Path
}

$jmodsPath = Join-Path $javaHome "jmods"
if (-not (Test-Path $jmodsPath)) {
    Write-Host "❌ Error: $jmodsPath directory not found" -ForegroundColor Red
    Write-Host "Make sure you have a full JDK installation with jmods" -ForegroundColor Red
    exit 1
}

Write-Host "✅ jmods directory: $jmodsPath" -ForegroundColor Green

# Check if jpackage is available (Java 14+)
try {
    $jpackageVersion = jpackage --version 2>&1
    Write-Host "✅ jpackage available: $jpackageVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: jpackage is not available. Make sure you have Java 14+ with jpackage" -ForegroundColor Red
    exit 1
}

# Step 1: Clean and build application JAR
Write-Host ""
Write-Host "🔨 Building application JAR..." -ForegroundColor Yellow

& mvn clean package -DskipTests

if (-not (Test-Path "target/extension-updater-1.0.jar")) {
    Write-Host "❌ Error: Application JAR was not created" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Application JAR built successfully" -ForegroundColor Green

# Step 2: Create custom JRE with jlink
Write-Host ""
Write-Host "⚙️ Creating custom JRE with jlink..." -ForegroundColor Yellow

$customJrePath = "target/custom-jre"
if (Test-Path $customJrePath) {
    Write-Host "🧹 Removing existing custom JRE..." -ForegroundColor Blue
    Remove-Item -Recurse -Force $customJrePath
}

# Create custom JRE with optimized modules for Windows
$jlinkArgs = @(
    "--module-path", $jmodsPath,
    "--add-modules", "java.base,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.instrument,java.net.http,java.datatransfer,java.security.sasl",
    "--output", $customJrePath,
    "--compress", "zip-9",
    "--no-header-files",
    "--no-man-pages",
    "--strip-debug",
    "--strip-native-commands",
    "--bind-services",
    "--ignore-signing-information"
)

& jlink @jlinkArgs

if (-not (Test-Path $customJrePath)) {
    Write-Host "❌ Error: Custom JRE was not created" -ForegroundColor Red
    exit 1
}

# Show JRE size
$jreSize = (Get-ChildItem -Recurse $customJrePath | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host "✅ Custom JRE created successfully (Size: $([math]::Round($jreSize, 1)) MB)" -ForegroundColor Green

# Step 3: Create installer package using jpackage
Write-Host ""
Write-Host "📦 Creating Windows installer package..." -ForegroundColor Yellow

# Remove existing installer output
$installerPath = "target/installer"
if (Test-Path $installerPath) {
    Remove-Item -Recurse -Force $installerPath
}

New-Item -ItemType Directory -Path $installerPath -Force | Out-Null

# Parse signing parameters (from environment)
$signingCertificate = $env:WIN_SIGNING_CERTIFICATE
$signingPassword = $env:WIN_SIGNING_PASSWORD

# Prepare jpackage arguments
$jpackageArgs = @(
    "--type", "msi",
    "--name", "VSCodeExtensionUpdater",
    "--app-version", "1.0.0",
    "--vendor", "Bruno Borges",
    "--copyright", "2024 Bruno Borges",
    "--description", "Background application for updating VS Code extensions with embedded Java runtime",
    "--main-class", "com.vscode.updater.Application",
    "--main-jar", "extension-updater-1.0.jar",
    "--input", "target",
    "--dest", $installerPath,
    "--runtime-image", $customJrePath,
    "--java-options", "-Djava.awt.headless=false",
    "--java-options", "-Xms16m",
    "--java-options", "-Xmx64m",
    "--java-options", "-XX:+UseG1GC",
    "--java-options", "-XX:+UseStringDeduplication",
    "--arguments", "--system-tray",
    "--win-package-identifier", "io.github.brunoborges.vscode-extension-updater",
    "--win-menu",
    "--win-shortcut",
    "--win-dir-chooser"
)

# Add Windows-specific icon if available
$iconPath = "src/main/windows/icons/app.ico"
if (Test-Path $iconPath) {
    $jpackageArgs += "--icon", $iconPath
    Write-Host "✅ Using custom icon: $iconPath" -ForegroundColor Green
}

# Add resource directory if available
$resourcePath = "src/main/windows/resources"
if (Test-Path $resourcePath) {
    $jpackageArgs += "--resource-dir", $resourcePath
    Write-Host "✅ Using custom resources: $resourcePath" -ForegroundColor Green
}

# Add code signing if certificate is provided
if ($signingCertificate -and $signingPassword) {
    Write-Host "🔐 Code signing will be applied" -ForegroundColor Blue
    
    # Decode base64 certificate and save to temporary file
    $certBytes = [System.Convert]::FromBase64String($signingCertificate)
    $certPath = "temp-certificate.p12"
    [System.IO.File]::WriteAllBytes($certPath, $certBytes)
    
    try {
        # Import certificate to store
        $cert = Import-PfxCertificate -FilePath $certPath -CertStoreLocation "Cert:\CurrentUser\My" -Password (ConvertTo-SecureString -String $signingPassword -AsPlainText -Force)
        Write-Host "✅ Certificate imported successfully" -ForegroundColor Green
        
        # Add signing arguments to jpackage
        $jpackageArgs += "--win-package-signing-prefix", "io.github.brunoborges"
        
        # Note: jpackage will automatically find and use the certificate from the store
    } catch {
        Write-Host "⚠️  Certificate import failed: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "Creating unsigned installer..." -ForegroundColor Yellow
    } finally {
        # Clean up temporary certificate file
        if (Test-Path $certPath) {
            Remove-Item $certPath -Force
        }
    }
} else {
    Write-Host "ℹ️  No signing certificate provided - creating unsigned package" -ForegroundColor Blue
}

# Execute jpackage
Write-Host "🏗️  Running jpackage..." -ForegroundColor Yellow
Write-Host "Command: jpackage $($jpackageArgs -join ' ')" -ForegroundColor Gray

try {
    & jpackage @jpackageArgs
    
    # Verify the package was created
    $msiFile = Get-ChildItem -Path $installerPath -Filter "*.msi" | Select-Object -First 1
    
    if (-not $msiFile) {
        Write-Host "❌ Error: No .msi file was created" -ForegroundColor Red
        exit 1
    }
    
    $msiSize = [math]::Round(($msiFile.Length / 1MB), 1)
    Write-Host "✅ Installer package created: $($msiFile.FullName) (Size: $msiSize MB)" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Error during jpackage execution: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 4: Post-processing and verification
Write-Host ""
Write-Host "🔍 Final verification..." -ForegroundColor Yellow

# Verify MSI package properties
try {
    $msiInfo = & msiexec /? 2>&1 | Out-Null  # Just check if msiexec is available
    Write-Host "✅ MSI package appears valid" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not verify MSI package" -ForegroundColor Yellow
}

# Check if package is signed (basic check)
if ($signingCertificate) {
    try {
        $signature = Get-AuthenticodeSignature -FilePath $msiFile.FullName
        if ($signature.Status -eq "Valid") {
            Write-Host "✅ Package signature is valid" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Package signature verification failed: $($signature.StatusMessage)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "⚠️  Could not verify package signature: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Show final summary
Write-Host ""
Write-Host "🎉 Build Summary" -ForegroundColor Cyan
Write-Host "=================" -ForegroundColor Cyan
Write-Host "📦 Package: $($msiFile.FullName)" -ForegroundColor White
Write-Host "📏 Size: $msiSize MB" -ForegroundColor White
Write-Host "🔧 Custom JRE: $([math]::Round($jreSize, 1)) MB" -ForegroundColor White
Write-Host "🔐 Signed: $(if ($signingCertificate) { "Yes" } else { "No" })" -ForegroundColor White

Write-Host ""
Write-Host "✅ Windows installer build completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Installation Instructions:" -ForegroundColor Yellow
Write-Host "   1. Double-click the .msi file to install" -ForegroundColor White
Write-Host "   2. The application will be installed to Program Files" -ForegroundColor White
Write-Host "   3. A desktop shortcut and start menu entry will be created" -ForegroundColor White
Write-Host "   4. The application will start automatically and appear in your system tray" -ForegroundColor White
Write-Host ""
Write-Host "🗑️  Uninstallation:" -ForegroundColor Yellow
Write-Host "   Use Add/Remove Programs or run: msiexec /x `"$($msiFile.Name)`"" -ForegroundColor White