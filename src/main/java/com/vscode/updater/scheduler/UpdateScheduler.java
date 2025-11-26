package com.vscode.updater.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vscode.updater.config.VSCodeConfig;
import com.vscode.updater.discovery.VSCodeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages automatic scheduling of VS Code extension updates.
 * Milestone 3: Scheduling and Automation implementation.
 */
public class UpdateScheduler {
    private static final Logger logger = LoggerFactory.getLogger(UpdateScheduler.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final ScheduledExecutorService scheduler;
    private final Consumer<VSCodeInstance> updateCallback;
    private final Consumer<String> statusCallback;
    
    private ScheduledFuture<?> currentSchedule;
    private VSCodeConfig config;
    private boolean isRunning = false;
    private LocalDateTime lastUpdateTime;
    private LocalDateTime nextUpdateTime;
    
    /**
     * Interface for scheduling configuration.
     */
    public record ScheduleConfig(
        boolean enabled,
        int intervalMinutes,
        ScheduleType type,
        String customCron,
        boolean updateOnStartup,
        boolean skipIfRecentlyUpdated,
        int recentlyUpdatedThresholdMinutes,
        boolean onlyWhenIdle,
        int maxConcurrentUpdates
    ) {
        public enum ScheduleType {
            MINUTES("Every X minutes"),
            HOURLY("Every X hours"), 
            DAILY("Daily at specified time"),
            CUSTOM_CRON("Custom cron expression");
            
            private final String description;
            
            ScheduleType(String description) {
                this.description = description;
            }
            
            public String getDescription() { return description; }
        }
        
        public static ScheduleConfig createDefault() {
            return new ScheduleConfig(
                false,        // disabled by default
                60,           // every 60 minutes
                ScheduleType.MINUTES,
                "",           // no custom cron
                false,        // don't update on startup
                true,         // skip if recently updated
                30,           // within last 30 minutes
                true,         // only when idle
                2             // max 2 concurrent updates
            );
        }
        
        public String validate() {
            if (intervalMinutes <= 0) {
                return "Interval must be positive";
            }
            if (intervalMinutes > 10080) { // 7 days
                return "Interval cannot exceed 7 days";
            }
            if (recentlyUpdatedThresholdMinutes < 0) {
                return "Recently updated threshold cannot be negative";
            }
            if (maxConcurrentUpdates <= 0 || maxConcurrentUpdates > 10) {
                return "Concurrent updates must be between 1 and 10";
            }
            if (type == ScheduleType.CUSTOM_CRON && (customCron == null || customCron.trim().isEmpty())) {
                return "Custom cron expression is required when using custom cron type";
            }
            return null;
        }
        
        @JsonIgnore
        public Duration getIntervalDuration() {
            return switch (type) {
                case MINUTES -> Duration.ofMinutes(intervalMinutes);
                case HOURLY -> Duration.ofHours(intervalMinutes);
                case DAILY -> Duration.ofDays(1); // Fixed daily interval
                case CUSTOM_CRON -> Duration.ofMinutes(intervalMinutes); // Fallback
            };
        }
        
        @JsonIgnore
        public String getDisplaySchedule() {
            return switch (type) {
                case MINUTES -> String.format("Every %d minute%s", intervalMinutes, intervalMinutes == 1 ? "" : "s");
                case HOURLY -> String.format("Every %d hour%s", intervalMinutes, intervalMinutes == 1 ? "" : "s");
                case DAILY -> "Daily";
                case CUSTOM_CRON -> "Custom: " + customCron;
            };
        }
    }
    
    public UpdateScheduler(Consumer<VSCodeInstance> updateCallback, 
                          Consumer<String> statusCallback) {
        this.updateCallback = updateCallback;
        this.statusCallback = statusCallback;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "UpdateScheduler");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("Update scheduler initialized");
    }
    
    /**
     * Starts the scheduler with the given configuration.
     */
    public void start(VSCodeConfig config) {
        this.config = config;
        ScheduleConfig scheduleConfig = config.schedule();
        
        if (!scheduleConfig.enabled()) {
            logger.info("Update scheduling is disabled");
            updateStatus("Scheduling disabled");
            return;
        }
        
        stop(); // Stop any existing schedule
        
        long delayMinutes = scheduleConfig.getIntervalDuration().toMinutes();
        logger.info("Starting update scheduler: {}", scheduleConfig.getDisplaySchedule());
        
        // Schedule the periodic updates
        currentSchedule = scheduler.scheduleAtFixedRate(
            this::performScheduledUpdate,
            scheduleConfig.updateOnStartup() ? 1 : delayMinutes, // Initial delay
            delayMinutes,
            TimeUnit.MINUTES
        );
        
        isRunning = true;
        calculateNextUpdateTime();
        updateStatus("Scheduled: " + scheduleConfig.getDisplaySchedule());
        
        logger.info("Update scheduler started successfully. Next update at: {}", 
            nextUpdateTime != null ? nextUpdateTime.format(TIME_FORMAT) : "unknown");
    }
    
    /**
     * Stops the scheduler.
     */
    public void stop() {
        if (currentSchedule != null) {
            currentSchedule.cancel(false);
            currentSchedule = null;
        }
        isRunning = false;
        nextUpdateTime = null;
        updateStatus("Stopped");
        logger.info("Update scheduler stopped");
    }
    
