package com.vscode.updater.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vscode.updater.discovery.VSCodeDetector;
import com.vscode.updater.discovery.VSCodeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Enhanced configuration manager for Milestone 2 with multi-instance VS Code support.
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE_NAME = "config.json";
    
    private final Path configDir;
    private final Path configFile;
    private final ObjectMapper objectMapper;
    
    public ConfigManager() {
        this.configDir = getConfigDirectory();
        this.configFile = configDir.resolve(CONFIG_FILE_NAME);
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
        
        ensureConfigDirectoryExists();
    }
    
    /**
     * Loads configuration from file, creating default with auto-detection if none exists.
     */
    public VSCodeConfig loadConfig() {
        if (!Files.exists(configFile)) {
            logger.info("Configuration file not found, creating default config with VS Code detection");
            VSCodeConfig defaultConfig = createConfigWithDetection();
            saveConfig(defaultConfig);
            return defaultConfig;
        }
        
        try {
            VSCodeConfig config = objectMapper.readValue(configFile.toFile(), VSCodeConfig.class);
            
            // Validate configuration
            String validationError = config.validate();
            if (validationError != null) {
                logger.warn("Invalid configuration: {}. Using defaults.", validationError);
                return createConfigWithDetection();
            }
            
            // If auto-detection is enabled and no instances configured, detect now
            if (config.autoDetectInstances() && config.vsCodeInstances().isEmpty()) {
                logger.info("Auto-detection enabled but no instances configured, running detection");
                List<VSCodeInstance> detectedInstances = VSCodeDetector.detectInstallations();
                config = config.withUpdatedInstances(detectedInstances);
                saveConfig(config); // Save updated config
            }
            
            logger.info("Configuration loaded successfully with {} VS Code instance(s)", 
                config.vsCodeInstances().size());
            return config;
            
        } catch (IOException e) {
            logger.error("Failed to load configuration: {}", e.getMessage());
            logger.info("Using default configuration with detection");
            return createConfigWithDetection();
        }
    }
    
    /**
     * Saves configuration to file.
     */
    public void saveConfig(VSCodeConfig config) {
        try {
            String validationError = config.validate();
            if (validationError != null) {
                throw new IllegalArgumentException("Invalid configuration: " + validationError);
            }
            
            // Ensure config directory exists before saving
            ensureConfigDirectoryExists();
            
            objectMapper.writeValue(configFile.toFile(), config);
            logger.info("Configuration saved successfully with {} VS Code instance(s)", 
                config.vsCodeInstances().size());
            
        } catch (IOException e) {
            logger.error("Failed to save configuration to {}: {}", configFile, e.getMessage());
            throw new RuntimeException("Configuration save failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while saving configuration: {}", e.getMessage());
            throw new RuntimeException("Configuration save failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Re-detects VS Code installations and updates the configuration.
     */
    public VSCodeConfig refreshDetection(VSCodeConfig currentConfig) {
        logger.info("Refreshing VS Code installation detection");
        List<VSCodeInstance> detectedInstances = VSCodeDetector.detectInstallations();
        
        // Merge with existing configuration, preserving enabled/disabled state
        List<VSCodeInstance> mergedInstances = mergeInstances(
            currentConfig.vsCodeInstances(), 
            detectedInstances
        );
        
        VSCodeConfig updatedConfig = currentConfig.withUpdatedInstances(mergedInstances);
        saveConfig(updatedConfig);
        
        logger.info("Detection refresh complete. Found {} VS Code instance(s)", 
            mergedInstances.size());
        return updatedConfig;
    }
    
    private VSCodeConfig createConfigWithDetection() {
        logger.info("Running VS Code installation detection...");
        List<VSCodeInstance> detectedInstances = VSCodeDetector.detectInstallations();
        return VSCodeConfig.withInstances(detectedInstances);
    }
    
    private List<VSCodeInstance> mergeInstances(
            List<VSCodeInstance> existing, 
            List<VSCodeInstance> detected) {
        
        return detected.stream().map(detectedInstance -> {
            // Find existing instance with same path and edition
            return existing.stream()
                .filter(existingInstance -> 
                    existingInstance.executablePath().equals(detectedInstance.executablePath()) &&
                    existingInstance.edition() == detectedInstance.edition())
                .findFirst()
                .map(existingInstance -> 
                    // Preserve enabled state and update info from existing
                    new VSCodeInstance(
                        detectedInstance.executablePath(),
                        detectedInstance.edition(),
                        detectedInstance.version(), // Use detected version (might be updated)
                        detectedInstance.displayName(),
                        existingInstance.enabled(), // Preserve enabled state
                        existingInstance.lastUpdateTime(), // Preserve last update info
                        existingInstance.lastUpdateStatus()
                    ))
                .orElse(detectedInstance); // Use detected instance if not in existing config
        }).toList();
    }
    
    /**
     * Gets the configuration file path.
     */
    public Path getConfigFile() {
        return configFile;
    }
    
    /**
     * Gets the configuration directory path.
     */
    public Path getConfigDirectory() {
        String osName = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        
        if (osName.contains("win")) {
            // Windows: %APPDATA%\VSCodeExtensionUpdater
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                return Path.of(appData, "VSCodeExtensionUpdater");
            }
            return Path.of(userHome, "AppData", "Roaming", "VSCodeExtensionUpdater");
        } else if (osName.contains("mac")) {
            // macOS: ~/Library/Application Support/VSCodeExtensionUpdater
            return Path.of(userHome, "Library", "Application Support", "VSCodeExtensionUpdater");
        } else {
            // Linux: ~/.config/vscode-extension-updater
            return Path.of(userHome, ".config", "vscode-extension-updater");
        }
    }
    
    private void ensureConfigDirectoryExists() {
        try {
            Files.createDirectories(configDir);
            logger.debug("Configuration directory: {}", configDir);
        } catch (IOException e) {
            logger.error("Failed to create configuration directory: {}", e.getMessage());
            throw new RuntimeException("Configuration directory creation failed", e);
        }
    }
}