# Windows Icons

This directory contains icon resources for the Windows installer.

## Current Icons

- **vsc-updater.ico** - Main application icon (converted from PNG logo)
  - Multi-resolution ICO file with sizes: 16x16, 32x32, 48x48, 64x64, 128x128, 256x256
  - Used by jpackage for installer package, desktop shortcuts, and system tray
  - Source: `src/main/resources/vsc-updater-logo.png`

## Icon Usage

The `vsc-updater.ico` file is automatically used by:
- Windows installer (MSI package)
- Desktop shortcut
- Start Menu entry  
- System tray icon
- Application title bar
- Windows taskbar
- Add/Remove Programs

## Regenerating Icons

If you need to update the icon from the source PNG:

```bash
# Install ImageMagick (if not already installed)
brew install imagemagick

# Convert PNG to ICO with multiple resolutions
convert src/main/resources/vsc-updater-logo.png \
  \( -clone 0 -resize 16x16 \) \
  \( -clone 0 -resize 32x32 \) \
  \( -clone 0 -resize 48x48 \) \
  \( -clone 0 -resize 64x64 \) \
  \( -clone 0 -resize 128x128 \) \
  \( -clone 0 -resize 256x256 \) \
  -delete 0 src/main/windows/icons/vsc-updater.ico
```

## Icon Requirements

Windows ICO files should include multiple sizes for optimal display across different UI contexts:
- 16x16 - Small icons (details view)
- 32x32 - Medium icons (list view) 
- 48x48 - Large icons (icon view)
- 64x64 - Extra large icons
- 128x128 - Jumbo icons
- 256x256 - High DPI displays

## Status
✅ **Production Ready** - Professional application icon integrated