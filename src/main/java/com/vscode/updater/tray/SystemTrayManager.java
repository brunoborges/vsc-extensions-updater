package com.vscode.updater.tray;

import com.vscode.updater.config.VSCodeConfig;
import com.vscode.updater.config.ConfigManager;
import com.vscode.updater.discovery.VSCodeInstance;
import com.vscode.updater.executor.CommandExecutor;
import com.vscode.updater.gui.AboutDialog;
import com.vscode.updater.gui.LogViewerWindow;
import com.vscode.updater.gui.SettingsWindow;
import com.vscode.updater.logging.LogManager;
import com.vscode.updater.scheduler.UpdateScheduler;
import com.vscode.updater.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Enhanced system tray manager with multi-instance VS Code support.
 * Milestone 2 implementation with dynamic menu building and session logging.
 */
public class SystemTrayManager implements MultiInstanceMenuBuilder.MenuActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(SystemTrayManager.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
    
    private final ConfigManager configManager;
    private final LogViewerWindow logViewer;
    private final LogManager logManager;
    private final AtomicInteger runningUpdates;
    private final UpdateScheduler scheduler;
    private final SettingsWindow settingsWindow;
    
    private VSCodeConfig config;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    
    // Status tracking
    private LocalDateTime lastUpdateTime;
    private String lastUpdateSummary = "";
    private String schedulerStatus = "Disabled";
    
    public SystemTrayManager(VSCodeConfig initialConfig, ConfigManager configManager) {
        this.config = initialConfig;
        this.configManager = configManager;
        this.logViewer = new LogViewerWindow();
        this.logManager = new LogManager();
        this.runningUpdates = new AtomicInteger(0);
        
        // Initialize scheduler with callbacks
        this.scheduler = new UpdateScheduler(
            this::handleScheduledUpdate,
            this::updateSchedulerStatus
        );
        
        // Initialize settings window
        this.settingsWindow = new SettingsWindow(config, this::handleConfigUpdate);
        
        if (!SystemTray.isSupported()) {
            throw new UnsupportedOperationException("System tray is not supported on this platform");
        }
        
        setupLogManager();
        initializeSystemTray();
        updateTrayMenu();
        
        // Start scheduler with current config
        scheduler.start(config);
    }
    
    private void setupLogManager() {
        // Connect log manager to log viewer
        logManager.addGlobalConsumer(entry -> {
            logViewer.appendLog(entry.getFormattedMessage());
        });
        
        // Add initial welcome message
        LogManager.LogSession welcomeSession = logManager.createSession(
            new VSCodeInstance("", VSCodeInstance.VSCodeEdition.STABLE, "", "System", true, "", ""));
        welcomeSession.addEntry(LogManager.LogLevel.INFO, 
            "VS Code Extension Updater started with " + config.vsCodeInstances().size() + " detected instance(s)", "");
    }
    
    private void initializeSystemTray() {
        try {
            systemTray = SystemTray.getSystemTray();
            
            // Create tray icon
            Image trayImage = createTrayIcon();
            trayIcon = new TrayIcon(trayImage, "VS Code Extension Updater", null);
            trayIcon.setImageAutoSize(true);
            updateTrayTooltip();
            
            // Add double-click handler to show logs
            trayIcon.addActionListener(e -> {
                logger.info("Tray icon double-clicked");
                showLogViewer();
            });
            
            systemTray.add(trayIcon);
            logger.info("System tray initialized successfully");
            
        } catch (AWTException e) {
            logger.error("Failed to initialize system tray", e);
            throw new RuntimeException("System tray initialization failed", e);
        }
    }
    
    private void updateTrayMenu() {
        UpdateScheduler.SchedulerStatus schedulerStatus = scheduler.getStatus();
        
        PopupMenu menu = MultiInstanceMenuBuilder.buildMenu(
            config.vsCodeInstances(),
            lastUpdateSummary,
            schedulerStatus.getStatusSummary(),
            schedulerStatus.isRunning(),
            this
        );
        trayIcon.setPopupMenu(menu);
        updateTrayTooltip();
    }
    
    private void updateTrayTooltip() {
        int totalInstances = config.vsCodeInstances().size();
        int enabledInstances = config.getEnabledInstances().size();
        int runningCount = runningUpdates.get();
        
        String tooltip = String.format("VS Code Extension Updater - %d/%d instances enabled", 
            enabledInstances, totalInstances);
        
        if (runningCount > 0) {
            tooltip += String.format(" (%d updating)", runningCount);
        }
        
        trayIcon.setToolTip(tooltip);
    }
    
    // Implementation of MenuActionHandler interface
    
    @Override
    public void onUpdateInstance(VSCodeInstance instance) {
        if (runningUpdates.get() >= config.concurrency().maxConcurrentUpdates()) {
            showError("Update Limit Reached", 
                String.format("Maximum %d concurrent updates allowed. Please wait.", 
                    config.concurrency().maxConcurrentUpdates()));
            return;
        }
        
        runSingleInstanceUpdate(instance);
    }
    
    @Override
    public void onUpdateAndOpenInstance(VSCodeInstance instance) {
        if (runningUpdates.get() >= config.concurrency().maxConcurrentUpdates()) {
            showError("Update Limit Reached", 
                String.format("Maximum %d concurrent updates allowed. Please wait.", 
                    config.concurrency().maxConcurrentUpdates()));
            return;
        }
        
        runUpdateAndOpenInstance(instance);
    }
    
    @Override
    public void onToggleInstance(VSCodeInstance instance) {
        logger.info("Toggling instance: {} -> {}", instance.displayName(), !instance.enabled());
        
        // Update instance in config
        List<VSCodeInstance> updatedInstances = config.vsCodeInstances().stream()
            .map(i -> i.executablePath().equals(instance.executablePath()) ? 
                i.withEnabled(!i.enabled()) : i)
            .toList();
        
        config = config.withUpdatedInstances(updatedInstances);
        configManager.saveConfig(config);
        
        updateTrayMenu();
        showNotification("Instance " + (instance.enabled() ? "Disabled" : "Enabled"), 
            instance.displayName() + " has been " + (instance.enabled() ? "disabled" : "enabled"));
    }
    
    @Override
    public void onRefreshDetection() {
        logger.info("Refreshing VS Code detection");
        showNotification("Refreshing...", "Re-detecting VS Code installations");
        
        SwingUtilities.invokeLater(() -> {
            config = configManager.refreshDetection(config);
            updateTrayMenu();
            
            int detected = config.vsCodeInstances().size();
            showNotification("Detection Complete", 
                String.format("Found %d VS Code installation%s", detected, detected == 1 ? "" : "s"));
        });
    }
    
    @Override
    public void onViewLogs() {
        showLogViewer();
    }
    
    @Override
    public void onShowAbout() {
        showAbout();
    }
    
    @Override
    public void onExit() {
        exitApplication();
    }
    
    // New methods for Milestone 3 scheduling
    
    public void onShowSettings() {
        showSettings();
    }
    
    public void onToggleScheduler() {
        toggleScheduler();
    }
    
    public void onRunScheduledUpdate() {
        scheduler.triggerImmediateUpdate();
    }
    
    private void runSingleInstanceUpdate(VSCodeInstance instance) {
        runningUpdates.incrementAndGet();
        updateTrayMenu();
        
        LogManager.LogSession session = logManager.createSession(instance);
        session.addEntry(LogManager.LogLevel.INFO, 
            "Starting extension update for " + instance.displayName(), "");
        
        logger.info("Starting extension update for: {}", instance.displayName());
        
        // Create output consumer for this specific session
        Consumer<String> outputConsumer = line -> {
            session.addEntry(LogManager.LogLevel.INFO, line, line);
        };
        
        // Run command asynchronously
        CommandExecutor.updateExtensionsAsync(instance.executablePath(), 
            config.commandTimeoutSeconds(), outputConsumer)
        .whenComplete((result, throwable) -> {
            SwingUtilities.invokeLater(() -> {
                handleUpdateCompletion(instance, session, result, throwable);
            });
        });
    }
    
    private void runUpdateAndOpenInstance(VSCodeInstance instance) {
        runningUpdates.incrementAndGet();
        updateTrayMenu();
        
        LogManager.LogSession session = logManager.createSession(instance);
        session.addEntry(LogManager.LogLevel.INFO, 
            "Starting extension update and VS Code launch for " + instance.displayName(), "");
        
        logger.info("Starting extension update and VS Code launch for: {}", instance.displayName());
        
        // Create output consumer for this specific session
        Consumer<String> outputConsumer = line -> {
            session.addEntry(LogManager.LogLevel.INFO, line, line);
        };
        
        // Run update and open command asynchronously
        CommandExecutor.updateAndLaunchAsync(instance.executablePath(), 
            config.commandTimeoutSeconds(), outputConsumer)
        .whenComplete((result, throwable) -> {
            SwingUtilities.invokeLater(() -> {
                handleUpdateAndOpenCompletion(instance, session, result, throwable);
            });
        });
    }
    
    private void handleUpdateCompletion(VSCodeInstance instance, LogManager.LogSession session,
                                      CommandExecutor.ExecutionResult result, Throwable throwable) {
        runningUpdates.decrementAndGet();
        lastUpdateTime = LocalDateTime.now();
        
        String status;
        boolean success;
        
        if (throwable != null) {
            logger.error("Update failed for {}: {}", instance.displayName(), throwable.getMessage());
            status = "Failed: " + throwable.getMessage();
            success = false;
            session.addEntry(LogManager.LogLevel.ERROR, "Update failed: " + throwable.getMessage(), "");
        } else if (result.success()) {
            logger.info("Update completed successfully for {}: {}", instance.displayName(), result.summary());
            status = "Success";
            success = true;
            session.addEntry(LogManager.LogLevel.INFO, "Update completed successfully", "");
        } else {
            logger.warn("Update failed for {}: {}", instance.displayName(), result.summary());
            status = "Failed";
            success = false;
            session.addEntry(LogManager.LogLevel.ERROR, "Update failed: " + result.summary(), "");
        }
        
        // Update instance status in config
        updateInstanceStatus(instance, lastUpdateTime.format(TIME_FORMAT), status);
        
        // Update summary
        lastUpdateSummary = String.format("Last: %s - %s", 
            instance.displayName(), status);
        
        updateTrayMenu();
        
        if (success && config.showNotifications()) {
            showNotification("Update Completed", 
                instance.displayName() + " extensions updated successfully");
        } else if (!success) {
            showError("Update Failed", 
                instance.displayName() + " update failed: " + status);
        }
        
        session.addEntry(LogManager.LogLevel.INFO, "=== Update session completed ===", "");
    }
    
    private void handleUpdateAndOpenCompletion(VSCodeInstance instance, LogManager.LogSession session,
                                             CommandExecutor.UpdateAndLaunchResult result, Throwable throwable) {
        runningUpdates.decrementAndGet();
        lastUpdateTime = LocalDateTime.now();
        
        String status;
        boolean success;
        
        if (throwable != null) {
            logger.error("Update and launch failed for {}: {}", instance.displayName(), throwable.getMessage());
            status = "Failed: " + throwable.getMessage();
            success = false;
            session.addEntry(LogManager.LogLevel.ERROR, "Update and launch failed: " + throwable.getMessage(), "");
        } else if (result.success()) {
            logger.info("Update and launch completed successfully for {}: {}", instance.displayName(), result.summary());
            status = "Success + Opened";
            success = true;
            session.addEntry(LogManager.LogLevel.INFO, "Update and launch completed successfully", "");
        } else if (result.updateResult().success()) {
            logger.warn("Update succeeded but launch failed for {}: {}", instance.displayName(), result.summary());
            status = "Updated (Launch Failed)";
            success = false;
            session.addEntry(LogManager.LogLevel.WARN, "Update succeeded but VS Code launch failed", "");
        } else {
            logger.warn("Update and launch failed for {}: {}", instance.displayName(), result.summary());
            status = "Failed";
            success = false;
            session.addEntry(LogManager.LogLevel.ERROR, "Update failed: " + result.updateResult().summary(), "");
        }
        
        // Update instance status in config
        updateInstanceStatus(instance, lastUpdateTime.format(TIME_FORMAT), status);
        
        // Update summary
        lastUpdateSummary = String.format("Last: %s - %s", 
            instance.displayName(), status);
        
        updateTrayMenu();
        
        if (result != null && result.success() && config.showNotifications()) {
            showNotification("Update and Launch Completed", 
                instance.displayName() + " extensions updated and VS Code launched successfully");
        } else if (result != null && result.updateResult().success() && !result.launchSuccess() && config.showNotifications()) {
            showNotification("Update Completed", 
                instance.displayName() + " extensions updated successfully, but failed to launch VS Code");
        } else if (!success) {
            showError("Update and Launch Failed", 
                instance.displayName() + " update and launch failed: " + status);
        }
        
        session.addEntry(LogManager.LogLevel.INFO, "=== Update and launch session completed ===", "");
    }
    
    private void updateInstanceStatus(VSCodeInstance instance, String updateTime, String status) {
        List<VSCodeInstance> updatedInstances = config.vsCodeInstances().stream()
            .map(i -> i.executablePath().equals(instance.executablePath()) ? 
                i.withUpdateStatus(updateTime, status) : i)
            .toList();
        
        config = config.withUpdatedInstances(updatedInstances);
        configManager.saveConfig(config);
    }
    
    private Image createTrayIcon() {
        try {
            // Load the VSCode Extension Updater logo from resources
            InputStream imageStream = getClass().getResourceAsStream("/vsc-updater-logo.png");
            if (imageStream == null) {
                logger.warn("Could not find vsc-updater-logo.png in resources, falling back to programmatic icon");
                return createFallbackTrayIcon();
            }
            
            BufferedImage originalImage = ImageIO.read(imageStream);
            imageStream.close();
            
            // Scale to appropriate tray icon size (typically 16x16)
            int size = 16;
            BufferedImage scaledImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaledImage.createGraphics();
            
            // Enable high-quality rendering
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Draw the scaled VS Code logo
            g.drawImage(originalImage, 0, 0, size, size, null);
            
            // Add status indicator overlay if needed
            addStatusOverlay(g, size);
            
            g.dispose();
            return scaledImage;
            
        } catch (IOException e) {
            logger.error("Failed to load VS Code logo image", e);
            return createFallbackTrayIcon();
        }
    }
    
    private void addStatusOverlay(Graphics2D g, int size) {
        // Add a small status indicator in the bottom-right corner
        int overlaySize = 6;
        int overlayX = size - overlaySize - 1;
        int overlayY = size - overlaySize - 1;
        
        // Choose color based on instance status
        Color statusColor;
        if (runningUpdates.get() > 0) {
            statusColor = Color.ORANGE; // Updates in progress
        } else if (config.getEnabledInstances().isEmpty()) {
            statusColor = Color.GRAY; // No enabled instances
        } else {
            statusColor = Color.GREEN; // Ready
        }
        
        // Draw status indicator with a subtle border
        g.setColor(Color.WHITE);
        g.fillOval(overlayX - 1, overlayY - 1, overlaySize + 2, overlaySize + 2);
        g.setColor(statusColor);
        g.fillOval(overlayX, overlayY, overlaySize, overlaySize);
        
        // Add instance count if multiple instances
        if (config.vsCodeInstances().size() > 1) {
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 6));
            String count = String.valueOf(config.vsCodeInstances().size());
            FontMetrics fm = g.getFontMetrics();
            int x = overlayX + (overlaySize - fm.stringWidth(count)) / 2;
            int y = overlayY + (overlaySize + fm.getAscent()) / 2 - 1;
            g.drawString(count, x, y);
        }
    }
    
    private Image createFallbackTrayIcon() {
        // Fallback to the original programmatic icon if image loading fails
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        
        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Choose color based on instance status
        Color iconColor;
        if (runningUpdates.get() > 0) {
            iconColor = Color.ORANGE; // Updates in progress
        } else if (config.getEnabledInstances().isEmpty()) {
            iconColor = Color.GRAY; // No enabled instances
        } else {
            iconColor = Color.GREEN; // Ready
        }
        
        g.setColor(iconColor);
        g.fillOval(2, 2, size - 4, size - 4);
        
        // Draw border
        g.setColor(Color.DARK_GRAY);
        g.drawOval(1, 1, size - 3, size - 3);
        
        // Add instance count indicator if multiple instances
        if (config.vsCodeInstances().size() > 1) {
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
            String count = String.valueOf(config.vsCodeInstances().size());
            FontMetrics fm = g.getFontMetrics();
            int x = (size - fm.stringWidth(count)) / 2;
            int y = (size + fm.getAscent()) / 2 - 1;
            g.drawString(count, x, y);
        }
        
        g.dispose();
        return image;
    }
    
    private void showLogViewer() {
        logger.info("Showing log viewer window");
        SwingUtilities.invokeLater(() -> {
            try {
                logViewer.showWindow();
                logger.debug("Log viewer window should now be visible");
            } catch (Exception e) {
                logger.error("Failed to show log viewer", e);
                showError("Error", "Failed to open log viewer: " + e.getMessage());
            }
        });
    }
    
    private void showAbout() {
        logger.info("Showing About dialog");
        SwingUtilities.invokeLater(() -> {
            try {
                AboutDialog.showAbout(null);
            } catch (Exception e) {
                logger.error("Failed to show About dialog", e);
                showError("Error", "Failed to open About dialog: " + e.getMessage());
            }
        });
    }
    
    private void showError(String title, String message) {
        logger.error("Showing error dialog: {}", message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    private void showNotification(String title, String message) {
        if (config.showNotifications()) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }
    
    private void exitApplication() {
        logger.info("Application exit requested");
        
        // Shutdown scheduler first
        scheduler.shutdown();
        
        // Hide windows
        logViewer.setVisible(false);
        settingsWindow.setVisible(false);
        
        // Remove tray icon
        systemTray.remove(trayIcon);
        
        // Exit application
        System.exit(0);
    }
    
    /**
     * Gets the current configuration.
     */
    public VSCodeConfig getConfig() {
        return config;
    }
    
    /**
     * Updates the configuration and refreshes the tray menu.
     */
    public void updateConfig(VSCodeConfig newConfig) {
        this.config = newConfig;
        updateTrayMenu();
        updateTrayIcon();
    }
    
    /**
     * Updates the tray icon based on current state.
     */
    public void updateTrayIcon() {
        trayIcon.setImage(createTrayIcon());
        updateTrayTooltip();
    }
    
    /**
     * Gets the log manager for external access.
     */
    public LogManager getLogManager() {
        return logManager;
    }
    
    // Milestone 3: Scheduling integration methods
    
    private void handleScheduledUpdate(VSCodeInstance instance) {
        // This is called by the scheduler to perform updates
        runSingleInstanceUpdate(instance);
    }
    
    private void updateSchedulerStatus(String status) {
        this.schedulerStatus = status;
        updateTrayMenu(); // Refresh menu to show new status
    }
    
    private void handleConfigUpdate(VSCodeConfig newConfig) {
        try {
            // Save configuration first
            configManager.saveConfig(newConfig);
            
            // Update current config only after successful save
            this.config = newConfig;
            
            // Restart scheduler with new configuration
            scheduler.start(newConfig);
            
            // Update UI elements
            updateTrayMenu();
            updateTrayIcon();
            settingsWindow.updateConfig(newConfig);
            
            logger.info("Configuration updated and applied successfully");
            
        } catch (RuntimeException e) {
            logger.error("Failed to save configuration: {}", e.getMessage());
            // Don't update current config if save failed
            
            // Show error notification to user
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                    "Configuration save failed: " + e.getMessage() + 
                    "\n\nYour settings have not been saved.",
                    "Configuration Error",
                    JOptionPane.ERROR_MESSAGE);
            });
            
            // Re-throw so the settings dialog can also handle it
            throw e;
        }
    }
    
    private void showSettings() {
        logger.info("Showing settings window");
        SwingUtilities.invokeLater(() -> {
            try {
                // Ensure settings window has the latest configuration
                settingsWindow.updateConfig(config);
                settingsWindow.setVisible(true);
                settingsWindow.toFront();
                
                // Update scheduler status in settings window
                UpdateScheduler.SchedulerStatus status = scheduler.getStatus();
                settingsWindow.updateSchedulerStatus(status);
                
            } catch (Exception e) {
                logger.error("Failed to show settings window", e);
                showError("Error", "Failed to open settings: " + e.getMessage());
            }
        });
    }
    
    private void toggleScheduler() {
        UpdateScheduler.SchedulerStatus status = scheduler.getStatus();
        
        if (status.isRunning()) {
            logger.info("Stopping scheduler");
            scheduler.stop();
            showNotification("Scheduler Stopped", "Automatic updates have been stopped");
        } else {
            logger.info("Starting scheduler");
            scheduler.start(config);
            showNotification("Scheduler Started", 
                "Automatic updates scheduled: " + config.schedule().getDisplaySchedule());
        }
        
        updateTrayMenu();
    }
}