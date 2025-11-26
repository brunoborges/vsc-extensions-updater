package com.vscode.updater.config;

import com.vscode.updater.discovery.VSCodeInstance;
import com.vscode.updater.scheduler.UpdateScheduler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Tests for VSCodeConfig functionality.
 */
class VSCodeConfigTest {
    
    @Test
    void testDefaultConfigCreation() {
        VSCodeConfig config = VSCodeConfig.createDefault();
        
        assertTrue(config.vsCodeInstances().isEmpty());
        assertTrue(config.autoDetectInstances());
        assertEquals(300, config.commandTimeoutSeconds());
        assertEquals("INFO", config.logLevel());
        assertTrue(config.autoStart());
        assertTrue(config.showNotifications());
        assertNotNull(config.concurrency());
        assertNotNull(config.ui());
    }
    
    @Test
    void testConfigWithInstances() {
        VSCodeInstance instance = new VSCodeInstance(
            "/path/to/code",
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0",
            "VS Code",
            true,
            "Never",
            "Not run"
        );
        
        VSCodeConfig config = VSCodeConfig.withInstances(List.of(instance));
        
        assertEquals(1, config.vsCodeInstances().size());
        assertEquals(instance, config.vsCodeInstances().get(0));
        assertEquals(1, config.getEnabledInstances().size());
    }
    
    @Test
    void testGetEnabledInstances() {
        VSCodeInstance enabled = new VSCodeInstance(
            "/path/to/code",
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0",
            "VS Code",
            true,
            "Never",
            "Not run"
        );
        
        VSCodeInstance disabled = new VSCodeInstance(
            "/path/to/code-insiders", 
            VSCodeInstance.VSCodeEdition.INSIDERS,
            "1.86.0",
            "VS Code Insiders",
            false,
            "Never",
            "Not run"
        );
        
        VSCodeConfig config = VSCodeConfig.withInstances(List.of(enabled, disabled));
        
        assertEquals(2, config.vsCodeInstances().size());
        assertEquals(1, config.getEnabledInstances().size());
        assertEquals(enabled, config.getEnabledInstances().get(0));
    }
    
    @Test
    void testWithUpdatedInstances() {
        VSCodeInstance original = new VSCodeInstance(
            "/path/to/code",
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0",
            "VS Code",
            true,
            "Never",
            "Not run"
        );
        
        VSCodeConfig config = VSCodeConfig.withInstances(List.of(original));
        
        VSCodeInstance updated = original.withUpdateStatus("2024-01-15", "Success");
        VSCodeConfig newConfig = config.withUpdatedInstances(List.of(updated));
        
        assertEquals(1, newConfig.vsCodeInstances().size());
        assertEquals("Success", newConfig.vsCodeInstances().get(0).lastUpdateStatus());
    }
    
    @Test
    void testConfigValidation() {
        // Valid config
        VSCodeConfig validConfig = VSCodeConfig.createDefault();
        assertNull(validConfig.validate());
        
        // Invalid timeout - negative
        VSCodeConfig invalidTimeout = new VSCodeConfig(
            List.of(),
            true,
            -1,
            "INFO",
            true,
            true,
            VSCodeConfig.ConcurrencyConfig.createDefault(),
            VSCodeConfig.UIConfig.createDefault(),
            UpdateScheduler.ScheduleConfig.createDefault()
        );
        assertNotNull(invalidTimeout.validate());
        
        // Invalid log level
        VSCodeConfig invalidLogLevel = new VSCodeConfig(
            List.of(),
            true,
            300,
            "INVALID",
            true,
            true,
            VSCodeConfig.ConcurrencyConfig.createDefault(),
            VSCodeConfig.UIConfig.createDefault(),
            UpdateScheduler.ScheduleConfig.createDefault()
        );
        assertNotNull(invalidLogLevel.validate());
        
        // Invalid concurrency
        VSCodeConfig.ConcurrencyConfig invalidConcurrency = new VSCodeConfig.ConcurrencyConfig(0, true, false);
        VSCodeConfig invalidConcurrencyConfig = new VSCodeConfig(
            List.of(),
            true,
            300,
            "INFO",
            true,
            true,
            invalidConcurrency,
            VSCodeConfig.UIConfig.createDefault(),
            UpdateScheduler.ScheduleConfig.createDefault()
        );
        assertNotNull(invalidConcurrencyConfig.validate());
    }
    
    @Test
    void testConcurrencyConfigDefaults() {
        VSCodeConfig.ConcurrencyConfig config = VSCodeConfig.ConcurrencyConfig.createDefault();
        
        assertEquals(3, config.maxConcurrentUpdates());
        assertTrue(config.useVirtualThreads());
        assertFalse(config.updateInstancesSequentially());
    }
    
    @Test
    void testUIConfigDefaults() {
        VSCodeConfig.UIConfig config = VSCodeConfig.UIConfig.createDefault();
        
        assertTrue(config.startMinimized());
        assertTrue(config.showInstancesInTray());
        assertTrue(config.groupLogsByInstance());
        assertTrue(config.autoOpenLogsOnError());
    }
}