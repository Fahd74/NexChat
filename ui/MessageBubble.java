package ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Helper class for rendering chat message bubbles inside a JTextPane.
 * Provides custom rendering for three message types:
 * <ul>
 *   <li>SENT — user's own messages, right-aligned with "Delivered" indicator</li>
 *   <li>RECEIVED — other users' messages, left-aligned with avatar</li>
 *   <li>SYSTEM — centered, gray event notifications (joins/leaves)</li>
 * </ul>
 */
public class MessageBubble {

    /** Size of the avatar circle in pixels. */
    private static final int AVATAR_SIZE = 24;

    /** Background color for message bubbles. */
    private static final Color BUBBLE_BG = new Color(0x212336);

    /** Color for the "sent" avatar circle. */
    private static final Color SENT_AVATAR_COLOR = new Color(0x7C6AFA);

    /** Color for the "received" avatar circle. */
    private static final Color RECEIVED_AVATAR_COLOR = new Color(0x4A4D6E);

    /** Color for the online status dot. */
    private static final Color ONLINE_DOT_COLOR = new Color(0x4CAF50);

    /** Color for timestamp and secondary text. */
    private static final Color SECONDARY_TEXT_COLOR = new Color(0x888888);

    /** The type of message to render. */
    public enum MessageType {
        SENT, RECEIVED, SYSTEM
    }

    /**
     * Appends a message to the provided JTextPane with styling based on the type.
     *
     * @param pane      the JTextPane to append the message to
     * @param sender    the name of the sender
     * @param text      the message text
     * @param type      the type of message (SENT, RECEIVED, SYSTEM)
     * @param timestamp the time the message was sent/received (HH:mm)
     */
    public static void appendMessage(JTextPane pane, String sender, String text, MessageType type, String timestamp) {
        try {
            StyledDocument doc = pane.getStyledDocument();

            if (type == MessageType.SYSTEM) {
                appendSystemMessage(pane, doc, text, timestamp);
            } else {
                appendChatBubble(pane, doc, sender, text, type, timestamp);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Appends a centered system notification (e.g., "Alice joined the chat").
     */
    private static void appendSystemMessage(JTextPane pane, StyledDocument doc, String text, String timestamp) throws BadLocationException {
        JPanel sysPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        sysPanel.setOpaque(false);

        JLabel sysLabel = new JLabel("  \uD83D\uDC65 " + text + " \u2022 " + timestamp + "  ");
        sysLabel.setForeground(SECONDARY_TEXT_COLOR);
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
    }

    /**
     * Appends a chat bubble (sent or received) with avatar, header, and optional delivery indicator.
     */
    private static void appendChatBubble(JTextPane pane, StyledDocument doc, String sender, String text, MessageType type, String timestamp) throws BadLocationException {
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

    /**
     * Creates the full bubble panel: header (avatar + name + time) + body + optional delivery indicator.
     */
    private static JPanel createBubblePanel(String sender, String text, MessageType type, String timestamp) {
        boolean isSent = (type == MessageType.SENT);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Header row
        String initials = computeInitials(sender, isSent);
        String displaySender = isSent ? "You" : sender;
        JPanel headerPanel = createHeaderPanel(initials, displaySender, timestamp, isSent);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(headerPanel);
        container.add(Box.createVerticalStrut(4));

        // Bubble body
        JPanel bubbleWrapper = createBubbleBody(text);
        bubbleWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(bubbleWrapper);

        // Delivered indicator (sent messages only)
        if (isSent) {
            JPanel deliveredWrapper = createDeliveredIndicator(text);
            deliveredWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(deliveredWrapper);
        }

        return container;
    }

    /**
     * Computes the 1-2 character initials to display inside the avatar circle.
     *
     * @param sender the sender name
     * @param isSent whether this is the current user's own message
     * @return the initials string (e.g., "ME", "AK", "JD")
     */
    private static String computeInitials(String sender, boolean isSent) {
        if (isSent) return "ME";
        if (sender == null || sender.length() < 2) return "U";

        String[] parts = sender.split(" ");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return sender.substring(0, 2).toUpperCase();
    }

    /**
     * Creates the header row containing avatar circle, sender name, and timestamp.
     */
    private static JPanel createHeaderPanel(String initials, String displaySender, String timestamp, boolean isSent) {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setOpaque(false);

        JPanel avatar = createAvatarPanel(initials, isSent);
        JLabel nameLabel = new JLabel(displaySender);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel timeLabel = new JLabel(timestamp);
        timeLabel.setForeground(SECONDARY_TEXT_COLOR);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        headerPanel.add(avatar);
        if (isSent) {
            headerPanel.add(timeLabel);
            headerPanel.add(nameLabel);
        } else {
            headerPanel.add(nameLabel);
            headerPanel.add(timeLabel);
        }

        return headerPanel;
    }

    /**
     * Creates a circular avatar panel with initials and an online-status dot.
     *
     * @param initials the text to draw inside the circle
     * @param isSent   true for purple (self), false for gray (others)
     * @return the avatar JPanel
     */
    private static JPanel createAvatarPanel(String initials, boolean isSent) {
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSent ? SENT_AVATAR_COLOR : RECEIVED_AVATAR_COLOR);
                g2.fillOval(0, 0, AVATAR_SIZE, AVATAR_SIZE);

                // Online dot
                g2.setColor(ONLINE_DOT_COLOR);
                g2.fillOval(16, 16, 8, 8);

                // Initials text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int x = (AVATAR_SIZE - fm.stringWidth(initials)) / 2;
                int y = ((AVATAR_SIZE - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initials, x, y);
            }
        };
        avatar.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatar.setOpaque(false);
        return avatar;
    }

    /**
     * Creates the rounded-rectangle bubble body containing the message text.
     * Supports inline code styling for file paths matching /docs/*.
     *
     * @param text the message text
     * @return a wrapper panel with left indentation
     */
    private static JPanel createBubbleBody(String text) {
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BUBBLE_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
            }
        };
        bubble.setLayout(new BorderLayout());
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setOpaque(false);

        String formattedText = text.replace("<", "&lt;").replace(">", "&gt;");
        formattedText = formattedText.replaceAll(
                "(/docs/[a-zA-Z0-9_.-]+)",
                "<span style='background-color:#13151F; color:#CCCCCC; padding:2px 6px; border-radius:4px; font-family:monospace;'>$1</span>"
        );
        String html = "<html><body style='color:white; font-family:\"Segoe UI\", sans-serif; font-size:13px; margin:0; padding:0;'>"
                + formattedText + "</body></html>";
        textPane.setText(html);
        bubble.add(textPane, BorderLayout.CENTER);

        JPanel bubbleWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 0));
        bubbleWrapper.setOpaque(false);
        bubbleWrapper.add(bubble);
        return bubbleWrapper;
    }

    /**
     * Creates the "✔✔ Delivered" indicator shown below sent messages.
     *
     * @param text the message text (used to size the indicator width)
     * @return a wrapper panel with the delivery status label
     */
    private static JPanel createDeliveredIndicator(String text) {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        statusPanel.setOpaque(false);
        JLabel statusLabel = new JLabel("\u2714\u2714 Delivered");
        statusLabel.setForeground(new Color(0xAAAAAA));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        int textWidth = Math.min(600, text.length() * 8 + 32);
        statusPanel.setPreferredSize(new Dimension(textWidth, 20));
        statusPanel.add(statusLabel);

        JPanel statusWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 0));
        statusWrapper.setOpaque(false);
        statusWrapper.add(statusPanel);
        return statusWrapper;
    }
}
