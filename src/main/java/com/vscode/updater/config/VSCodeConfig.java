package com.vscode.updater.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vscode.updater.discovery.VSCodeInstance;
import com.vscode.updater.scheduler.UpdateScheduler;

import java.util.List;

/**
 * Enhanced configuration record for Milestone 3 with scheduling support.
 */
public record VSCodeConfig(
    @JsonProperty("vsCodeInstances") 
    @JsonAlias("enabledInstances") // Support legacy field name
    List<VSCodeInstance> vsCodeInstances,
    
    @JsonProperty("autoDetectInstances") 
    boolean autoDetectInstances,
    
    @JsonProperty("commandTimeout") 
    int commandTimeoutSeconds,
    
    @JsonProperty("logLevel") 
    String logLevel,
    
    @JsonProperty("autoStart") 
    boolean autoStart,
    
    @JsonProperty("showNotifications") 
    boolean showNotifications,
    
    @JsonProperty("concurrency")
    ConcurrencyConfig concurrency,
    
    @JsonProperty("ui")
    UIConfig ui,
    
    @JsonProperty("schedule")
    UpdateScheduler.ScheduleConfig schedule
) {
    
    public record ConcurrencyConfig(
        @JsonProperty("maxConcurrentUpdates")
        int maxConcurrentUpdates,
        
        @JsonProperty("useVirtualThreads") 
        boolean useVirtualThreads,
        
        @JsonProperty("updateInstancesSequentially")
        boolean updateInstancesSequentially
    ) {
        public static ConcurrencyConfig createDefault() {
            return new ConcurrencyConfig(3, true, false);
        }
    }
    
    public record UIConfig(
        @JsonProperty("startMinimized")
        boolean startMinimized,
        
        @JsonProperty("showInstancesInTray")
        boolean showInstancesInTray,
        
        @JsonProperty("groupLogsByInstance")
        boolean groupLogsByInstance,
        
        @JsonProperty("autoOpenLogsOnError")
        boolean autoOpenLogsOnError
    ) {
        public static UIConfig createDefault() {
            return new UIConfig(true, true, true, true);
        }
    }
    
    /**
     * Creates a default configuration with sensible defaults.
     */
    public static VSCodeConfig createDefault() {
        return new VSCodeConfig(
            List.of(), // Empty list, will be populated by auto-detection
            true,      // Auto-detect instances
            300,       // 5 minutes timeout
            "INFO",
            true,
            true,
            ConcurrencyConfig.createDefault(),
            UIConfig.createDefault(),
            UpdateScheduler.ScheduleConfig.createDefault()
        );
    }
    
    /**
     * Creates configuration with detected VS Code instances.
     */
    public static VSCodeConfig withInstances(List<VSCodeInstance> instances) {
        return new VSCodeConfig(
            instances,
            true,
            300,
            "INFO",
            true,
            true,
            ConcurrencyConfig.createDefault(),
            UIConfig.createDefault(),
            UpdateScheduler.ScheduleConfig.createDefault()
        );
    }
    
    /**
     * Returns a new config with updated VS Code instances.
     */
    public VSCodeConfig withUpdatedInstances(List<VSCodeInstance> newInstances) {
        return new VSCodeConfig(
            newInstances,
            autoDetectInstances,
            commandTimeoutSeconds,
            logLevel,
            autoStart,
            showNotifications,
            concurrency,
            ui,
            schedule
        );
    }
    
    /**
     * Returns a new config with updated schedule.
     */
    public VSCodeConfig withUpdatedSchedule(UpdateScheduler.ScheduleConfig newSchedule) {
        return new VSCodeConfig(
            vsCodeInstances,
            autoDetectInstances,
            commandTimeoutSeconds,
            logLevel,
            autoStart,
            showNotifications,
            concurrency,
            ui,
            newSchedule
        );
    }
    
    /**
     * Gets only the enabled VS Code instances.
     */
    @JsonIgnore
    public List<VSCodeInstance> getEnabledInstances() {
        return vsCodeInstances.stream()
            .filter(VSCodeInstance::enabled)
            .toList();
    }
    
    /**
     * Validates the configuration and returns any issues.
     */
    public String validate() {
        if (commandTimeoutSeconds <= 0) {
            return "Command timeout must be positive";
        }
        
        if (commandTimeoutSeconds > 3600) {
            return "Command timeout cannot exceed 1 hour";
        }
        
        if (!isValidLogLevel(logLevel)) {
            return "Log level must be one of: TRACE, DEBUG, INFO, WARN, ERROR";
        }
        
        if (concurrency.maxConcurrentUpdates <= 0) {
            return "Max concurrent updates must be positive";
        }
        
        if (concurrency.maxConcurrentUpdates > 10) {
            return "Max concurrent updates cannot exceed 10";
        }
        
        // Validate VS Code instances
        for (VSCodeInstance instance : vsCodeInstances) {
            if (!instance.isValid()) {
                return "Invalid VS Code instance: " + instance.executablePath();
            }
        }
        
        // Validate schedule configuration
        if (schedule != null) {
            String scheduleValidation = schedule.validate();
            if (scheduleValidation != null) {
                return "Invalid schedule configuration: " + scheduleValidation;
            }
        }
        
        return null; // No validation errors
    }
    
    private boolean isValidLogLevel(String level) {
        return level != null && 
               level.matches("(?i)(TRACE|DEBUG|INFO|WARN|ERROR)");
    }
}