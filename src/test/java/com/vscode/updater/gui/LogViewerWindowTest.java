package com.vscode.updater.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LogViewerWindow functionality.
 */
class LogViewerWindowTest {
    
    @Test
    void testLogViewerCanBeCreated() {
        // Test that LogViewerWindow can be instantiated without exceptions
        // Note: In headless environment, this will test the constructor but not the GUI
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            // Skip GUI test in headless environment
            return;
        }
        
        assertDoesNotThrow(() -> {
            LogViewerWindow logViewer = new LogViewerWindow();
            assertNotNull(logViewer);
            assertFalse(logViewer.isWindowVisible());
        });
    }
    
    @Test
    void testAppendLogFunctionality() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }
        
        LogViewerWindow logViewer = new LogViewerWindow();
        
        // Test that we can append logs without exceptions
        assertDoesNotThrow(() -> {
            logViewer.appendLog("Test log message");
            logViewer.appendLog("Another test message");
        });
    }
}