    /**
     * Performs a scheduled update cycle.
     */
    private void performScheduledUpdate() {
        try {
            logger.info("Starting scheduled update cycle");
            updateStatus("Running scheduled update...");
            
            if (config == null) {
                logger.warn("No configuration available for scheduled update");
                return;
            }
            
            ScheduleConfig scheduleConfig = config.schedule();
            
            // Check if we should skip due to recent updates
            if (scheduleConfig.skipIfRecentlyUpdated() && wasRecentlyUpdated(scheduleConfig)) {
                logger.info("Skipping scheduled update - recently updated within {} minutes", 
                    scheduleConfig.recentlyUpdatedThresholdMinutes());
                updateStatus("Skipped - recently updated");
                calculateNextUpdateTime();
                return;
            }
            
            // Check system idle state if required
            if (scheduleConfig.onlyWhenIdle() && !isSystemIdle()) {
                logger.info("Skipping scheduled update - system is not idle");
                updateStatus("Skipped - system busy");
                calculateNextUpdateTime();
                return;
            }
            
            // Get enabled instances
            var enabledInstances = config.getEnabledInstances();
            if (enabledInstances.isEmpty()) {
                logger.info("No enabled VS Code instances for scheduled update");
                updateStatus("Skipped - no enabled instances");
                calculateNextUpdateTime();
                return;
            }
            
            lastUpdateTime = LocalDateTime.now();
            
            // Perform updates with concurrency limit
            int maxConcurrent = Math.min(scheduleConfig.maxConcurrentUpdates(), enabledInstances.size());
            logger.info("Updating {} enabled instance(s) with max concurrency {}", 
                enabledInstances.size(), maxConcurrent);
            
            // Simple sequential updates for now (can be enhanced to true concurrency)
            int updated = 0;
            for (VSCodeInstance instance : enabledInstances) {
                if (updated >= maxConcurrent) {
                    logger.info("Reached concurrent update limit, queueing remaining updates");
                    break;
                }
                
                try {
                    logger.info("Scheduled update for: {}", instance.displayName());
                    updateCallback.accept(instance);
                    updated++;
                } catch (Exception e) {
                    logger.error("Failed to update {} during scheduled cycle: {}", 
                        instance.displayName(), e.getMessage());
                }
            }
            
            updateStatus(String.format("Updated %d instance(s)", updated));
            calculateNextUpdateTime();
            
            logger.info("Scheduled update cycle completed. Updated {} instance(s)", updated);
            
        } catch (Exception e) {
            logger.error("Error during scheduled update cycle", e);
            updateStatus("Error: " + e.getMessage());
            calculateNextUpdateTime();
        }
    }
    
    private boolean wasRecentlyUpdated(ScheduleConfig scheduleConfig) {
        if (lastUpdateTime == null) {
            return false;
        }
        
        Duration timeSinceUpdate = Duration.between(lastUpdateTime, LocalDateTime.now());
        Duration threshold = Duration.ofMinutes(scheduleConfig.recentlyUpdatedThresholdMinutes());
        
        return timeSinceUpdate.compareTo(threshold) < 0;
    }
    
    private boolean isSystemIdle() {
        // Simplified idle detection - can be enhanced with platform-specific checks
        try {
            // Check system load, active processes, etc.
            // For now, always return true (assume system is idle)
            // TODO: Implement platform-specific idle detection
            return true;
        } catch (Exception e) {
            logger.debug("Could not determine system idle state: {}", e.getMessage());
            return true; // Default to allowing updates
        }
    }
    
    private void calculateNextUpdateTime() {
        if (config == null || !config.schedule().enabled() || !isRunning) {
            nextUpdateTime = null;
            return;
        }
        
        Duration interval = config.schedule().getIntervalDuration();
        nextUpdateTime = LocalDateTime.now().plus(interval);
    }
    
    private void updateStatus(String status) {
        if (statusCallback != null) {
            statusCallback.accept(status);
        }
    }
    
    /**
     * Gets the current scheduler status.
     */
    public SchedulerStatus getStatus() {
        return new SchedulerStatus(
            isRunning,
            config != null ? config.schedule() : null,
            lastUpdateTime,
            nextUpdateTime,
            currentSchedule != null
        );
    }
    
    /**
     * Triggers an immediate update cycle (manual trigger).
     */
    public void triggerImmediateUpdate() {
        logger.info("Triggering immediate update cycle");
        scheduler.execute(this::performScheduledUpdate);
    }
    
    /**
     * Shuts down the scheduler completely.
     */
    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate gracefully, forcing shutdown");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
        logger.info("Update scheduler shut down");
    }
    
    /**
     * Status information for the scheduler.
     */
    public record SchedulerStatus(
        boolean isRunning,
        ScheduleConfig config,
        LocalDateTime lastUpdateTime,
        LocalDateTime nextUpdateTime,
        boolean hasActiveSchedule
    ) {
        public String getStatusSummary() {
            if (!isRunning) {
                return "Stopped";
            }
            if (config == null || !config.enabled()) {
                return "Disabled";
            }
            if (nextUpdateTime == null) {
                return "Scheduled";
            }
            return "Next: " + nextUpdateTime.format(TIME_FORMAT);
        }
    }
}