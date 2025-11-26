package com.vscode.updater.logging;

import com.vscode.updater.discovery.VSCodeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Enhanced log manager for Milestone 2 with multi-instance session support.
 */
public class LogManager {
    private static final Logger logger = LoggerFactory.getLogger(LogManager.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final Map<String, LogSession> sessions = new ConcurrentHashMap<>();
    private final List<Consumer<LogEntry>> globalLogConsumers = new CopyOnWriteArrayList<>();
    
    /**
     * Log entry record containing session and instance information.
     */
    public record LogEntry(
        String sessionId,
        String instanceId,
        LocalDateTime timestamp,
        LogLevel level,
        String message,
        String rawMessage
    ) {
        public String getFormattedMessage() {
            return String.format("[%s] [%s] %s: %s", 
                timestamp.format(TIME_FORMAT),
                instanceId,
                level.name(),
                message
            );
        }
    }
    
    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG
    }
    
    /**
     * Represents a logging session for a specific VS Code instance.
     */
    public static class LogSession {
        private final String sessionId;
        private final VSCodeInstance instance;
        private final List<LogEntry> entries = new CopyOnWriteArrayList<>();
        private final List<Consumer<LogEntry>> consumers = new CopyOnWriteArrayList<>();
        private final LocalDateTime startTime;
        
        public LogSession(String sessionId, VSCodeInstance instance) {
            this.sessionId = sessionId;
            this.instance = instance;
            this.startTime = LocalDateTime.now();
        }
        
        public void addEntry(LogLevel level, String message, String rawMessage) {
            LogEntry entry = new LogEntry(
                sessionId,
                instance.edition().getDisplayName(),
                LocalDateTime.now(),
                level,
                message,
                rawMessage != null ? rawMessage : message
            );
            
            entries.add(entry);
            
            // Notify session-specific consumers
            consumers.forEach(consumer -> {
                try {
                    consumer.accept(entry);
                } catch (Exception e) {
                    logger.error("Error in log consumer", e);
                }
            });
        }
        
        public void addConsumer(Consumer<LogEntry> consumer) {
            consumers.add(consumer);
        }
        
        public void removeConsumer(Consumer<LogEntry> consumer) {
            consumers.remove(consumer);
        }
        
        public List<LogEntry> getEntries() {
            return new ArrayList<>(entries);
        }
        
        public VSCodeInstance getInstance() { return instance; }
        public String getSessionId() { return sessionId; }
        public LocalDateTime getStartTime() { return startTime; }
        
        public void clear() {
            entries.clear();
        }
    }
    
    /**
     * Creates a new logging session for a VS Code instance.
     */
    public LogSession createSession(VSCodeInstance instance) {
        String sessionId = generateSessionId(instance);
        LogSession session = new LogSession(sessionId, instance);
        sessions.put(sessionId, session);
        
        // Add global log consumer that forwards to global consumers
        session.addConsumer(entry -> {
            globalLogConsumers.forEach(consumer -> {
                try {
                    consumer.accept(entry);
                } catch (Exception e) {
                    logger.error("Error in global log consumer", e);
                }
            });
        });
        
        logger.debug("Created log session {} for {}", sessionId, instance.displayName());
        return session;
    }
    
    /**
     * Gets an existing session by ID.
     */
    public LogSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * Gets all active sessions.
     */
    public List<LogSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }
    
    /**
     * Removes a session.
     */
    public void removeSession(String sessionId) {
        LogSession removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.debug("Removed log session {}", sessionId);
        }
    }
    
    /**
     * Adds a global log consumer that receives entries from all sessions.
     */
    public void addGlobalConsumer(Consumer<LogEntry> consumer) {
        globalLogConsumers.add(consumer);
    }
    
    /**
     * Removes a global log consumer.
     */
    public void removeGlobalConsumer(Consumer<LogEntry> consumer) {
        globalLogConsumers.remove(consumer);
    }
    
    /**
     * Gets all log entries across all sessions.
     */
    public List<LogEntry> getAllEntries() {
        return sessions.values().stream()
            .flatMap(session -> session.getEntries().stream())
            .sorted((e1, e2) -> e1.timestamp().compareTo(e2.timestamp()))
            .toList();
    }
    
    /**
     * Clears all sessions and entries.
     */
    public void clearAll() {
        sessions.values().forEach(LogSession::clear);
        logger.debug("Cleared all log sessions");
    }
    
    private String generateSessionId(VSCodeInstance instance) {
        return String.format("%s_%s_%d", 
            instance.edition().name().toLowerCase(),
            instance.version().replace(".", "_"),
            System.currentTimeMillis()
        );
    }
}