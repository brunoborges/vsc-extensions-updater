package com.vscode.updater.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for process execution and VS Code detection.
 */
public class ProcessUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);
    
    /**
     * Detects VS Code installation path based on the current platform.
     */
    public static String detectVSCodePath() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return detectWindowsVSCode();
        } else if (osName.contains("mac")) {
            return detectMacOSVSCode();
        } else {
            return detectLinuxVSCode();
        }
    }
    
    private static String detectWindowsVSCode() {
        // Try common Windows installation paths
        String[] paths = {
            System.getenv("LOCALAPPDATA") + "\\Programs\\Microsoft VS Code\\bin\\code.cmd",
            System.getenv("PROGRAMFILES") + "\\Microsoft VS Code\\bin\\code.cmd",
            "C:\\Program Files\\Microsoft VS Code\\bin\\code.cmd",
            "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Programs\\Microsoft VS Code\\bin\\code.cmd"
        };
        
        for (String path : paths) {
            if (path != null && Files.exists(Path.of(path))) {
                logger.info("Found VS Code at: {}", path);
                return path;
            }
        }
        
        // Try to find code.cmd in PATH
        return findInPath("code.cmd");
    }
    
    private static String detectMacOSVSCode() {
        // Try common macOS installation paths
        String[] paths = {
            "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code",
            "/usr/local/bin/code",
            System.getProperty("user.home") + "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code"
        };
        
        for (String path : paths) {
            if (Files.exists(Path.of(path))) {
                logger.info("Found VS Code at: {}", path);
                return path;
            }
        }
        
        // Try to find code in PATH
        return findInPath("code");
    }
    
    private static String detectLinuxVSCode() {
        // Try common Linux installation paths
        String[] paths = {
            "/usr/bin/code",
            "/usr/local/bin/code",
            "/snap/bin/code",
            "/opt/visual-studio-code/bin/code"
        };
        
        for (String path : paths) {
            if (Files.exists(Path.of(path))) {
                logger.info("Found VS Code at: {}", path);
                return path;
            }
        }
        
        // Try to find code in PATH
        return findInPath("code");
    }
    
    /**
     * Finds an executable in the system PATH.
     */
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
                    logger.info("Found {} in PATH: {}", executable, result);
                    return result;
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.debug("Failed to find {} in PATH: {}", executable, e.getMessage());
        }
        
        logger.warn("VS Code executable not found");
        return null;
    }
    
    /**
     * Tests if VS Code executable is working by running --version command.
     */
    public static boolean isVSCodeValid(String vsCodePath) {
        if (vsCodePath == null || vsCodePath.isEmpty()) {
            return false;
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(vsCodePath, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            
            if (finished && process.exitValue() == 0) {
                String version = new String(process.getInputStream().readAllBytes()).trim();
                logger.info("VS Code validation successful. Version info: {}", version.split("\n")[0]);
                return true;
            } else {
                logger.warn("VS Code validation failed. Exit code: {}", 
                    finished ? process.exitValue() : "timeout");
                return false;
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("VS Code validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the display name for the current operating system.
     */
    public static String getOSDisplayName() {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        return osName + " (" + osArch + ")";
    }
}