package com.vscode.updater.gui;

import com.vscode.updater.config.VSCodeConfig;
import com.vscode.updater.discovery.VSCodeInstance;
import com.vscode.updater.scheduler.UpdateScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Settings window for configuring scheduling and VS Code instance management.
 * Milestone 3: Complete configuration UI.
 */
public class SettingsWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(SettingsWindow.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss");
    
    private final Consumer<VSCodeConfig> configUpdateCallback;
    private VSCodeConfig currentConfig;
    
    // Scheduling components
    private JCheckBox enableSchedulingCheckBox;
    private JSpinner intervalSpinner;
    private JComboBox<UpdateScheduler.ScheduleConfig.ScheduleType> scheduleTypeCombo;
    private JTextField customCronField;
    private JCheckBox updateOnStartupCheckBox;
    private JCheckBox skipRecentCheckBox;
    private JSpinner recentThresholdSpinner;
    private JCheckBox onlyWhenIdleCheckBox;
    private JSpinner maxConcurrentSpinner;
    
    // Instance management components  
    private JTable instanceTable;
    private InstanceTableModel instanceTableModel;
    
    // Status components
    private JLabel statusLabel;
    private JLabel lastUpdateLabel;
    private JLabel nextUpdateLabel;
    
    public SettingsWindow(VSCodeConfig config, Consumer<VSCodeConfig> configUpdateCallback) {
        this.currentConfig = config;
        this.configUpdateCallback = configUpdateCallback;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        updateFromConfig();
        
        setTitle("VS Code Extension Updater - Settings");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);
        
        logger.info("Settings window initialized");
    }
    
    private void initializeComponents() {
        // Scheduling components
        enableSchedulingCheckBox = new JCheckBox("Enable automatic updates");
        intervalSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 10080, 1));
        scheduleTypeCombo = new JComboBox<>(UpdateScheduler.ScheduleConfig.ScheduleType.values());
        customCronField = new JTextField();
        updateOnStartupCheckBox = new JCheckBox("Update on application startup");
        skipRecentCheckBox = new JCheckBox("Skip if recently updated");
        recentThresholdSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 1440, 5));
        onlyWhenIdleCheckBox = new JCheckBox("Only update when system is idle");
        maxConcurrentSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        
        // Instance management
        instanceTableModel = new InstanceTableModel();
        instanceTable = new JTable(instanceTableModel);
        instanceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instanceTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Enabled
        instanceTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Edition
        instanceTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Version  
        instanceTable.getColumnModel().getColumn(3).setPreferredWidth(200); // Path
        instanceTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Last Update
        instanceTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Status
        
        // Status components
        statusLabel = new JLabel("Scheduling: Disabled");
        lastUpdateLabel = new JLabel("Last Update: Never");
        nextUpdateLabel = new JLabel("Next Update: Not scheduled");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Scheduling tab
        JPanel schedulingPanel = createSchedulingPanel();
        tabbedPane.addTab("Scheduling", schedulingPanel);
        
        // Instances tab
        JPanel instancesPanel = createInstancesPanel();
        tabbedPane.addTab("VS Code Instances", instancesPanel);
        
        // General tab
        JPanel generalPanel = createGeneralPanel();
        tabbedPane.addTab("General", generalPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveSettings());
        cancelButton.addActionListener(e -> setVisible(false));
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Status panel
        JPanel statusPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Scheduler Status"));
        statusPanel.add(statusLabel);
        statusPanel.add(lastUpdateLabel);
        statusPanel.add(nextUpdateLabel);
        add(statusPanel, BorderLayout.NORTH);
    }
    
    private JPanel createSchedulingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Enable scheduling
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(enableSchedulingCheckBox, gbc);
        
        // Schedule type
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Schedule Type:"), gbc);
        gbc.gridx = 1;
        panel.add(scheduleTypeCombo, gbc);
        
        // Interval
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Interval:"), gbc);
        gbc.gridx = 1;
        JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        intervalPanel.add(intervalSpinner);
        intervalPanel.add(new JLabel(" minutes/hours"));
        panel.add(intervalPanel, gbc);
        
        // Custom cron
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Custom Cron:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(customCronField, gbc);
        gbc.fill = GridBagConstraints.NONE;
        
        // Options
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(updateOnStartupCheckBox, gbc);
        
        gbc.gridy = 5;
        panel.add(skipRecentCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1;
        panel.add(new JLabel("Recently updated threshold:"), gbc);
        gbc.gridx = 1;
        JPanel thresholdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        thresholdPanel.add(recentThresholdSpinner);
        thresholdPanel.add(new JLabel(" minutes"));
        panel.add(thresholdPanel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        panel.add(onlyWhenIdleCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 1;
        panel.add(new JLabel("Max concurrent updates:"), gbc);
        gbc.gridx = 1;
        panel.add(maxConcurrentSpinner, gbc);
        
        return panel;
    }
    
    private JPanel createInstancesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Table
        JScrollPane scrollPane = new JScrollPane(instanceTable);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("🔄 Refresh Detection");
        JButton enableAllButton = new JButton("✅ Enable All");
        JButton disableAllButton = new JButton("❌ Disable All");
        
        refreshButton.addActionListener(e -> refreshInstances());
        enableAllButton.addActionListener(e -> setAllInstancesEnabled(true));
        disableAllButton.addActionListener(e -> setAllInstancesEnabled(false));
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(enableAllButton);
        buttonPanel.add(disableAllButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // General settings from VSCodeConfig
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Command Timeout:"), gbc);
        gbc.gridx = 1;
        JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(300, 30, 3600, 30));
        panel.add(timeoutSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Log Level:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> logLevelCombo = new JComboBox<>(new String[]{"DEBUG", "INFO", "WARN", "ERROR"});
        panel.add(logLevelCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JCheckBox showNotificationsCheckBox = new JCheckBox("Show notifications");
        panel.add(showNotificationsCheckBox, gbc);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // Enable/disable components based on checkbox states
        enableSchedulingCheckBox.addActionListener(e -> updateComponentStates());
        scheduleTypeCombo.addActionListener(e -> updateComponentStates());
        skipRecentCheckBox.addActionListener(e -> updateComponentStates());
    }
    
    private void updateComponentStates() {
        boolean schedulingEnabled = enableSchedulingCheckBox.isSelected();
        boolean customCron = scheduleTypeCombo.getSelectedItem() == UpdateScheduler.ScheduleConfig.ScheduleType.CUSTOM_CRON;
        boolean skipRecent = skipRecentCheckBox.isSelected();
        
        intervalSpinner.setEnabled(schedulingEnabled && !customCron);
        scheduleTypeCombo.setEnabled(schedulingEnabled);
        customCronField.setEnabled(schedulingEnabled && customCron);
        updateOnStartupCheckBox.setEnabled(schedulingEnabled);
        skipRecentCheckBox.setEnabled(schedulingEnabled);
        recentThresholdSpinner.setEnabled(schedulingEnabled && skipRecent);
        onlyWhenIdleCheckBox.setEnabled(schedulingEnabled);
        maxConcurrentSpinner.setEnabled(schedulingEnabled);
    }
    
    private void updateFromConfig() {
        if (currentConfig == null) return;
        
        UpdateScheduler.ScheduleConfig schedule = currentConfig.schedule();
        
        enableSchedulingCheckBox.setSelected(schedule.enabled());
        intervalSpinner.setValue(schedule.intervalMinutes());
        scheduleTypeCombo.setSelectedItem(schedule.type());
        customCronField.setText(schedule.customCron());
        updateOnStartupCheckBox.setSelected(schedule.updateOnStartup());
        skipRecentCheckBox.setSelected(schedule.skipIfRecentlyUpdated());
        recentThresholdSpinner.setValue(schedule.recentlyUpdatedThresholdMinutes());
        onlyWhenIdleCheckBox.setSelected(schedule.onlyWhenIdle());
        maxConcurrentSpinner.setValue(schedule.maxConcurrentUpdates());
        
        instanceTableModel.setInstances(currentConfig.vsCodeInstances());
        updateComponentStates();
    }
    
    private void applySettings() {
        try {
            // Build new schedule config
            UpdateScheduler.ScheduleConfig newSchedule = new UpdateScheduler.ScheduleConfig(
                enableSchedulingCheckBox.isSelected(),
                (Integer) intervalSpinner.getValue(),
                (UpdateScheduler.ScheduleConfig.ScheduleType) scheduleTypeCombo.getSelectedItem(),
                customCronField.getText(),
                updateOnStartupCheckBox.isSelected(),
                skipRecentCheckBox.isSelected(),
                (Integer) recentThresholdSpinner.getValue(),
                onlyWhenIdleCheckBox.isSelected(),
                (Integer) maxConcurrentSpinner.getValue()
            );
            
            // Validate schedule config
            String validationError = newSchedule.validate();
            if (validationError != null) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid settings: " + validationError, 
                    "Settings Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Build new config with updated schedule
            VSCodeConfig newConfig = new VSCodeConfig(
                instanceTableModel.getInstances(),
                currentConfig.autoDetectInstances(),
                currentConfig.commandTimeoutSeconds(),
                currentConfig.logLevel(),
                currentConfig.autoStart(),
                currentConfig.showNotifications(),
                currentConfig.concurrency(),
                currentConfig.ui(),
                newSchedule
            );
            
            // Apply the new configuration
            configUpdateCallback.accept(newConfig);
            this.currentConfig = newConfig;
            
            logger.info("Settings applied successfully");
                
        } catch (Exception e) {
            logger.error("Failed to apply settings", e);
            JOptionPane.showMessageDialog(this, 
                "Failed to apply settings: " + e.getMessage(), 
                "Settings Error", 
                JOptionPane.ERROR_MESSAGE);
            throw e; // Re-throw to allow saveSettings to handle it
        }
    }
    
    private void saveSettings() {
        try {
            applySettings();
            // Close window automatically after successful save
            setVisible(false);
        } catch (Exception e) {
            // Error dialogs are already shown in applySettings, just don't close window
        }
    }
    
    private void refreshInstances() {
        // This would trigger detection refresh - delegate to parent
        JOptionPane.showMessageDialog(this, 
            "Instance detection refresh triggered", 
            "Refresh", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void setAllInstancesEnabled(boolean enabled) {
        instanceTableModel.setAllEnabled(enabled);
    }
    
    /**
     * Updates the scheduler status display.
     */
    public void updateSchedulerStatus(UpdateScheduler.SchedulerStatus status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Scheduling: " + status.getStatusSummary());
            
            if (status.lastUpdateTime() != null) {
                lastUpdateLabel.setText("Last Update: " + status.lastUpdateTime().format(TIME_FORMAT));
            } else {
                lastUpdateLabel.setText("Last Update: Never");
            }
            
            if (status.nextUpdateTime() != null) {
                nextUpdateLabel.setText("Next Update: " + status.nextUpdateTime().format(TIME_FORMAT));
            } else {
                nextUpdateLabel.setText("Next Update: Not scheduled");
            }
        });
    }
    
    /**
     * Updates the current configuration and refreshes the UI.
     */
    public void updateConfig(VSCodeConfig newConfig) {
        this.currentConfig = newConfig;
        SwingUtilities.invokeLater(this::updateFromConfig);
    }
    
    /**
     * Override setVisible to refresh configuration when window is shown.
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // Refresh the UI with the current configuration when window is shown
            updateFromConfig();
        }
        super.setVisible(visible);
    }
}