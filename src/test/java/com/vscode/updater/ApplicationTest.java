package com.vscode.updater;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the main Application class.
 */
class ApplicationTest {
    
    @Test
    void testApplicationCanBeInstantiated() {
        // This test verifies that the Application class loads without issues
        // We can't test the GUI components without a display, but we can test class loading
        assertDoesNotThrow(() -> {
            Application app = new Application();
        });
    }
}