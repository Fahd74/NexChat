package ui;

import client.ConnectionConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Phase 1: UI Only - Connection Dialog shown before the main window.
 * Gathers Username, Server IP, and Port to return a ConnectionConfig.
 */
public class LoginDialog extends JDialog {
    private JTextField usernameField;
    private JTextField serverIpField;
    private JTextField portField;
    private JLabel statusLabel;
    private ConnectionConfig config = null;

    public LoginDialog(JFrame parent) {
        super(parent, "NexChat - Secure Developer Chat", true);
        setSize(400, 480); // Adjusted height to fit UI well
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(new Color(0x13151F)); // Very dark background like the image
        setLayout(new BorderLayout());

        // --- Top Logo Area ---
        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw rounded logo background
                g2.setColor(new Color(0x7C6AFA));
                int size = 50;
                int x = (getWidth() - size) / 2;
                int y = 20;
                g2.fill(new RoundRectangle2D.Float(x, y, size, size, 16, 16));
                
                // Draw 'N' inside logo
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("N", x + (size - fm.stringWidth("N")) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
            }
        };
        logoPanel.setPreferredSize(new Dimension(400, 120));
        logoPanel.setOpaque(false);
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        
        // App Title below logo
        logoPanel.add(Box.createVerticalStrut(80));
        JLabel titleLabel = new JLabel("NexChat");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Secure Developer Chat");
        subtitleLabel.setForeground(new Color(0x888888));
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        logoPanel.add(titleLabel);
        logoPanel.add(Box.createVerticalStrut(4));
        logoPanel.add(subtitleLabel);
        
        add(logoPanel, BorderLayout.NORTH);

        // --- Form Area ---
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        usernameField = createCustomTextField("Fahd Mohamed Saad");
        serverIpField = createCustomTextField("192.168.1.100"); // Default from image
        portField = createCustomTextField("20443");

        addFormField(formPanel, "Username", usernameField);
        addFormField(formPanel, "Server IP", serverIpField);
        addFormField(formPanel, "Port", portField);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(0xFF5555)); // Error red
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(statusLabel);

        add(formPanel, BorderLayout.CENTER);

        // --- Bottom Button Area ---
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 40, 20, 40));
        
        JButton connectBtn = new JButton("Connect");
        styleButton(connectBtn, new Color(0x7C6AFA), Color.WHITE);
        connectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectBtn.setMaximumSize(new Dimension(320, 40));
        connectBtn.setPreferredSize(new Dimension(320, 40));
        
        connectBtn.addActionListener((ActionEvent e) -> handleConnect());
        
        JLabel footerLabel = new JLabel("Network Programming Project \u2022 2025");
        footerLabel.setForeground(new Color(0x666666));
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottomPanel.add(connectBtn);
        bottomPanel.add(Box.createVerticalStrut(20));
        bottomPanel.add(footerLabel);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Enter key mapping
        getRootPane().setDefaultButton(connectBtn);
    }

    private void addFormField(JPanel panel, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(0xAAAAAA));
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(320, 35));
        field.setPreferredSize(new Dimension(320, 35));

        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(field);
        panel.add(Box.createVerticalStrut(15));
    }

    private JTextField createCustomTextField(String defaultText) {
        JTextField field = new JTextField(defaultText) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                super.paintComponent(g);
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setBackground(new Color(0x1A1D2E));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A2D3E), 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return field;
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Simple hover effect could be added here
    }

    private void handleConnect() {
        String user = usernameField.getText().trim();
        String host = serverIpField.getText().trim();
        String portStr = portField.getText().trim();

        if (user.isEmpty() || host.isEmpty() || portStr.isEmpty()) {
            statusLabel.setText("All fields are required");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) throw new NumberFormatException();
            
            config = new ConnectionConfig(user, host, port);
            dispose();
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid port number");
        }
    }

    /**
     * Shows the dialog and blocks until closed.
     * @return ConnectionConfig if valid login, null if cancelled or closed
     */
    public ConnectionConfig showDialog() {
        setVisible(true);
        return config;
    }
}
