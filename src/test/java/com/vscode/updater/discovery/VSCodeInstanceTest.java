package com.vscode.updater.discovery;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VSCodeInstance functionality.
 */
class VSCodeInstanceTest {
    
    @Test
    void testVSCodeInstanceCreation() {
        VSCodeInstance instance = new VSCodeInstance(
            "/path/to/code",
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0",
            "VS Code (1.85.0)",
            true,
            "Never",
            "Not run"
        );
        
        assertTrue(instance.isValid());
        assertEquals("VS Code", instance.edition().getDisplayName());
        assertEquals("code", instance.edition().getExecutable());
        assertTrue(instance.enabled());
    }
    
    @Test
    void testVSCodeInstanceWithUpdateStatus() {
        VSCodeInstance original = new VSCodeInstance(
            "/path/to/code",
            VSCodeInstance.VSCodeEdition.INSIDERS,
            "1.86.0",
            "VS Code Insiders",
            true,
            "Never",
            "Not run"
        );
        
        VSCodeInstance updated = original.withUpdateStatus("2024-01-15 10:30", "Success");
        
        assertEquals("2024-01-15 10:30", updated.lastUpdateTime());
        assertEquals("Success", updated.lastUpdateStatus());
        assertEquals(original.executablePath(), updated.executablePath());
        assertEquals(original.edition(), updated.edition());
    }
    
    @Test
    void testVSCodeInstanceToggleEnabled() {
        VSCodeInstance instance = new VSCodeInstance(
            "/path/to/code",
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0",
            "VS Code",
            true,
            "Never", 
            "Not run"
        );
        
        VSCodeInstance disabled = instance.withEnabled(false);
        assertFalse(disabled.enabled());
        assertTrue(instance.enabled()); // Original unchanged
        
        VSCodeInstance reEnabled = disabled.withEnabled(true);
        assertTrue(reEnabled.enabled());
    }
    
    @Test
    void testGetShortPath() {
        VSCodeInstance shortPath = new VSCodeInstance(
            "/usr/bin/code",
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0",
            "VS Code",
            true,
            "Never",
            "Not run"
        );
        
        assertEquals("/usr/bin/code", shortPath.getShortPath());
        
        VSCodeInstance longPath = new VSCodeInstance(
            "/very/long/path/to/vs/code/installation/directory/bin/code",
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0", 
            "VS Code",
            true,
            "Never",
            "Not run"
        );
        
        assertTrue(longPath.getShortPath().startsWith("..."));
        assertTrue(longPath.getShortPath().length() <= 40);
    }
    
    @Test
    void testGetUpdateCommand() {
        VSCodeInstance instance = new VSCodeInstance(
            "/path/to/code",
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0",
            "VS Code",
            true,
            "Never",
            "Not run"
        );
        
        String[] command = instance.getUpdateCommand();
        assertEquals(2, command.length);
        assertEquals("/path/to/code", command[0]);
        assertEquals("--update-extensions", command[1]);
    }
    
    @Test
    void testInvalidInstance() {
        VSCodeInstance invalid1 = new VSCodeInstance(
            null, // Invalid path
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0",
            "VS Code",
            true,
            "Never",
            "Not run"
        );
        assertFalse(invalid1.isValid());
        
        VSCodeInstance invalid2 = new VSCodeInstance(
            "/path/to/code",
            null, // Invalid edition
            "1.85.0",
            "VS Code",
            true,
            "Never",
            "Not run"
        );
        assertFalse(invalid2.isValid());
        
        VSCodeInstance invalid3 = new VSCodeInstance(
            "", // Empty path
            VSCodeInstance.VSCodeEdition.STABLE,
            "1.85.0", 
            "VS Code",
            true,
            "Never",
            "Not run"
        );
        assertFalse(invalid3.isValid());
    }
}