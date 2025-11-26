package com.vscode.updater.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Captures output from a process InputStream in real-time.
 * Implements Runnable for use with Virtual Threads.
 */
public class OutputStreamCapture implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(OutputStreamCapture.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final InputStream inputStream;
    private final Consumer<String> outputConsumer;
    private final List<String> outputLines;
    
    public OutputStreamCapture(InputStream inputStream, Consumer<String> outputConsumer) {
        this.inputStream = inputStream;
        this.outputConsumer = outputConsumer;
        this.outputLines = new CopyOnWriteArrayList<>();
    }
    
    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                String timestamp = LocalDateTime.now().format(TIME_FORMAT);
                String formattedLine = String.format("[%s] %s", timestamp, line);
                
                // Store the original line for later processing
                outputLines.add(line);
                
                // Send formatted line to consumer for real-time display
                outputConsumer.accept(formattedLine);
                
                // Log to application logs as well
                logger.debug("VS Code output: {}", line);
            }
            
        } catch (IOException e) {
            String errorMessage = String.format("[%s] ERROR: Failed to read command output: %s", 
                LocalDateTime.now().format(TIME_FORMAT), e.getMessage());
            outputConsumer.accept(errorMessage);
            logger.error("Failed to capture command output", e);
        }
    }
    
    /**
     * Gets all captured output lines (without timestamps).
     */
    public List<String> getOutputLines() {
        return new ArrayList<>(outputLines);
    }
    
    /**
     * Gets the number of lines captured so far.
     */
    public int getLineCount() {
        return outputLines.size();
    }
    
    /**
     * Checks if any output has been captured.
     */
    public boolean hasOutput() {
        return !outputLines.isEmpty();
    }
}