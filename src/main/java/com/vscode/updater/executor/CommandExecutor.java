package com.vscode.updater.executor;

import com.vscode.updater.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Executes VS Code commands and captures their output in real-time.
 * Uses Virtual Threads for non-blocking execution.
 */
public class CommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * Result of command execution.
     */
    public record ExecutionResult(
        boolean success,
        int exitCode,
        Duration duration,
        String summary,
        List<String> outputLines
    ) {}
    
    /**
     * Executes VS Code extension update command asynchronously.
     * 
     * @param vsCodePath Path to VS Code executable
     * @param timeoutSeconds Maximum execution time in seconds
     * @param outputConsumer Consumer for real-time output lines
     * @return CompletableFuture with execution result
     */
    public static CompletableFuture<ExecutionResult> updateExtensionsAsync(
            String vsCodePath, 
            int timeoutSeconds,
            Consumer<String> outputConsumer) {
        
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime startTime = LocalDateTime.now();
            String timeStamp = startTime.format(TIME_FORMAT);
            
            // Validate VS Code path first
            if (!ProcessUtils.isVSCodeValid(vsCodePath)) {
                String error = String.format("[%s] ERROR: VS Code executable not found or invalid: %s", 
                    timeStamp, vsCodePath);
                outputConsumer.accept(error);
                logger.error("VS Code validation failed: {}", vsCodePath);
                return new ExecutionResult(false, -1, Duration.ZERO, 
                    "VS Code executable validation failed", List.of(error));
            }
            
            try {
                // Log execution start
                String startMessage = String.format("[%s] INFO: Starting VS Code extension update...", timeStamp);
                outputConsumer.accept(startMessage);
                logger.info("Starting extension update command: {}", vsCodePath);
                
                String commandMessage = String.format("[%s] INFO: Executing: %s --update-extensions", 
                    timeStamp, vsCodePath);
                outputConsumer.accept(commandMessage);
                
                // Create and start process
                ProcessBuilder processBuilder = new ProcessBuilder(vsCodePath, "--update-extensions");
                processBuilder.redirectErrorStream(true); // Merge stderr with stdout
                
                Process process = processBuilder.start();
                
                // Capture output in real-time using Virtual Thread
                OutputStreamCapture capture = new OutputStreamCapture(
                    process.getInputStream(), 
                    outputConsumer
                );
                
                Thread.startVirtualThread(capture);
                
                // Wait for process completion with timeout
                boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                
                LocalDateTime endTime = LocalDateTime.now();
                Duration duration = Duration.between(startTime, endTime);
                
                if (!completed) {
                    process.destroyForcibly();
                    String timeoutMessage = String.format("[%s] ERROR: Command timed out after %d seconds", 
                        endTime.format(TIME_FORMAT), timeoutSeconds);
                    outputConsumer.accept(timeoutMessage);
                    logger.warn("Command execution timed out after {} seconds", timeoutSeconds);
                    
                    return new ExecutionResult(false, -1, duration, 
                        "Command execution timed out", capture.getOutputLines());
                }
                
                int exitCode = process.exitValue();
                boolean success = exitCode == 0;
                
                String completionMessage = String.format("[%s] %s: Command completed with exit code: %d (duration: %d seconds)", 
                    endTime.format(TIME_FORMAT),
                    success ? "INFO" : "ERROR",
                    exitCode,
                    duration.getSeconds());
                outputConsumer.accept(completionMessage);
                
                if (success) {
                    logger.info("Extension update completed successfully in {} seconds", duration.getSeconds());
                } else {
                    logger.warn("Extension update failed with exit code: {}", exitCode);
                }
                
                String summary = String.format("Extension update %s in %d seconds", 
                    success ? "completed successfully" : "failed", duration.getSeconds());
                
                return new ExecutionResult(success, exitCode, duration, summary, capture.getOutputLines());
                
            } catch (IOException e) {
                String errorMessage = String.format("[%s] ERROR: Failed to start command: %s", 
                    LocalDateTime.now().format(TIME_FORMAT), e.getMessage());
                outputConsumer.accept(errorMessage);
                logger.error("Failed to execute command", e);
                
                return new ExecutionResult(false, -1, Duration.between(startTime, LocalDateTime.now()), 
                    "Failed to start command: " + e.getMessage(), List.of(errorMessage));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                String interruptMessage = String.format("[%s] ERROR: Command execution interrupted", 
                    LocalDateTime.now().format(TIME_FORMAT));
                outputConsumer.accept(interruptMessage);
                logger.warn("Command execution interrupted", e);
                
                return new ExecutionResult(false, -1, Duration.between(startTime, LocalDateTime.now()), 
                    "Command execution interrupted", List.of(interruptMessage));
            }
        });
    }
    
    /**
     * Executes command synchronously with default timeout.
     */
    public static ExecutionResult updateExtensions(String vsCodePath, Consumer<String> outputConsumer) {
        try {
            return updateExtensionsAsync(vsCodePath, 300, outputConsumer).get();
        } catch (Exception e) {
            logger.error("Synchronous command execution failed", e);
            String errorMessage = "Execution failed: " + e.getMessage();
            outputConsumer.accept(errorMessage);
            return new ExecutionResult(false, -1, Duration.ZERO, errorMessage, List.of(errorMessage));
        }
    }
    
    /**
     * Launches VS Code instance asynchronously.
     * 
     * @param vsCodePath Path to VS Code executable
     * @return CompletableFuture that completes when VS Code is launched
     */
    public static CompletableFuture<Boolean> launchVSCodeAsync(String vsCodePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Launching VS Code: {}", vsCodePath);
                
                // Launch VS Code without waiting for it to complete
                ProcessBuilder pb = new ProcessBuilder(vsCodePath);
                pb.start();
                
                logger.info("VS Code launched successfully");
                return true;
                
            } catch (IOException e) {
                logger.error("Failed to launch VS Code: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Updates extensions and then launches VS Code.
     * 
     * @param vsCodePath Path to VS Code executable
     * @param timeoutSeconds Timeout for update command
     * @param outputConsumer Consumer for real-time output lines
     * @return CompletableFuture with update result and launch status
     */
    public static CompletableFuture<UpdateAndLaunchResult> updateAndLaunchAsync(
            String vsCodePath, 
            int timeoutSeconds,
            Consumer<String> outputConsumer) {
        
        return updateExtensionsAsync(vsCodePath, timeoutSeconds, outputConsumer)
            .thenCompose(updateResult -> {
                if (updateResult.success()) {
                    outputConsumer.accept(String.format("[%s] INFO: Extensions updated successfully, launching VS Code...", 
                        LocalDateTime.now().format(TIME_FORMAT)));
                    
                    return launchVSCodeAsync(vsCodePath)
                        .thenApply(launchSuccess -> new UpdateAndLaunchResult(updateResult, launchSuccess));
                } else {
                    outputConsumer.accept(String.format("[%s] ERROR: Extension update failed, skipping VS Code launch", 
                        LocalDateTime.now().format(TIME_FORMAT)));
                    
                    return CompletableFuture.completedFuture(new UpdateAndLaunchResult(updateResult, false));
                }
            });
    }
    
    /**
     * Result of update and launch operation.
     */
    public record UpdateAndLaunchResult(
        ExecutionResult updateResult,
        boolean launchSuccess
    ) {
        public boolean success() {
            return updateResult.success() && launchSuccess;
        }
        
        public String summary() {
            if (success()) {
                return updateResult.summary() + " and VS Code launched";
            } else if (updateResult.success()) {
                return updateResult.summary() + " but failed to launch VS Code";
            } else {
                return updateResult.summary();
            }
        }
    }
}