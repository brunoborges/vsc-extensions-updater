package com.vscode.updater.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BasicConfig functionality.
 */
class BasicConfigTest {
    
    @Test
    void testDefaultConfigCreation() {
        BasicConfig config = BasicConfig.createDefault();
        
        assertNull(config.vsCodePath());
        assertEquals(300, config.commandTimeoutSeconds());
        assertEquals("INFO", config.logLevel());
        assertTrue(config.autoStart());
        assertTrue(config.showNotifications());
    }
    
    @Test
    void testConfigValidation() {
        // Valid config
        BasicConfig validConfig = new BasicConfig(null, 300, "INFO", true, true);
        assertNull(validConfig.validate());
        
        // Invalid timeout - negative
        BasicConfig invalidTimeout1 = new BasicConfig(null, -1, "INFO", true, true);
        assertNotNull(invalidTimeout1.validate());
        
        // Invalid timeout - too high
        BasicConfig invalidTimeout2 = new BasicConfig(null, 4000, "INFO", true, true);
        assertNotNull(invalidTimeout2.validate());
        
        // Invalid log level
        BasicConfig invalidLogLevel = new BasicConfig(null, 300, "INVALID", true, true);
        assertNotNull(invalidLogLevel.validate());
    }
    
    @Test
    void testValidLogLevels() {
        String[] validLevels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "trace", "debug", "info", "warn", "error"};
        
        for (String level : validLevels) {
            BasicConfig config = new BasicConfig(null, 300, level, true, true);
            assertNull(config.validate(), "Log level " + level + " should be valid");
        }
    }
}