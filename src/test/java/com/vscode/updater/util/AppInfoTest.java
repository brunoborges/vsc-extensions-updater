package com.vscode.updater.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AppInfo functionality.
 */
class AppInfoTest {
    
    @Test
    void testAppInfoProperties() {
        // Test that app info can be read
        assertNotNull(AppInfo.getName());
        assertNotNull(AppInfo.getVersion());
        assertNotNull(AppInfo.getAuthor());
        assertNotNull(AppInfo.getLicense());
        assertNotNull(AppInfo.getDescription());
        
        // Test expected values
        assertEquals("Bruno Borges", AppInfo.getAuthor());
        assertEquals("MIT License", AppInfo.getLicense());
        assertEquals("1.0", AppInfo.getVersion());
        assertTrue(AppInfo.getName().contains("VS Code"));
    }
    
    @Test
    void testFormattedInfo() {
        String info = AppInfo.getFormattedInfo();
        assertNotNull(info);
        assertFalse(info.isEmpty());
        
        // Should contain key information
        assertTrue(info.contains("Bruno Borges"));
        assertTrue(info.contains("MIT License"));
        assertTrue(info.contains("1.0"));
        assertTrue(info.contains("VS Code"));
    }
}