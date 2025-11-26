package com.vscode.updater.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for accessing application information from build properties.
 */
public class AppInfo {
    private static final Logger logger = LoggerFactory.getLogger(AppInfo.class);
    private static final Properties properties = new Properties();
    private static boolean loaded = false;
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        try (InputStream is = AppInfo.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
                loaded = true;
                logger.debug("Application properties loaded successfully");
            } else {
                logger.warn("application.properties not found in classpath");
            }
        } catch (IOException e) {
            logger.error("Failed to load application properties", e);
        }
    }
    
    public static String getName() {
        return properties.getProperty("app.name", "VS Code Extension Updater");
    }
    
    public static String getVersion() {
        return properties.getProperty("app.version", "1.0");
    }
    
    public static String getDescription() {
        return properties.getProperty("app.description", "Background application for updating VS Code extensions");
    }
    
    public static String getAuthor() {
        return properties.getProperty("app.author", "Bruno Borges");
    }
    
    public static String getLicense() {
        return properties.getProperty("app.license", "MIT License");
    }
    
    public static String getJavaVersion() {
        return properties.getProperty("app.java.version", "21");
    }
    
    public static boolean isLoaded() {
        return loaded;
    }
    
    /**
     * Gets formatted application information for About dialogs.
     */
    public static String getFormattedInfo() {
        return String.format("""
            %s
            Version: %s
            
            Author: %s
            License: %s
            
            Description:
            %s
            
            Platform: %s
            Java: %s (Target: %s)
            """, 
            getName(),
            getVersion(),
            getAuthor(),
            getLicense(),
            getDescription(),
            ProcessUtils.getOSDisplayName(),
            System.getProperty("java.version"),
            getJavaVersion()
        );
    }
}