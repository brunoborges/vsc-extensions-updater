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