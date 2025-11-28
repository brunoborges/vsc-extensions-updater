# build-windows-installer.ps1
# Script to build a Windows Installer (MSI) using jpackage
# Requires: JDK 14+ (with jpackage), Maven, and WiX Toolset 3.x

$ErrorActionPreference = "Stop"

# --- Configuration ---
$AppName = "VSCodeExtensionUpdater"
$AppVersion = "1.0.0"
$Vendor = "Bruno Borges"
$Copyright = "Copyright 2025 Bruno Borges"
$Description = "Background application for updating VS Code extensions"
$MainClass = "com.vscode.updater.Application"

# Paths
$TargetDir = Resolve-Path "target" -ErrorAction SilentlyContinue
if (-not $TargetDir) { New-Item -ItemType Directory -Path "target" | Out-Null; $TargetDir = Resolve-Path "target" }

$InstallerInput = Join-Path $TargetDir "installer-input"
$InstallerOutput = Join-Path $TargetDir "installer-output"
$RuntimeImage = Join-Path $TargetDir "runtime-image"

# --- Helper Functions ---

function Check-Command($cmd) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        return $false
    }
    return $true
}

function Clean-Directory($path) {
    if (Test-Path $path) {
        Write-Host "Cleaning $path..." -ForegroundColor Gray
        # Use cmd /c rmdir for robust removal of deep paths/symlinks
        $winPath = $path -replace '/', '\'
        cmd /c "rmdir /s /q `"$winPath`"" | Out-Null
    }
    if (Test-Path $path) {
        # Fallback if cmd failed (e.g. file locked)
        Remove-Item -Recurse -Force $path -ErrorAction Continue
    }
}

# --- Main Script ---

Write-Host "`n=== Building Windows Installer for $AppName ===`n" -ForegroundColor Cyan

# 1. Check Prerequisites
Write-Host "Checking prerequisites..." -ForegroundColor Yellow

if (-not (Check-Command "java")) { Write-Error "Java not found in PATH."; exit 1 }
if (-not (Check-Command "jpackage")) { Write-Error "jpackage not found. Please use JDK 14+."; exit 1 }
if (-not (Check-Command "mvn") -and -not (Test-Path "mvnw.cmd")) { Write-Error "Maven not found."; exit 1 }

# Check for WiX 3.x (Required for MSI/EXE)
# jpackage specifically looks for light.exe and candle.exe
if (-not (Check-Command "light.exe") -or -not (Check-Command "candle.exe")) {
    Write-Warning "WiX Toolset 3.x (light.exe/candle.exe) not found in PATH."
    Write-Warning "jpackage requires WiX 3.x to build MSI/EXE installers."
    Write-Warning "WiX 4/5 (wix.exe) is NOT compatible with jpackage out of the box."
    Write-Warning "Please install WiX 3.11 from: https://github.com/wixtoolset/wix3/releases"
    Write-Warning "Add the WiX bin directory to your PATH environment variable."
    
    Write-Warning "Attempting to proceed anyway (this will likely fail)..."
    Start-Sleep -Seconds 3
} else {
    Write-Host "Found WiX Toolset 3.x." -ForegroundColor Green
}

# 2. Build with Maven (Clean first)
Write-Host "`nBuilding JAR with Maven..." -ForegroundColor Yellow
$MvnCmd = if (Test-Path "mvnw.cmd") { ".\mvnw.cmd" } else { "mvn" }
& $MvnCmd clean package -DskipTests

if ($LASTEXITCODE -ne 0) { Write-Error "Maven build failed."; exit 1 }

# 3. Prepare Directories (After Maven build, as 'clean' removes target)
Write-Host "`nPreparing directories..." -ForegroundColor Yellow
Clean-Directory $InstallerInput
Clean-Directory $InstallerOutput

New-Item -ItemType Directory -Path $InstallerInput | Out-Null
New-Item -ItemType Directory -Path $InstallerOutput | Out-Null

$JarFile = Get-ChildItem "target/extension-updater-*.jar" | Select-Object -First 1
if (-not $JarFile) { Write-Error "JAR file not found."; exit 1 }

Write-Host "Copying $($JarFile.Name) to input directory..."
Copy-Item $JarFile.FullName -Destination $InstallerInput

# 4. Create Runtime Image (jlink)
# This creates a smaller, bundled JRE
if (-not (Test-Path $RuntimeImage)) {
    Write-Host "`nCreating custom runtime image (jlink)..." -ForegroundColor Yellow
    
    # Detect modules (simplified approach - add basic modules)
    # For a real app, use jdeps to find exact modules: jdeps --print-module-deps ...
    $Modules = "java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.scripting,java.xml"
    
    $JlinkArgs = @(
        "--strip-debug",
        "--no-man-pages",
        "--no-header-files",
        "--compress", "2",
        "--add-modules", $Modules,
        "--output", $RuntimeImage
    )
    
    Write-Host "Running jlink..."
    & jlink $JlinkArgs
    if ($LASTEXITCODE -ne 0) { Write-Error "jlink failed."; exit 1 }
} else {
    Write-Host "`nUsing existing runtime image at $RuntimeImage" -ForegroundColor Gray
}

$WixTempDir = Join-Path $TargetDir "wix-temp"
Clean-Directory $WixTempDir

# 5. Run jpackage
Write-Host "`nRunning jpackage..." -ForegroundColor Yellow

$JPackageArgs = @(
    "--type", "msi",
    "--dest", $InstallerOutput,
    "--input", $InstallerInput,
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", $Vendor,
    "--copyright", $Copyright,
    "--description", $Description,
    "--main-jar", $JarFile.Name,
    "--main-class", $MainClass,
    "--runtime-image", $RuntimeImage,
    "--win-dir-chooser",
    "--win-menu",
    "--win-menu-group", "VSCode Tools",
    "--win-shortcut",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--temp", $WixTempDir,
    "--verbose"
)

# Add icon if it exists
$IconPath = "src/main/windows/icons/vsc-updater.ico"
if (Test-Path $IconPath) {
    $JPackageArgs += "--icon"
    $JPackageArgs += $IconPath
}

Write-Host "Command: jpackage $JPackageArgs" -ForegroundColor Gray

# Capture output to parse for WiX command if failure occurs
$JPackageOutput = & jpackage $JPackageArgs 2>&1 | Tee-Object -FilePath "jpackage.log"
$JPackageExitCode = $LASTEXITCODE

if ($JPackageExitCode -ne 0) { 
    Write-Warning "jpackage failed (Exit code $JPackageExitCode)."
    
    # Check if we can recover by building manually with WiX
    $MainWxs = Join-Path $WixTempDir "config/main.wxs"
    if (Test-Path $MainWxs) {
        Write-Host "`nAttempting manual WiX build from generated sources..." -ForegroundColor Yellow
        
        # Find the wix command in the log
        $LogContent = Get-Content "jpackage.log" -Raw
        # Regex to find the wix.exe build command. It usually starts after "Command [PID: ...]:" and is indented.
        # We look for "wix.exe build" and capture until the end of the line or next log entry.
        if ($LogContent -match '(?ms)Command \[PID: \d+\]:\s+(wix\.exe build .*?)(?=\r?\n\[|\Z)') {
            $WixCommand = $Matches[1] -replace "[\r\n]+", " " -replace "\s+", " "
            
            Write-Host "Found WiX command. Executing manually..." -ForegroundColor Cyan
            # Write-Host $WixCommand -ForegroundColor DarkGray
            
            # Execute the command
            # We need to invoke it using cmd /c or Invoke-Expression, but Invoke-Expression is dangerous with quotes.
            # Better to start process.
            # But the command string has arguments.
            
            # Let's try Invoke-Expression since we trust the source (jpackage output)
            Invoke-Expression $WixCommand
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "Manual WiX build succeeded!" -ForegroundColor Green
                
                # Find the generated MSI
                $GeneratedMsi = Get-ChildItem -Path "$WixTempDir/wixobj" -Filter "*.msi" | Select-Object -First 1
                if ($GeneratedMsi) {
                    $DestMsi = Join-Path $InstallerOutput "$AppName-$AppVersion.msi"
                    Move-Item $GeneratedMsi.FullName $DestMsi -Force
                    Write-Host "Moved MSI to: $DestMsi" -ForegroundColor Green
                    
                    Write-Host "`n=== SUCCESS (Manual Recovery) ===" -ForegroundColor Green
                    Get-ChildItem $InstallerOutput
                    exit 0
                } else {
                    Write-Error "MSI file not found in wixobj after successful build."
                }
            } else {
                Write-Error "Manual WiX build failed."
            }
        } else {
            Write-Warning "Could not find wix.exe build command in jpackage output."
        }
    }
    exit 1 
}

Write-Host "`n=== SUCCESS ===" -ForegroundColor Green
Write-Host "Installer created at: $InstallerOutput" -ForegroundColor Cyan
Get-ChildItem $InstallerOutput
