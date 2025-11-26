package com.vscode.updater.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Window for displaying real-time command output and logs.
 */
public class LogViewerWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(LogViewerWindow.class);
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private JTextArea logTextArea;
    private JScrollPane scrollPane;
    private JButton clearButton;
    private JButton exportButton;
    private JButton closeButton;
    private JCheckBox autoScrollCheckbox;
    private JLabel statusLabel;
    
    private final List<String> logLines;
    private boolean autoScroll = true;
    
    public LogViewerWindow() {
        this.logLines = new ArrayList<>();
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupKeyboardShortcuts();
        
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setTitle("VS Code Extension Updater - Logs");
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Add welcome message  
        appendLog("=== VS Code Extension Updater - Log Viewer (Milestone 2) ===");
        appendLog("Multi-instance VS Code support enabled.");
        appendLog("Logs will show entries from all detected VS Code installations.");
        appendLog("Use 'Update Extensions' from the tray menu to see live logs.");
        appendLog("");
        
        logger.debug("Log viewer window initialized");
    }
    
    private void initializeComponents() {
        // Text area for log display
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logTextArea.setBackground(Color.BLACK);
        logTextArea.setForeground(Color.GREEN);
        logTextArea.setCaretColor(Color.GREEN);
        logTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Scroll pane
        scrollPane = new JScrollPane(logTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // Control buttons
        clearButton = new JButton("Clear");
        clearButton.setToolTipText("Clear log display (Ctrl+L)");
        
        exportButton = new JButton("Export...");
        exportButton.setToolTipText("Export logs to file (Ctrl+S)");
        
        closeButton = new JButton("Close");
        closeButton.setToolTipText("Close log viewer (Escape)");
        
        // Auto-scroll checkbox
        autoScrollCheckbox = new JCheckBox("Auto-scroll", autoScroll);
        autoScrollCheckbox.setToolTipText("Automatically scroll to bottom when new logs arrive");
        
        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(new EmptyBorder(2, 5, 2, 5));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main log display
        add(scrollPane, BorderLayout.CENTER);
        
        // Control panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(clearButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(autoScrollCheckbox);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(closeButton);
        
        controlPanel.add(buttonPanel, BorderLayout.CENTER);
        controlPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        clearButton.addActionListener(e -> clearLogs());
        exportButton.addActionListener(e -> exportLogs());
        closeButton.addActionListener(e -> setVisible(false));
        
        autoScrollCheckbox.addActionListener(e -> {
            autoScroll = autoScrollCheckbox.isSelected();
            if (autoScroll) {
                scrollToBottom();
            }
        });
    }
    
    private void setupKeyboardShortcuts() {
        // Ctrl+L to clear
        registerKeyboardAction(
            e -> clearLogs(),
            KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // Ctrl+S to export
        registerKeyboardAction(
            e -> exportLogs(),
            KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // Escape to close
        registerKeyboardAction(
            e -> setVisible(false),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
    
    private void registerKeyboardAction(ActionListener action, KeyStroke keyStroke, int condition) {
        getRootPane().registerKeyboardAction(action, keyStroke, condition);
    }
    
    /**
     * Appends a new log line to the display.
     */
    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logLines.add(message);
            logTextArea.append(message + "\n");
            
            if (autoScroll) {
                scrollToBottom();
            }
            
            updateStatusLabel();
        });
    }
    
    /**
     * Clears all logs from the display.
     */
    private void clearLogs() {
        logLines.clear();
        logTextArea.setText("");
        updateStatusLabel();
        logger.debug("Log display cleared");
    }
    
    /**
     * Exports logs to a file.
     */
    private void exportLogs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Logs");
        fileChooser.setSelectedFile(new java.io.File(
            "vscode-updater-logs_" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".txt"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            
            try (FileWriter writer = new FileWriter(file)) {
                for (String line : logLines) {
                    writer.write(line + System.lineSeparator());
                }
                
                statusLabel.setText("Logs exported to: " + file.getName());
                logger.info("Logs exported to: {}", file.getAbsolutePath());
                
                // Reset status after 3 seconds
                Timer timer = new Timer(3000, e -> updateStatusLabel());
                timer.setRepeats(false);
                timer.start();
                
            } catch (IOException e) {
                logger.error("Failed to export logs", e);
                JOptionPane.showMessageDialog(this,
                    "Failed to export logs:\n" + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Scrolls to the bottom of the log display.
     */
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }
    
    /**
     * Updates the status label with current log count.
     */
    private void updateStatusLabel() {
        statusLabel.setText(String.format("Lines: %d", logLines.size()));
    }
    
    /**
     * Shows the window and brings it to front.
     */
    public void showWindow() {
        SwingUtilities.invokeLater(() -> {
            logger.info("Making log viewer window visible");
            setVisible(true);
            toFront();
            repaint();
            
            // Force window to front on macOS
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                setExtendedState(JFrame.NORMAL);
                setAlwaysOnTop(true);
                setAlwaysOnTop(false);
            }
            
            requestFocus();
            logger.info("Log viewer window is now visible: {}", isVisible());
        });
    }
    
    /**
     * Checks if the window is currently visible.
     */
    public boolean isWindowVisible() {
        return isVisible();
    }
    
    /**
     * Main method for standalone testing of the log viewer.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Skip look and feel for now
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            
            LogViewerWindow logViewer = new LogViewerWindow();
            logViewer.appendLog("=== Standalone Log Viewer Test ===");
            logViewer.appendLog("This is a test to verify the log viewer window works correctly.");
            logViewer.appendLog("You should see this message in a terminal-style window.");
            logViewer.appendLog("Try the buttons: Clear, Export, and keyboard shortcuts.");
            logViewer.appendLog("");
            logViewer.showWindow();
            
            // Keep the application running
            logViewer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        });
    }
}