# Windows Installer Build Success

## Overview
We have successfully built the Windows Installer (MSI) for the VS Code Extension Updater using `jpackage` and WiX Toolset 6.

## Prerequisites
- **JDK 21**: Provides `jpackage`.
- **WiX Toolset 6**: Installed via `.NET Tool` (`wix`).
- **WiX Extensions**: `WixToolset.UI.wixext` and `WixToolset.Util.wixext` installed via `wix extension add`.

## Build Process
The `build-windows-installer.ps1` script performs the following steps:
1. **Checks Prerequisites**: Verifies Java, Maven, and WiX are available.
2. **Builds JAR**: Uses Maven to build the application JAR.
3. **Creates Runtime Image**: Uses `jlink` to create a custom, minimized JRE.
4. **Runs jpackage**:
   - Generates WiX source files in `target/wix-temp`.
   - Uses `wix.exe` to build the MSI.
   - Includes a fallback mechanism to manually run `wix build` if `jpackage` fails to invoke it correctly (which was an issue during development).

## Output
The installer is located at:
`target/installer-output/VSCodeExtensionUpdater-1.0.0.msi`

## Troubleshooting
If `jpackage` fails with `error WIX0144: The extension 'WixToolset.Util.wixext' could not be found`, the script attempts to run the WiX build command manually, which has been proven to work.

## Verification
The MSI file is approximately 37MB and contains the application and the custom runtime.
