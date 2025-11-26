package com.vscode.updater;

import com.vscode.updater.config.VSCodeConfig;
import com.vscode.updater.config.ConfigManager;
import com.vscode.updater.tray.SystemTrayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.*;

/**
 * Main application entry point for VS Code Extension Updater.
 * Milestone 2: Multi-instance VS Code support with enhanced detection and management.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    public static void main(String[] args) {
        logger.info("Starting VS Code Extension Updater v1.0 (Milestone 2)");
        logger.info("Java version: {}", System.getProperty("java.version"));
        logger.info("Operating System: {}", System.getProperty("os.name"));
        
        // Check for headless environment
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Error: This application requires a graphical environment.");
            System.exit(1);
        }
        
        // Check system tray support
        if (!SystemTray.isSupported()) {
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
                showErrorDialog("Configuration Error", 
                    "Invalid configuration: " + validationError + "\n\nUsing default settings with detection.");
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
            showErrorDialog("Startup Error", 
                "Failed to start VS Code Extension Updater:\n" + e.getMessage());
            System.exit(1);
        }
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