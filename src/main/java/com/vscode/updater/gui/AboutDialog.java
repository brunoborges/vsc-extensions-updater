package com.vscode.updater.gui;

import com.vscode.updater.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * About dialog for the VS Code Extension Updater application.
 */
public class AboutDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(AboutDialog.class);
    
    public AboutDialog(Frame parent) {
        super(parent, "About " + AppInfo.getName(), true);
        initializeDialog();
    }
    
    public AboutDialog() {
        super((Frame) null, "About " + AppInfo.getName(), true);
        initializeDialog();
    }
    
    private void initializeDialog() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // App icon and title
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        // Create a simple icon
        JLabel iconLabel = new JLabel(createAppIcon());
        JLabel titleLabel = new JLabel(AppInfo.getName());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        
        headerPanel.add(iconLabel);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(titleLabel);
        
        // App information
        JTextArea infoArea = new JTextArea(AppInfo.getFormattedInfo());
        infoArea.setEditable(false);
        infoArea.setOpaque(false);
        infoArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        infoArea.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(80, 30));
        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        
        // Layout
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Keyboard shortcut
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // Set default button
        getRootPane().setDefaultButton(okButton);
        
        pack();
        setLocationRelativeTo(getParent());
        
        logger.debug("About dialog initialized");
    }
    
    private ImageIcon createAppIcon() {
        try {
            // Load the VSCode Extension Updater logo from resources
            InputStream imageStream = getClass().getResourceAsStream("/vsc-updater-logo.png");
            if (imageStream != null) {
                BufferedImage originalImage = ImageIO.read(imageStream);
                imageStream.close();
                
                // Scale to 32x32 for the about dialog
                int size = 32;
                BufferedImage scaledImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaledImage.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(originalImage, 0, 0, size, size, null);
                g.dispose();
                
                return new ImageIcon(scaledImage);
            }
        } catch (IOException e) {
            logger.warn("Could not load application logo, falling back to programmatic icon", e);
        }
        
        // Fallback to programmatic icon if logo can't be loaded
        return createFallbackIcon();
    }
    
    private ImageIcon createFallbackIcon() {
        // Create a simple 32x32 fallback icon
        int size = 32;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        
        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background circle
        g.setColor(new Color(0, 150, 255));
        g.fillOval(2, 2, size - 4, size - 4);
        
        // Draw "VS" text
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        String text = "VS";
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        int x = (size - textWidth) / 2;
        int y = (size + textHeight) / 2 - 2;
        g.drawString(text, x, y);
        
        g.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Shows the About dialog.
     */
    public static void showAbout(Component parent) {
        SwingUtilities.invokeLater(() -> {
            try {
                AboutDialog dialog = new AboutDialog();
                dialog.setVisible(true);
            } catch (Exception e) {
                logger.error("Failed to show About dialog", e);
                // Fallback to simple message dialog
                JOptionPane.showMessageDialog(parent, AppInfo.getFormattedInfo(), 
                    "About " + AppInfo.getName(), JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
    
    /**
     * Main method for standalone testing of the About dialog.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set system properties for better GUI experience
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            
            AboutDialog dialog = new AboutDialog();
            dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            dialog.setVisible(true);
        });
    }
}