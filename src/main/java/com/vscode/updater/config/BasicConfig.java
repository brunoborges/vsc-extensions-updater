package com.vscode.updater.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic configuration record for MVP milestone.
 * Contains minimal settings needed for system tray app functionality.
 */
public record BasicConfig(
    @JsonProperty("vsCodePath") 
    String vsCodePath,
    
    @JsonProperty("commandTimeout") 
    int commandTimeoutSeconds,
    
    @JsonProperty("logLevel") 
    String logLevel,
    
    @JsonProperty("autoStart") 
    boolean autoStart,
    
    @JsonProperty("showNotifications") 
    boolean showNotifications
) {
    
    /**
     * Creates a default configuration with sensible defaults.
     */
    public static BasicConfig createDefault() {
        return new BasicConfig(
            null, // Auto-detect VS Code path
            300,  // 5 minutes timeout
            "INFO",
            true,
            true
        );
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
        
        return null; // No validation errors
    }
    
    private boolean isValidLogLevel(String level) {
        return level != null && 
               level.matches("(?i)(TRACE|DEBUG|INFO|WARN|ERROR)");
    }
}