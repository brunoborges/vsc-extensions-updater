package com.vscode.updater.tray;

import com.vscode.updater.discovery.VSCodeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;

/**
 * Builds dynamic tray menus for multi-instance VS Code support.
 */
public class MultiInstanceMenuBuilder {
    private static final Logger logger = LoggerFactory.getLogger(MultiInstanceMenuBuilder.class);
    
    /**
     * Interface for handling menu actions.
     */
    public interface MenuActionHandler {
        void onUpdateInstance(VSCodeInstance instance);
        void onUpdateAndOpenInstance(VSCodeInstance instance);
        void onToggleInstance(VSCodeInstance instance);
        void onRefreshDetection();
        void onViewLogs();
        void onShowAbout();
        void onExit();
        
        // Milestone 3: Scheduling actions
        void onShowSettings();
        void onToggleScheduler();
        void onRunScheduledUpdate();
    }
    
    /**
     * Builds a dynamic popup menu based on detected VS Code instances.
     */
    public static PopupMenu buildMenu(List<VSCodeInstance> instances, 
                                    String lastUpdateSummary,
                                    String schedulerStatus,
                                    boolean schedulerRunning,
                                    MenuActionHandler handler) {
        PopupMenu menu = new PopupMenu();
        
        // Header with status
        MenuItem statusItem = new MenuItem("● VS Code Extension Updater");
        statusItem.setEnabled(false);
        menu.add(statusItem);
        
        menu.addSeparator();
        
        if (instances.isEmpty()) {
            // No instances detected
            MenuItem noInstancesItem = new MenuItem("No VS Code installations detected");
            noInstancesItem.setEnabled(false);
            menu.add(noInstancesItem);
            
            MenuItem refreshItem = new MenuItem("🔄 Refresh Detection");
            refreshItem.addActionListener(e -> handler.onRefreshDetection());
            menu.add(refreshItem);
        } else {
            // Add global update action if multiple instances
            if (instances.size() > 1) {
                MenuItem updateAllItem = new MenuItem("🚀 Update All Extensions");
                updateAllItem.addActionListener(e -> {
                    // Update all enabled instances
                    instances.stream()
                        .filter(VSCodeInstance::enabled)
                        .forEach(handler::onUpdateInstance);
                });
                menu.add(updateAllItem);
                menu.addSeparator();
            }
            
            // Add menu items for each instance
            for (VSCodeInstance instance : instances) {
                Menu instanceMenu = createInstanceMenu(instance, handler);
                menu.add(instanceMenu);
            }
            
            menu.addSeparator();
            
            // Refresh detection
            MenuItem refreshItem = new MenuItem("🔄 Refresh Detection");
            refreshItem.addActionListener(e -> handler.onRefreshDetection());
            menu.add(refreshItem);
        }
        
        menu.addSeparator();
        
        // Last update summary
        if (lastUpdateSummary != null && !lastUpdateSummary.isEmpty()) {
            MenuItem summaryItem = new MenuItem(lastUpdateSummary);
            summaryItem.setEnabled(false);
            menu.add(summaryItem);
            menu.addSeparator();
        }
        
        // Standard menu items
        MenuItem logsItem = new MenuItem("📋 View Logs...");
        logsItem.addActionListener(e -> handler.onViewLogs());
        menu.add(logsItem);
        
        MenuItem settingsItem = new MenuItem("⚙️ Settings...");
        settingsItem.addActionListener(e -> handler.onShowSettings());
        menu.add(settingsItem);
        
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.addActionListener(e -> handler.onShowAbout());
        menu.add(aboutItem);
        
        menu.addSeparator();
        
        MenuItem exitItem = new MenuItem("Quit");
        exitItem.addActionListener(e -> handler.onExit());
        menu.add(exitItem);
        
        logger.debug("Built menu with {} VS Code instance(s)", instances.size());
        return menu;
    }
    
    private static Menu createInstanceMenu(VSCodeInstance instance, MenuActionHandler handler) {
        String menuTitle = String.format("%s %s %s", 
            instance.enabled() ? "✅" : "❌",
            instance.edition().getDisplayName(),
            instance.version()
        );
        
        Menu instanceMenu = new Menu(menuTitle);
        
        // Update action
        MenuItem updateItem = new MenuItem("🚀 Update Extensions");
        updateItem.setEnabled(instance.enabled());
        updateItem.addActionListener(e -> handler.onUpdateInstance(instance));
        instanceMenu.add(updateItem);
        
        // Update and Open action
        MenuItem updateAndOpenItem = new MenuItem("🚀🗂️ Update and Open VS Code");
        updateAndOpenItem.setEnabled(instance.enabled());
        updateAndOpenItem.addActionListener(e -> handler.onUpdateAndOpenInstance(instance));
        instanceMenu.add(updateAndOpenItem);
        
        instanceMenu.addSeparator();
        
        // Toggle enabled/disabled
        MenuItem toggleItem = new MenuItem(instance.enabled() ? "❌ Disable" : "✅ Enable");
        toggleItem.addActionListener(e -> handler.onToggleInstance(instance));
        instanceMenu.add(toggleItem);
        
        instanceMenu.addSeparator();
        
        // Instance info
        MenuItem pathItem = new MenuItem("📁 " + instance.getShortPath());
        pathItem.setEnabled(false);
        instanceMenu.add(pathItem);
        
        MenuItem lastUpdateItem = new MenuItem("🕒 Last: " + instance.lastUpdateTime());
        lastUpdateItem.setEnabled(false);
        instanceMenu.add(lastUpdateItem);
        
        MenuItem statusItem = new MenuItem("📊 " + instance.lastUpdateStatus());
        statusItem.setEnabled(false);
        instanceMenu.add(statusItem);
        
        return instanceMenu;
    }
    
    /**
     * Creates a simple menu item with an icon and text.
     */
    public static MenuItem createMenuItem(String icon, String text, Runnable action) {
        MenuItem item = new MenuItem(icon + " " + text);
        if (action != null) {
            item.addActionListener(e -> action.run());
        }
        return item;
    }
    
    /**
     * Estimates the optimal menu layout based on number of instances.
     */
    public static MenuLayout calculateOptimalLayout(int instanceCount) {
        if (instanceCount == 0) {
            return MenuLayout.SIMPLE;
        } else if (instanceCount == 1) {
            return MenuLayout.SINGLE_INSTANCE;
        } else if (instanceCount <= 3) {
            return MenuLayout.MULTI_INSTANCE_FLAT;
        } else {
            return MenuLayout.MULTI_INSTANCE_GROUPED;
        }
    }
    
    public enum MenuLayout {
        SIMPLE,                    // No instances detected
        SINGLE_INSTANCE,          // Single instance, simple layout
        MULTI_INSTANCE_FLAT,      // 2-3 instances, flat menu structure
        MULTI_INSTANCE_GROUPED    // 4+ instances, grouped with submenus
    }
}