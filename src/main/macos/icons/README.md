# macOS Icons

This directory contains icon resources for the macOS installer and application bundle.

## Current Icons

- **vsc-updater.icns** - Main application icon for macOS (Apple ICNS format)
  - Multi-resolution ICNS file with sizes: 16×16, 32×32, 128×128, 256×256, 512×512, 1024×1024
  - Includes @2x (Retina) variants for crisp display on high-DPI screens
  - Used by jpackage for app bundle, Finder icon, Dock, and Launchpad
  - Source: `src/main/resources/vsc-updater-logo.png`

## Icon Usage

The `vsc-updater.icns` file is automatically used by:
- macOS installer (PKG package)
- Application bundle in /Applications/
- Finder icon display
- Dock icon when running
- Launchpad icon
- Spotlight search results
- Application switcher (Command+Tab)

## Regenerating Icons

If you need to update the icon from the source PNG:

```bash
# Create temporary iconset directory
mkdir -p temp-iconset.iconset

# Create all required sizes for macOS (including Retina variants)
magick src/main/resources/vsc-updater-logo.png -resize 16x16     temp-iconset.iconset/icon_16x16.png
magick src/main/resources/vsc-updater-logo.png -resize 32x32     temp-iconset.iconset/icon_16x16@2x.png
magick src/main/resources/vsc-updater-logo.png -resize 32x32     temp-iconset.iconset/icon_32x32.png
magick src/main/resources/vsc-updater-logo.png -resize 64x64     temp-iconset.iconset/icon_32x32@2x.png
magick src/main/resources/vsc-updater-logo.png -resize 128x128   temp-iconset.iconset/icon_128x128.png
magick src/main/resources/vsc-updater-logo.png -resize 256x256   temp-iconset.iconset/icon_128x128@2x.png
magick src/main/resources/vsc-updater-logo.png -resize 256x256   temp-iconset.iconset/icon_256x256.png
magick src/main/resources/vsc-updater-logo.png -resize 512x512   temp-iconset.iconset/icon_256x256@2x.png
magick src/main/resources/vsc-updater-logo.png -resize 512x512   temp-iconset.iconset/icon_512x512.png
magick src/main/resources/vsc-updater-logo.png -resize 1024x1024 temp-iconset.iconset/icon_512x512@2x.png

# Convert iconset to ICNS using macOS iconutil
iconutil -c icns temp-iconset.iconset -o src/main/macos/icons/vsc-updater.icns

# Clean up
rm -rf temp-iconset.iconset
```

## macOS Icon Requirements

macOS ICNS files should include multiple sizes for optimal display across different contexts:

| Size | Usage | Retina Variant |
|------|-------|---------------|
| 16×16 | Small icons, lists | 32×32 (@2x) |
| 32×32 | Medium icons | 64×64 (@2x) |
| 128×128 | Large icons | 256×256 (@2x) |
| 256×256 | Very large icons | 512×512 (@2x) |
| 512×512 | Huge icons | 1024×1024 (@2x) |

## Important Notes

- **iconutil** is a macOS-only tool, so ICNS generation must be done on macOS
- The ICNS format includes all size variants in a single file (~2MB)
- jpackage automatically uses the ICNS for the app bundle icon
- High-quality scaling is essential for Retina display compatibility

## Status
✅ **Production Ready** - Professional macOS application icon integrated