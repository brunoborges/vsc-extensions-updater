package com.vscode.updater;

import com.vscode.updater.config.VSCodeConfig;
import com.vscode.updater.config.ConfigManager;
import com.vscode.updater.discovery.VSCodeDetector;
import com.vscode.updater.tray.SystemTrayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Main application entry point for VS Code Extension Updater.
 * Milestone 2: Multi-instance VS Code support with enhanced detection and management.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    public static void main(String[] args) {
        // Handle command-line arguments
        if (handleCommandLineArgs(args)) {
            return; // Exit after handling command-line args
        }
        
        logger.info("Starting VS Code Extension Updater v1.0 (Milestone 2)");
        logger.info("Java version: {}", System.getProperty("java.version"));
        logger.info("Operating System: {}", System.getProperty("os.name"));
        
        // Check for headless environment (unless specifically allowed for testing)
        boolean allowHeadless = Arrays.asList(args).contains("--allow-headless");
        if (GraphicsEnvironment.isHeadless() && !allowHeadless) {
            System.err.println("Error: This application requires a graphical environment.");
            System.exit(1);
        }
        
        // Check system tray support (unless in test mode)
        boolean testMode = Arrays.asList(args).contains("--test-startup");
        if (!SystemTray.isSupported() && !testMode) {
            System.err.println("Error: System tray is not supported on this platform.");
            System.exit(1);
        }
        
        // Set system properties for better GUI experience
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        try {
            // Initialize configuration with VS Code detection
            ConfigManager configManager = new ConfigManager();
            VSCodeConfig config = configManager.loadConfig();
            logger.info("Configuration loaded from: {}", configManager.getConfigFile());
            
            // Validate configuration
            String validationError = config.validate();
            if (validationError != null) {
                logger.error("Invalid configuration: {}", validationError);
                if (!testMode) {
                    showErrorDialog("Configuration Error", 
                        "Invalid configuration: " + validationError + "\n\nUsing default settings with detection.");
                }
                config = VSCodeConfig.createDefault();
                config = configManager.refreshDetection(config); // Re-detect
            }
            
            // Log configuration summary
            logger.info("Configuration: {} instances detected, timeout={}s, notifications={}", 
                config.vsCodeInstances().size(), 
                config.commandTimeoutSeconds(), 
                config.showNotifications());
            
            // Log detected VS Code instances
            config.vsCodeInstances().forEach(instance -> 
                logger.info("VS Code instance: {} {} at {}", 
                    instance.edition().getDisplayName(),
                    instance.version(),
                    instance.getShortPath())
            );
            
            // Handle test startup mode
            if (testMode) {
                logger.info("Test startup mode - exiting after initialization");
                System.exit(0);
            }
            
            // Initialize system tray with multi-instance support
            SystemTrayManager trayManager = new SystemTrayManager(config, configManager);
            logger.info("System tray initialized with {} VS Code instance(s)", 
                config.vsCodeInstances().size());
            
            // Show startup notification if enabled
            if (config.showNotifications()) {
                final VSCodeConfig finalConfig = config; // Make effectively final for lambda
                SwingUtilities.invokeLater(() -> {
                    // Small delay to ensure tray icon is visible
                    Timer timer = new Timer(1000, e -> {
                        showStartupNotification(finalConfig);
                    });
                    timer.setRepeats(false);
                    timer.start();
                });
            }
            
            // Application is now running in system tray
            logger.info("VS Code Extension Updater (Milestone 2) started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            if (!testMode) {
                showErrorDialog("Startup Error", 
                    "Failed to start VS Code Extension Updater:\n" + e.getMessage());
            }
            System.exit(1);
        }
    }
    
    /**
     * Handle command-line arguments for testing and utility functions
     * @param args Command line arguments
     * @return true if application should exit after handling args
     */
    private static boolean handleCommandLineArgs(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case "--version":
                    System.out.println("VS Code Extension Updater v1.0");
                    System.out.println("Java: " + System.getProperty("java.version"));
                    System.out.println("OS: " + System.getProperty("os.name"));
                    return true;
                    
                case "--test-detection":
                    testVSCodeDetection();
                    return true;
                    
                case "--help":
                    printHelp();
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Test VS Code detection functionality
     */
    private static void testVSCodeDetection() {
        System.out.println("Testing VS Code detection...");
        try {
            var instances = VSCodeDetector.detectInstallations();
            
            System.out.println("Found " + instances.size() + " VS Code instance(s):");
            instances.forEach(instance -> {
                System.out.println("  - " + instance.edition().getDisplayName() + 
                                 " " + instance.version() + 
                                 " at " + instance.executablePath());
            });
            
            if (instances.isEmpty()) {
                System.out.println("No VS Code installations detected.");
                System.out.println("Make sure VS Code is installed in standard locations:");
                System.out.println("  - /Applications/Visual Studio Code.app (macOS)");
                System.out.println("  - /usr/bin/code (Linux)");
                System.out.println("  - %LOCALAPPDATA%\\Programs\\Microsoft VS Code (Windows)");
            }
        } catch (Exception e) {
            System.err.println("Error during VS Code detection: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Print help information
     */
    private static void printHelp() {
        System.out.println("VS Code Extension Updater v1.0");
        System.out.println();
        System.out.println("Usage: java -jar extension-updater.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --version           Show version information");
        System.out.println("  --test-detection    Test VS Code detection");
        System.out.println("  --test-startup      Test application startup (for CI)");
        System.out.println("  --allow-headless    Allow running in headless environment");
        System.out.println("  --system-tray       Start in system tray mode (default)");
        System.out.println("  --help              Show this help message");
        System.out.println();
        System.out.println("When run without options, the application starts in system tray mode.");
    }
    
    private static void showStartupNotification(VSCodeConfig config) {
        try {
            SystemTray systemTray = SystemTray.getSystemTray();
            if (systemTray.getTrayIcons().length > 0) {
                TrayIcon trayIcon = systemTray.getTrayIcons()[0];
                
                int instanceCount = config.vsCodeInstances().size();
                int enabledCount = config.getEnabledInstances().size();
                
                String message = String.format(
                    "Found %d VS Code installation%s (%d enabled). Right-click for options.",
                    instanceCount,
                    instanceCount == 1 ? "" : "s",
                    enabledCount
                );
                
                trayIcon.displayMessage(
                    "VS Code Extension Updater", 
                    message,
                    TrayIcon.MessageType.INFO
                );
            }
        } catch (Exception e) {
            logger.debug("Failed to show startup notification: {}", e.getMessage());
        }
    }
    
    private static void showErrorDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                null, 
                message, 
                title, 
                JOptionPane.ERROR_MESSAGE
            );
        });
    }
}