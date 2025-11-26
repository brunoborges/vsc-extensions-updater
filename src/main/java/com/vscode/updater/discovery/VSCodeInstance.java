package com.vscode.updater.discovery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a detected VS Code installation.
 * Uses Java Records for immutable data representation.
 */
public record VSCodeInstance(
    @JsonProperty("path")
    String executablePath,
    
    @JsonProperty("edition") 
    VSCodeEdition edition,
    
    @JsonProperty("version")
    String version,
    
    @JsonProperty("displayName")
    String displayName,
    
    @JsonProperty("enabled")
    boolean enabled,
    
    @JsonProperty("lastUpdate")
    String lastUpdateTime,
    
    @JsonProperty("updateStatus")
    String lastUpdateStatus
) {
    
    public enum VSCodeEdition {
        STABLE("VS Code", "code"),
        INSIDERS("VS Code Insiders", "code-insiders");
        
        private final String displayName;
        private final String executable;
        
        VSCodeEdition(String displayName, String executable) {
            this.displayName = displayName;
            this.executable = executable;
        }
        
        public String getDisplayName() { return displayName; }
        public String getExecutable() { return executable; }
    }
    
    /**
     * Creates a new instance with updated status.
     */
    public VSCodeInstance withUpdateStatus(String updateTime, String status) {
        return new VSCodeInstance(
            executablePath, edition, version, displayName, 
            enabled, updateTime, status
        );
    }
    
    /**
     * Creates a new instance with enabled state changed.
     */
    public VSCodeInstance withEnabled(boolean newEnabled) {
        return new VSCodeInstance(
            executablePath, edition, version, displayName, 
            newEnabled, lastUpdateTime, lastUpdateStatus
        );
    }
    
    /**
     * Gets a short display path for UI purposes.
     */
    @JsonIgnore
    public String getShortPath() {
        if (executablePath == null || executablePath.length() <= 40) {
            return executablePath;
        }
        return "..." + executablePath.substring(executablePath.length() - 37);
    }
    
    /**
     * Gets the command arguments for updating extensions.
     */
    @JsonIgnore
    public String[] getUpdateCommand() {
        return new String[]{executablePath, "--update-extensions"};
    }
    
    /**
     * Validates that this VS Code instance is usable.
     */
    @JsonIgnore
    public boolean isValid() {
        return executablePath != null && 
               !executablePath.isEmpty() && 
               edition != null;
    }
}