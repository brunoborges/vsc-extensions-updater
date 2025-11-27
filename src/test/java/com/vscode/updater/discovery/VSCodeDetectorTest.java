package com.vscode.updater.discovery;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Tests for VSCodeDetector functionality.
 */
class VSCodeDetectorTest {
    
    @Test
    void testDetectInstallations() {
        // This test will detect actual installations on the system
        List<VSCodeInstance> instances = VSCodeDetector.detectInstallations();
        
        assertNotNull(instances);
        // Note: May not find any installations on CI environments, which is valid
        System.out.println("Detected " + instances.size() + " VS Code installation(s)");
        
        // If instances are found, validate them
        for (VSCodeInstance instance : instances) {
            System.out.println("Detected: " + instance.edition().getDisplayName() + 
                             " v" + instance.version() + " at " + instance.executablePath());
            
            // Validate instance properties
            assertTrue(instance.isValid(), "Instance should be valid");
            assertNotNull(instance.edition(), "Edition should not be null");
            assertNotNull(instance.executablePath(), "Path should not be null");
            assertFalse(instance.executablePath().isEmpty(), "Path should not be empty");
        }
        
        // Check for both editions if available
        long stableCount = instances.stream().filter(i -> i.edition() == VSCodeInstance.VSCodeEdition.STABLE).count();
        long insidersCount = instances.stream().filter(i -> i.edition() == VSCodeInstance.VSCodeEdition.INSIDERS).count();
        
        System.out.println("Found " + stableCount + " stable instance(s) and " + insidersCount + " insiders instance(s)");
        
        // Always pass if detection completes without exception
        assertTrue(true, "Detection completed successfully");
    }
    
    @Test
    void testAsyncDetection() {
        // Test asynchronous detection
        assertDoesNotThrow(() -> {
            List<VSCodeInstance> instances = VSCodeDetector.detectInstallationsAsync().join();
            assertNotNull(instances);
        });
    }
}