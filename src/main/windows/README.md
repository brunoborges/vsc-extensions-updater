# Windows Installer Resources

This directory contains Windows-specific resources for the installer package.

## Directory Structure

### icons/
- `app.ico` - Main application icon (multiple sizes: 16x16, 32x32, 48x48, 256x256)

### resources/
- `license.rtf` - Software license in RTF format for installer display
- `banner.bmp` - Installer banner image (493x58 pixels)
- `dialog.bmp` - Installer dialog background (493x312 pixels)

### installer-scripts/
- `post-install.bat` - Script executed after installation
- `pre-uninstall.bat` - Script executed before uninstallation

## Creating Icons

To create the Windows icon file:
1. Use an icon editor like IcoFX or GIMP
2. Include multiple resolutions: 16x16, 32x32, 48x48, 256x256
3. Save as `app.ico` in the icons directory

## Creating Installer Images

For professional installer appearance:
- **banner.bmp**: 493x58 pixels, 8-bit color depth
- **dialog.bmp**: 493x312 pixels, 8-bit color depth

## Notes

These resources are optional but recommended for a professional installation experience.