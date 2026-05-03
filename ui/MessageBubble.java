package ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Phase 2: Helper class to render chat message bubbles.
 * Provides custom rendering for sent, received, and system messages within a JTextPane.
 */
public class MessageBubble {

    public enum MessageType {
        SENT, RECEIVED, SYSTEM
    }

    public static void appendMessage(JTextPane pane, String sender, String text, MessageType type, String timestamp) {
        try {
            StyledDocument doc = pane.getStyledDocument();
            int length = doc.getLength();

            if (type == MessageType.SYSTEM) {
                // System messages are centered, gray, and italicized
                JPanel sysPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
                sysPanel.setOpaque(false);
                JLabel sysLabel = new JLabel("  \uD83D\uDC65 " + text + " \u2022 " + timestamp + "  ");
                sysLabel.setForeground(new Color(0x888888));
                sysLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                sysLabel.setBackground(new Color(0x2A2D3E));
                sysLabel.setOpaque(true);
                sysLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x3A3D4E), 1, true),
                        BorderFactory.createEmptyBorder(4, 10, 4, 10)
                ));
                sysPanel.add(sysLabel);

                pane.setCaretPosition(doc.getLength());
                pane.insertComponent(sysPanel);
                doc.insertString(doc.getLength(), "\n", null);
                
                SimpleAttributeSet center = new SimpleAttributeSet();
                StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
                StyleConstants.setSpaceAbove(center, 10);
                StyleConstants.setSpaceBelow(center, 10);
                doc.setParagraphAttributes(doc.getLength() - 1, 2, center, false);

            } else {
                JPanel bubblePanel = createBubblePanel(sender, text, type, timestamp);
                pane.setCaretPosition(doc.getLength());
                pane.insertComponent(bubblePanel);
                
                int newLen = doc.getLength();
                doc.insertString(newLen, "\n", null);
                
                SimpleAttributeSet leftAlign = new SimpleAttributeSet();
                StyleConstants.setAlignment(leftAlign, StyleConstants.ALIGN_LEFT);
                StyleConstants.setSpaceAbove(leftAlign, 5);
                StyleConstants.setSpaceBelow(leftAlign, 5);
                doc.setParagraphAttributes(newLen - 1, 2, leftAlign, false);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static JPanel createBubblePanel(String sender, String text, MessageType type, String timestamp) {
        boolean isSent = (type == MessageType.SENT);
        Color bgColor = new Color(0x212336); // Unified bubble color based on new UI description
        Color fgColor = Color.WHITE;

        // Container panel
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Header (Avatar + Sender + Timestamp)
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setOpaque(false);
        
        // Initials circle
        String initials = "U";
        String displaySender = sender;
        if (isSent) {
            initials = "ME";
            displaySender = "You";
        } else if (sender != null && sender.length() >= 2) {
            String[] parts = sender.split(" ");
            if (parts.length >= 2) {
                initials = (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            } else {
                initials = sender.substring(0, 2).toUpperCase();
            }
        }
        
        final String finalInitials = initials;
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSent ? new Color(0x7C6AFA) : new Color(0x4A4D6E));
                g2.fillOval(0, 0, 24, 24);
                
                // Online dot
                g2.setColor(new Color(0x4CAF50));
                g2.fillOval(16, 16, 8, 8);
                
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int x = (24 - fm.stringWidth(finalInitials)) / 2;
                int y = ((24 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(finalInitials, x, y);
            }
        };
        avatar.setPreferredSize(new Dimension(24, 24));
        avatar.setOpaque(false);
        
        JLabel nameLabel = new JLabel(displaySender);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        JLabel timeLabel = new JLabel(timestamp);
        timeLabel.setForeground(new Color(0x888888));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        headerPanel.add(avatar);
        if (!isSent) {
            headerPanel.add(nameLabel);
            headerPanel.add(timeLabel);
        } else {
            // For sent messages, image description says "14:30 You", so time first?
            headerPanel.add(timeLabel);
            headerPanel.add(nameLabel);
        }
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Bubble Panel
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
            }
        };
        bubble.setLayout(new BorderLayout());
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Code block styling for specific texts (e.g., /docs/auth.md)
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setOpaque(false);
        
        // Replace `code` with styled HTML
        String formattedText = text.replace("<", "&lt;").replace(">", "&gt;");
        formattedText = formattedText.replaceAll("(/docs/[a-zA-Z0-9_.-]+)", "<span style='background-color:#13151F; color:#CCCCCC; padding:2px 6px; border-radius:4px; font-family:monospace;'>$1</span>");
        
        String html = "<html><body style='color:white; font-family:\"Segoe UI\", sans-serif; font-size:13px; margin:0; padding:0;'>" + formattedText + "</body></html>";
        textPane.setText(html);
        
        bubble.add(textPane, BorderLayout.CENTER);

        // Build the layout
        container.add(headerPanel);
        container.add(Box.createVerticalStrut(4));
        
        // Wrap bubble in a panel to limit width
        JPanel bubbleWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 0)); // Indent bubble
        bubbleWrapper.setOpaque(false);
        bubbleWrapper.add(bubble);
        bubbleWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(bubbleWrapper);
        
        // Delivered indicator for sent messages
        if (isSent) {
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            statusPanel.setOpaque(false);
            JLabel statusLabel = new JLabel("\u2714\u2714 Delivered");
            statusLabel.setForeground(new Color(0xAAAAAA));
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            
            // Limit width to align with bubble
            int textWidth = Math.min(600, text.length() * 8 + 32); 
            statusPanel.setPreferredSize(new Dimension(textWidth, 20));
            
            JPanel statusWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 0));
            statusWrapper.setOpaque(false);
            statusWrapper.add(statusPanel);
            statusWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(statusWrapper);
        }

        return container;
    }
}
