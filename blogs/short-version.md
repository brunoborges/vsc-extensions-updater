# Auto-Update Your VS Code Extensions with This Lightweight Tool

![VS Code Extension Updater](screenshot1.png)

Tired of manually updating VS Code extensions across multiple instances? I built **VS Code Extension Updater** – a lightweight Java app that runs in your system tray and automatically keeps all your extensions up-to-date.

## ✨ Key Features

🎯 **Multi-Instance Support**: Auto-detects VS Code Stable, Insiders, and portable installations  
🚀 **Flexible Updates**: Schedule automatic updates or run on-demand  
🖥️ **Cross-Platform**: Native installers for Windows (MSI), macOS (PKG), and Linux  
📦 **Zero Dependencies**: Includes embedded Java runtime – no Java installation needed  
🛡️ **Professional Quality**: Code signing, proper installers, custom branding  

## 🚀 Quick Start

1. **Download**: Get the installer for your platform from [GitHub Releases](https://github.com/brunoborges/vsc-extensions-updater/releases)
2. **Install**: Double-click the installer (MSI/PKG) for one-click setup
3. **Configure**: Right-click the system tray icon to set your preferences
4. **Relax**: Extensions stay updated automatically in the background

![System Tray Menu](screenshot2.png)

## ⚙️ Perfect For

- **Multiple VS Code Instances**: Keep Stable and Insiders in sync
- **Team Consistency**: Ensure everyone has the same extension versions  
- **Morning Routine**: Auto-update and launch VS Code to start your day
- **Background Maintenance**: Set-and-forget scheduled updates

## 🔧 Technical Highlights

- **Lightweight**: Only 16-64MB memory usage
- **Custom JRE**: Optimized Java runtime (55% smaller than standard)
- **Professional Distribution**: Signed installers with proper OS integration
- **Enterprise-Grade**: Robust logging, error handling, configuration management

Built with Java 21, Maven, and modern deployment tools (jlink + jpackage).

## 📦 Installation Options

**Windows**: MSI installer with Start Menu integration  
**macOS**: PKG installer with Applications folder integration  
**Linux**: Portable JAR with desktop integration scripts  

All platforms include:
- System tray integration
- Custom application icons  
- Professional uninstall support
- Auto-start on system boot

## 🛠️ Configuration Example

```json
{
  "updateFrequency": "EVERY_4_HOURS",
  "enableNotifications": true,
  "workingHoursOnly": true,
  "vscodeInstances": {
    "VS Code Stable": { "enabled": true },
    "VS Code Insiders": { "enabled": true }
  }
}
```

## 🚀 What's Next

- **Extension Profiles**: Save/restore extension sets for different projects
- **Team Sync**: Share configurations across team members
- **VS Code Settings Sync**: Keep settings synchronized too
- **Rollback Support**: Undo problematic updates

---

**Never worry about outdated extensions again!**

⭐ [**Get Started**](https://github.com/brunoborges/vsc-extensions-updater) | 🔧 [**View Source**](https://github.com/brunoborges/vsc-extensions-updater) | 📚 [**Documentation**](https://github.com/brunoborges/vsc-extensions-updater#readme)

Open source, free forever. Built by developers, for developers.

#VSCode #Extensions #Automation #Java #OpenSource #DeveloperTools