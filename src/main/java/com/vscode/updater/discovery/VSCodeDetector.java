package com.vscode.updater.discovery;

import com.vscode.updater.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Detects VS Code installations across different platforms and editions.
 * Supports both VS Code stable and VS Code Insiders.
 */
public class VSCodeDetector {
    private static final Logger logger = LoggerFactory.getLogger(VSCodeDetector.class);
    
    /**
     * Detects all VS Code installations on the current platform.
     */
    public static List<VSCodeInstance> detectInstallations() {
        logger.info("Starting VS Code installation detection");
        List<VSCodeInstance> instances = new ArrayList<>();
        
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            instances.addAll(detectWindowsInstallations());
        } else if (osName.contains("mac")) {
            instances.addAll(detectMacOSInstallations());
        } else {
            instances.addAll(detectLinuxInstallations());
        }
        
        // Validate each instance and get version information
        List<VSCodeInstance> validInstances = new ArrayList<>();
        for (VSCodeInstance instance : instances) {
            VSCodeInstance validatedInstance = validateAndEnrichInstance(instance);
            if (validatedInstance != null) {
                validInstances.add(validatedInstance);
                logger.info("Detected: {} at {}", validatedInstance.displayName(), validatedInstance.getShortPath());
            }
        }
        
        logger.info("Detection complete. Found {} valid VS Code installation(s)", validInstances.size());
        return validInstances;
    }
    
    private static List<VSCodeInstance> detectWindowsInstallations() {
        List<VSCodeInstance> instances = new ArrayList<>();
        
        // VS Code Stable paths
        String[] stablePaths = {
            System.getenv("LOCALAPPDATA") + "\\Programs\\Microsoft VS Code\\bin\\code.cmd",
            System.getenv("PROGRAMFILES") + "\\Microsoft VS Code\\bin\\code.cmd",
            "C:\\Program Files\\Microsoft VS Code\\bin\\code.cmd"
        };
        
        for (String path : stablePaths) {
            if (path != null && Files.exists(Paths.get(path))) {
                instances.add(createInstance(path, VSCodeInstance.VSCodeEdition.STABLE));
                break; // Only add one stable instance
            }
        }
        
        // VS Code Insiders paths
        String[] insidersPaths = {
            System.getenv("LOCALAPPDATA") + "\\Programs\\Microsoft VS Code Insiders\\bin\\code-insiders.cmd",
            System.getenv("PROGRAMFILES") + "\\Microsoft VS Code Insiders\\bin\\code-insiders.cmd",
            "C:\\Program Files\\Microsoft VS Code Insiders\\bin\\code-insiders.cmd"
        };
        
        for (String path : insidersPaths) {
            if (path != null && Files.exists(Paths.get(path))) {
                instances.add(createInstance(path, VSCodeInstance.VSCodeEdition.INSIDERS));
                break; // Only add one insiders instance
            }
        }
        
        // Try PATH lookup for both
        addFromPath(instances, "code.cmd", VSCodeInstance.VSCodeEdition.STABLE);
        addFromPath(instances, "code-insiders.cmd", VSCodeInstance.VSCodeEdition.INSIDERS);
        
        return instances;
    }
    
    private static List<VSCodeInstance> detectMacOSInstallations() {
        List<VSCodeInstance> instances = new ArrayList<>();
        
        // VS Code Stable paths
        String[] stablePaths = {
            "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code",
            "/usr/local/bin/code",
            System.getProperty("user.home") + "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code"
        };
        
        for (String path : stablePaths) {
            if (Files.exists(Paths.get(path))) {
                instances.add(createInstance(path, VSCodeInstance.VSCodeEdition.STABLE));
                break; // Only add one stable instance
            }
        }
        
        // VS Code Insiders paths
        String[] insidersPaths = {
            "/Applications/Visual Studio Code - Insiders.app/Contents/Resources/app/bin/code", // Correct binary name
            "/usr/local/bin/code-insiders",
            System.getProperty("user.home") + "/Applications/Visual Studio Code - Insiders.app/Contents/Resources/app/bin/code"
        };
        
        for (String path : insidersPaths) {
            if (Files.exists(Paths.get(path)) && isInsidersPath(path)) {
                instances.add(createInstance(path, VSCodeInstance.VSCodeEdition.INSIDERS));
                break; // Only add one insiders instance
            }
        }
        
        // Try PATH lookup for both
        addFromPath(instances, "code", VSCodeInstance.VSCodeEdition.STABLE);
        addFromPath(instances, "code-insiders", VSCodeInstance.VSCodeEdition.INSIDERS);
        
        return instances;
    }
    
    private static List<VSCodeInstance> detectLinuxInstallations() {
        List<VSCodeInstance> instances = new ArrayList<>();
        
        // VS Code Stable paths
        String[] stablePaths = {
            "/usr/bin/code",
            "/usr/local/bin/code",
            "/snap/bin/code",
            "/opt/visual-studio-code/bin/code",
            "/flatpak/bin/code"
        };
        
        for (String path : stablePaths) {
            if (Files.exists(Paths.get(path))) {
                instances.add(createInstance(path, VSCodeInstance.VSCodeEdition.STABLE));
                break; // Only add one stable instance
            }
        }
        
        // VS Code Insiders paths
        String[] insidersPaths = {
            "/usr/bin/code-insiders",
            "/usr/local/bin/code-insiders",
            "/snap/bin/code-insiders",
            "/opt/visual-studio-code-insiders/bin/code-insiders"
        };
        
        for (String path : insidersPaths) {
            if (Files.exists(Paths.get(path))) {
                instances.add(createInstance(path, VSCodeInstance.VSCodeEdition.INSIDERS));
                break; // Only add one insiders instance
            }
        }
        
        // Try PATH lookup for both
        addFromPath(instances, "code", VSCodeInstance.VSCodeEdition.STABLE);
        addFromPath(instances, "code-insiders", VSCodeInstance.VSCodeEdition.INSIDERS);
        
        return instances;
    }
    
    private static void addFromPath(List<VSCodeInstance> instances, String executable, VSCodeInstance.VSCodeEdition edition) {
        // Don't add from PATH if we already have this edition
        boolean hasEdition = instances.stream().anyMatch(i -> i.edition() == edition);
        if (hasEdition) return;
        
        String path = findInPath(executable);
        if (path != null) {
            // Additional validation for Insiders to make sure the path matches the edition
            if (edition == VSCodeInstance.VSCodeEdition.INSIDERS && !isInsidersPath(path)) {
                logger.debug("PATH executable {} does not appear to be Insiders edition", path);
                return;
            }
            if (edition == VSCodeInstance.VSCodeEdition.STABLE && isInsidersPath(path)) {
                logger.debug("PATH executable {} appears to be Insiders edition, skipping for stable", path);
                return;
            }
            instances.add(createInstance(path, edition));
        }
    }
    
    private static String findInPath(String executable) {
        try {
            String[] command = System.getProperty("os.name").toLowerCase().contains("win") 
                ? new String[]{"where", executable}
                : new String[]{"which", executable};
                
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (finished && process.exitValue() == 0) {
                String result = new String(process.getInputStream().readAllBytes()).trim();
                if (!result.isEmpty()) {
                    return result.split("\n")[0]; // Return first result
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.debug("Failed to find {} in PATH: {}", executable, e.getMessage());
        }
        
        return null;
    }
    
    private static VSCodeInstance createInstance(String path, VSCodeInstance.VSCodeEdition edition) {
        return new VSCodeInstance(
            path,
            edition,
            "Unknown", // Will be populated by validation
            edition.getDisplayName(),
            true, // Enabled by default
            "Never",
            "Not run"
        );
    }
    
    private static VSCodeInstance validateAndEnrichInstance(VSCodeInstance instance) {
        if (!instance.isValid()) {
            return null;
        }
        
        try {
            // Test if executable works and get version
            ProcessBuilder pb = new ProcessBuilder(instance.executablePath(), "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            
            if (finished && process.exitValue() == 0) {
                String output = new String(process.getInputStream().readAllBytes()).trim();
                String[] lines = output.split("\n");
                String version = lines.length > 0 ? lines[0] : "Unknown";
                
                // Return enriched instance
                return new VSCodeInstance(
                    instance.executablePath(),
                    instance.edition(),
                    version,
                    instance.displayName() + " (" + version + ")",
                    instance.enabled(),
                    instance.lastUpdateTime(),
                    instance.lastUpdateStatus()
                );
            } else {
                logger.warn("VS Code validation failed for {}: exit code {}", 
                    instance.executablePath(), finished ? process.exitValue() : "timeout");
                return null;
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to validate VS Code instance {}: {}", instance.executablePath(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Asynchronously detects VS Code installations.
     */
    public static CompletableFuture<List<VSCodeInstance>> detectInstallationsAsync() {
        return CompletableFuture.supplyAsync(VSCodeDetector::detectInstallations);
    }
    
    /**
     * Checks if a given path represents VS Code Insiders installation.
     */
    private static boolean isInsidersPath(String path) {
        if (path == null) return false;
        
        // Check if path contains "Insiders" 
        if (path.contains("Insiders") || path.contains("insiders")) {
            return true;
        }
        
        // For symlinks, check the actual target
        try {
            Path realPath = Paths.get(path).toRealPath();
            return realPath.toString().contains("Insiders");
        } catch (IOException e) {
            logger.debug("Could not resolve real path for {}: {}", path, e.getMessage());
            return false;
        }
    }
}