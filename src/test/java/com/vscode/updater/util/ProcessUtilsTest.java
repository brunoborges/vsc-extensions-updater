package com.vscode.updater.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProcessUtils functionality.
 */
class ProcessUtilsTest {
    
    @Test
    void testGetOSDisplayName() {
        String osDisplayName = ProcessUtils.getOSDisplayName();
        assertNotNull(osDisplayName);
        assertFalse(osDisplayName.isEmpty());
        assertTrue(osDisplayName.contains("("));
        assertTrue(osDisplayName.contains(")"));
    }
    
    @Test
    void testDetectVSCodePath() {
        // This test will depend on whether VS Code is actually installed
        // Just ensure the method doesn't throw exceptions
        String path = ProcessUtils.detectVSCodePath();
        // path could be null if VS Code is not installed, which is fine
        if (path != null) {
            assertFalse(path.isEmpty());
        }
    }
    
    @Test
    void testIsVSCodeValid() {
        // Test with null path
        assertFalse(ProcessUtils.isVSCodeValid(null));
        
        // Test with empty path
        assertFalse(ProcessUtils.isVSCodeValid(""));
        
        // Test with invalid path
        assertFalse(ProcessUtils.isVSCodeValid("/nonexistent/path"));
    }
}