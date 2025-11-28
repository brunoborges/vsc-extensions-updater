# Installer Build Scripts

This directory contains scripts to build installers for Linux, macOS, and Windows.

## Structure

- `linux/`: Contains the build script and resources for the Linux DEB installer.
- `macos/`: Contains the build script and resources for the macOS PKG installer.
- `windows/`: Contains the build script and resources for the Windows MSI installer.

## Prerequisites

- **Java 21+ JDK**: Required for `jlink` and `jpackage`.
- **Maven**: Required to build the application JAR.

### Linux
- **fakeroot**: Required by `jpackage` to build DEB packages.
  ```bash
  sudo apt-get install fakeroot
  ```

### Windows
- **WiX Toolset 3.x**: Required by `jpackage` to build MSI packages.
  - Download from [WiX Toolset Releases](https://github.com/wixtoolset/wix3/releases).
  - Ensure `candle.exe` and `light.exe` are in your PATH.

## Building

### Linux
Run the build script from the project root or the `installers/linux` directory:
```bash
bash installers/linux/build.sh
```
The installer will be created in `target/installer/`.

### macOS
Run the build script from the project root or the `installers/macos` directory:
```bash
bash installers/macos/build.sh
```
The installer will be created in `target/installer/`.

### Windows
Run the PowerShell script from the project root or the `installers/windows` directory:
```powershell
.\installers\windows\build.ps1
```
The installer will be created in `target/installer/`.
