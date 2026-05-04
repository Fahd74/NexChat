package ui;

import client.ConnectionConfig;
import client.NetworkManager;
import client.ReconnectionHandler;
import common.TimeUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

/**
 * Main chat window GUI — wired to NetworkManager for real-time messaging.
 * Displays a 3-panel layout: left sidebar (navigation + online users),
 * center chat area, and right info panel (members + server stats).
 * All network calls are performed off the EDT via NetworkManager.
 */
public class ChatGUI extends JFrame {

    private static final Color BG_MAIN = new Color(0x1A1D2E);
    private static final Color BG_SIDEBAR = new Color(0x13151F);
    private static final Color ACCENT_PURPLE = new Color(0x7C6AFA);
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color TEXT_GRAY = new Color(0xAAAAAA);
    private static final Color STATUS_ONLINE = new Color(0x4CAF50);
    private static final Color STATUS_OFFLINE = new Color(0xF44336);
    private static final Color STATUS_RECONNECTING = new Color(0xFFC107);

    private DefaultListModel<String> userListModel;
    private JLabel userCountLabel;
    private JTextPane chatArea;
    private JTextField inputField;
    private JLabel connectionStatusDot;
    private JLabel connectionStatusText;

    private JPanel membersBox; // To hold right panel members
    private JLabel membersCountLabel;
    
    private JLabel statLatencyLabel;

    private final ConnectionConfig config;
    private volatile boolean isConnected = false;
    private NetworkManager networkManager;

    public ChatGUI(ConnectionConfig config) {
        this.config = config;

        setTitle("NexChat - Connected as " + config.getUsername());
        setSize(1100, 720); // Wider to accommodate 3 columns
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_MAIN);

        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 13));

        initSidebar();
        initCenterPanel();
        initRightPanel();
    }

    /**
     * Sets the NetworkManager instance used for sending/receiving messages.
     * @param nm the connected NetworkManager
     */
    public void setNetworkManager(NetworkManager nm) {
        this.networkManager = nm;
        setConnectionStatus(true);
    }

    /** Called by NetworkManager when a CHAT message is received from another user. */
    public void onMessageReceived(String sender, String text, String timestamp) {
        SwingUtilities.invokeLater(() -> {
            MessageBubble.appendMessage(chatArea, sender, text, MessageBubble.MessageType.RECEIVED, timestamp);
            scrollToBottom();
        });
    }

    /** Called by NetworkManager when a SYSTEM message is received. */
    public void onSystemMessage(String text, String timestamp) {
        SwingUtilities.invokeLater(() -> {
            MessageBubble.appendMessage(chatArea, "System", text, MessageBubble.MessageType.SYSTEM, timestamp);
            scrollToBottom();
        });
    }

    /** Called by NetworkManager when a USERLIST message is received. */
    public void onUserListUpdated(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            membersBox.removeAll();
            
            int count = 0;
            // Always add self to members list
            addMemberToRightPanel("You", "ME", true);
            count++;
            
            for (String user : users) {
                String trimmed = user.trim();
                if (!trimmed.isEmpty() && !trimmed.equals(config.getUsername())) {
                    userListModel.addElement(trimmed);
                    addMemberToRightPanel(trimmed, getInitials(trimmed), false);
                    count++;
                }
            }
            userCountLabel.setText(String.valueOf(userListModel.getSize()));
            membersCountLabel.setText("MEMBERS (" + count + ")");
            
            membersBox.revalidate();
            membersBox.repaint();
        });
    }
    
    private String getInitials(String name) {
        if (name == null || name.length() < 2) return "U";
        String[] parts = name.split(" ");
        if (parts.length >= 2) return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return name.substring(0, 2).toUpperCase();
    }

    private void addMemberToRightPanel(String name, String initials, boolean isMe) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(250, 40));
        
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isMe ? ACCENT_PURPLE : new Color(0x4A4D6E));
                g2.fillOval(0, 0, 24, 24);
                g2.setColor(STATUS_ONLINE);
                g2.fillOval(16, 16, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int x = (24 - fm.stringWidth(initials)) / 2;
                int y = ((24 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initials, x, y);
            }
        };
        avatar.setPreferredSize(new Dimension(24, 24));
        avatar.setOpaque(false);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT_WHITE);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        panel.add(avatar);
        panel.add(nameLabel);
        
        membersBox.add(panel);
    }

    /** Called by NetworkManager when the server connection is lost unexpectedly. */
    public void onConnectionLost() {
        SwingUtilities.invokeLater(() -> {
            isConnected = false;
            connectionStatusDot.setForeground(STATUS_OFFLINE);
            connectionStatusText.setText("Disconnected");
            statLatencyLabel.setText("--");
            statLatencyLabel.setForeground(TEXT_GRAY);
            showSystemMessage("Connection to server lost. Reconnecting...");
        });
        ReconnectionHandler handler = new ReconnectionHandler(
                config.getHost(), config.getPort(), config.getUsername(), this, networkManager);
        handler.attemptReconnect();
    }

    /** Called by ReconnectionHandler on each retry attempt. */
    public void onReconnectAttempt(int attempt, int maxAttempts) {
        SwingUtilities.invokeLater(() -> {
            connectionStatusDot.setForeground(STATUS_RECONNECTING);
            connectionStatusText.setText("Reconnecting... " + attempt + "/" + maxAttempts);
        });
    }

    /** Called by ReconnectionHandler when reconnection succeeds. */
    public void onReconnected() {
        SwingUtilities.invokeLater(() -> {
            isConnected = true;
            setConnectionStatus(true);
            showSystemMessage("Reconnected successfully.");
            statLatencyLabel.setText("12ms");
            statLatencyLabel.setForeground(STATUS_ONLINE);
        });
        // Start receiving on a NEW background thread — never on the EDT
        new Thread(() -> networkManager.startReceiving(this), "NexChat-PostReconnect").start();
    }

    /** Called by ReconnectionHandler when all reconnect attempts are exhausted. */
    public void onReconnectFailed() {
        SwingUtilities.invokeLater(() -> {
            connectionStatusDot.setForeground(STATUS_OFFLINE);
            connectionStatusText.setText("Failed");
            showSystemMessage("Could not reconnect after multiple attempts.");
        });
    }

    private void initSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x2A2D3E)));

        // --- North: Brand & Nav ---
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setOpaque(false);
        
        // Brand
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 20));
        brandPanel.setOpaque(false);
        JPanel logo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_PURPLE);
                g2.fillRoundRect(0, 0, 32, 32, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                g2.drawString("N", 9, 23);
            }
        };
        logo.setPreferredSize(new Dimension(32, 32));
        logo.setOpaque(false);
        
        JPanel brandText = new JPanel();
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.setOpaque(false);
        JLabel appTitle = new JLabel("NexChat");
        appTitle.setForeground(TEXT_WHITE);
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.setOpaque(false);
        connectionStatusDot = new JLabel("●");
        connectionStatusDot.setForeground(STATUS_OFFLINE);
        connectionStatusDot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        connectionStatusText = new JLabel("Disconnected");
        connectionStatusText.setForeground(TEXT_GRAY);
        connectionStatusText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusPanel.add(connectionStatusDot);
        statusPanel.add(connectionStatusText);
        
        brandText.add(appTitle);
        brandText.add(statusPanel);
        brandPanel.add(logo);
        brandPanel.add(brandText);
        northPanel.add(brandPanel);
        
        // Navigation
        String[] navItems = {"\uD83D\uDCAC  Messages", "\uD83D\uDDE8  Channels", "\uD83D\uDDA5  Servers", "\u2699  Settings"};
        for (int i = 0; i < navItems.length; i++) {
            JPanel navItem = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
            if (i == 1) { // Channels active
                navItem.setBackground(new Color(0x212336));
                navItem.setOpaque(true);
                navItem.setBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT_PURPLE));
            } else {
                navItem.setOpaque(false);
                navItem.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
            }
            JLabel navLabel = new JLabel(navItems[i]);
            navLabel.setForeground(i == 1 ? TEXT_WHITE : TEXT_GRAY);
            navLabel.setFont(new Font("Segoe UI", i == 1 ? Font.BOLD : Font.PLAIN, 14));
            navItem.add(navLabel);
            northPanel.add(navItem);
        }
        
        northPanel.add(Box.createVerticalStrut(20));
        
        // Online Users Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        JLabel headerLabel = new JLabel("ONLINE USERS");
        headerLabel.setForeground(TEXT_GRAY);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        userCountLabel = new JLabel("0");
        userCountLabel.setForeground(TEXT_WHITE);
        userCountLabel.setBackground(new Color(0x2A2D3E));
        userCountLabel.setOpaque(true);
        userCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        userCountLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A2D3E), 2, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        headerPanel.add(userCountLabel, BorderLayout.EAST);
        northPanel.add(headerPanel);
        
        sidebar.add(northPanel, BorderLayout.NORTH);

        // --- Center: User List ---
        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        userList.setBackground(BG_SIDEBAR);
        userList.setForeground(TEXT_WHITE);
        userList.setCellRenderer(new UserListRenderer());
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane listScroller = new JScrollPane(userList);
        listScroller.setBorder(null);
        listScroller.getViewport().setBackground(BG_SIDEBAR);
        styleScrollBar(listScroller.getVerticalScrollBar());
        sidebar.add(listScroller, BorderLayout.CENTER);
        
        // --- South: Profile ---
        JPanel profilePanel = new JPanel(new BorderLayout());
        profilePanel.setBackground(new Color(0x181A26));
        profilePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x2A2D3E)),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JPanel pInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pInfo.setOpaque(false);
        JLabel profileIcon = new JLabel("ME");
        profileIcon.setOpaque(true);
        profileIcon.setBackground(ACCENT_PURPLE);
        profileIcon.setForeground(Color.WHITE);
        profileIcon.setHorizontalAlignment(SwingConstants.CENTER);
        profileIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        profileIcon.setPreferredSize(new Dimension(32, 32));
        
        JPanel pTextPanel = new JPanel();
        pTextPanel.setLayout(new BoxLayout(pTextPanel, BoxLayout.Y_AXIS));
        pTextPanel.setOpaque(false);
        JLabel pName = new JLabel("Profile");
        pName.setForeground(Color.WHITE);
        pName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel pStatus = new JLabel("Online");
        pStatus.setForeground(TEXT_GRAY);
        pStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pTextPanel.add(pName);
        pTextPanel.add(pStatus);
        
        pInfo.add(profileIcon);
        pInfo.add(pTextPanel);
        
        JButton logoutBtn = new JButton("\u21A6"); // Right arrow icon
        logoutBtn.setForeground(TEXT_GRAY);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoutBtn.addActionListener(e -> disconnect());
        
        profilePanel.add(pInfo, BorderLayout.WEST);
        profilePanel.add(logoutBtn, BorderLayout.EAST);
        
        sidebar.add(profilePanel, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);
    }

    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BG_MAIN);

        // --- North: Chat Header ---
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(new Color(0x1D2033));
        chatHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2A2D3E)));
        chatHeader.setPreferredSize(new Dimension(0, 60));
        
        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 18));
        leftHeader.setOpaque(false);
        JLabel channelName = new JLabel("# general");
        channelName.setForeground(TEXT_WHITE);
        channelName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JLabel badge = new JLabel("24");
        badge.setForeground(TEXT_GRAY);
        badge.setBackground(new Color(0x2A2D3E));
        badge.setOpaque(true);
        badge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2A2D3E), 2, true),
            new EmptyBorder(2, 6, 2, 6)
        ));
        
        // Search bar
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        searchBox.setBackground(new Color(0x13151F));
        searchBox.setBorder(BorderFactory.createLineBorder(new Color(0x2A2D3E), 1, true));
        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setForeground(TEXT_GRAY);
        JTextField searchField = new JTextField("Search...", 15);
        searchField.setBackground(new Color(0x13151F));
        searchField.setForeground(TEXT_GRAY);
        searchField.setBorder(null);
        searchBox.add(searchIcon);
        searchBox.add(searchField);
        
        leftHeader.add(channelName);
        leftHeader.add(badge);
        leftHeader.add(Box.createHorizontalStrut(10));
        leftHeader.add(searchBox);
        
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 18));
        rightHeader.setOpaque(false);
        String[] icons = {"\uD83D\uDD14", "\u2753", "\u22EE"}; // Bell, Question, 3 dots
        for (String icon : icons) {
            JLabel lbl = new JLabel(icon);
            lbl.setForeground(TEXT_GRAY);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            rightHeader.add(lbl);
        }
        
        chatHeader.add(leftHeader, BorderLayout.WEST);
        chatHeader.add(rightHeader, BorderLayout.EAST);
        centerPanel.add(chatHeader, BorderLayout.NORTH);

        // --- Center: Chat Area ---
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(BG_MAIN);
        chatArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JScrollPane chatScroller = new JScrollPane(chatArea);
        chatScroller.setBorder(null);
        chatScroller.getViewport().setBackground(BG_MAIN);
        styleScrollBar(chatScroller.getVerticalScrollBar());
        centerPanel.add(chatScroller, BorderLayout.CENTER);

        // --- South: Input Bar ---
        JPanel inputBar = new JPanel(new BorderLayout(10, 0));
        inputBar.setBackground(BG_MAIN);
        inputBar.setBorder(new EmptyBorder(15, 20, 20, 20));

        JPanel inputContainer = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x212336)); // Input background
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0x2A2D3E));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
            }
        };
        inputContainer.setOpaque(false);
        inputContainer.setBorder(new EmptyBorder(8, 15, 8, 10));

        JLabel plusIcon = new JLabel("\u2295"); // Plus icon
        plusIcon.setForeground(TEXT_GRAY);
        plusIcon.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        plusIcon.setBorder(new EmptyBorder(0, 0, 0, 10));
        
        inputField = new JTextField();
        inputField.setBackground(new Color(0x212336));
        inputField.setForeground(TEXT_WHITE);
        inputField.setCaretColor(TEXT_WHITE);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(null);
        inputField.setOpaque(false);
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        JPanel rightInput = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightInput.setOpaque(false);
        JLabel smileyIcon = new JLabel("\u263A"); // Smiley
        smileyIcon.setForeground(TEXT_GRAY);
        smileyIcon.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        
        JButton sendBtn = new JButton("\u27A4"); // Send arrow
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setBackground(ACCENT_PURPLE);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sendBtn.setBorder(new EmptyBorder(5, 12, 5, 12));
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());
        
        rightInput.add(smileyIcon);
        rightInput.add(sendBtn);

        inputContainer.add(plusIcon, BorderLayout.WEST);
        inputContainer.add(inputField, BorderLayout.CENTER);
        inputContainer.add(rightInput, BorderLayout.EAST);

        inputBar.add(inputContainer, BorderLayout.CENTER);
        centerPanel.add(inputBar, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);
    }
    
    private void initRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BG_MAIN);
        rightPanel.setPreferredSize(new Dimension(260, 0));
        rightPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0x2A2D3E)));
        
        // --- North: Chat Info Header ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        headerPanel.setOpaque(false);
        JLabel headerLabel = new JLabel("Chat Info");
        headerLabel.setForeground(TEXT_WHITE);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerPanel.add(headerLabel);
        rightPanel.add(headerPanel, BorderLayout.NORTH);
        
        // --- Center: Members and Stats ---
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);
        centerContent.setBorder(new EmptyBorder(0, 20, 20, 20));
        
        // Members section
        JPanel membersHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        membersHeader.setOpaque(false);
        JLabel membersIcon = new JLabel("\uD83D\uDC65 ");
        membersIcon.setForeground(TEXT_GRAY);
        membersCountLabel = new JLabel("MEMBERS (0)");
        membersCountLabel.setForeground(TEXT_GRAY);
        membersCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        membersHeader.add(membersIcon);
        membersHeader.add(membersCountLabel);
        
        membersBox = new JPanel();
        membersBox.setLayout(new BoxLayout(membersBox, BoxLayout.Y_AXIS));
        membersBox.setOpaque(false);
        
        centerContent.add(membersHeader);
        centerContent.add(membersBox);
        centerContent.add(Box.createVerticalStrut(30));
        
        // Stats section
        JPanel statsHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        statsHeader.setOpaque(false);
        JLabel statsLabel = new JLabel("SERVER STATS");
        statsLabel.setForeground(TEXT_GRAY);
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statsHeader.add(statsLabel);
        
        JPanel statsBox = new JPanel(new GridLayout(3, 2, 10, 15));
        statsBox.setOpaque(false);
        
        JLabel lblIp = new JLabel("IP");
        lblIp.setForeground(TEXT_GRAY);
        JLabel valIp = new JLabel(config.getHost() == null || config.getHost().isEmpty() ? "192.168.1.104" : config.getHost()); 
        valIp.setForeground(TEXT_GRAY);
        valIp.setFont(new Font("Monospaced", Font.PLAIN, 12));
        valIp.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JLabel lblPort = new JLabel("Port");
        lblPort.setForeground(TEXT_GRAY);
        JLabel valPort = new JLabel(String.valueOf(config.getPort()));
        valPort.setForeground(TEXT_GRAY);
        valPort.setFont(new Font("Monospaced", Font.PLAIN, 12));
        valPort.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JLabel lblLatency = new JLabel("Latency");
        lblLatency.setForeground(TEXT_GRAY);
        statLatencyLabel = new JLabel("12ms");
        statLatencyLabel.setForeground(STATUS_ONLINE);
        statLatencyLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statLatencyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        statsBox.add(lblIp); statsBox.add(valIp);
        statsBox.add(lblPort); statsBox.add(valPort);
        statsBox.add(lblLatency); statsBox.add(statLatencyLabel);
        
        JPanel statsContainer = new JPanel(new BorderLayout());
        statsContainer.setBackground(new Color(0x212336));
        statsContainer.setBorder(new EmptyBorder(15, 15, 15, 15));
        statsContainer.add(statsBox, BorderLayout.CENTER);
        
        centerContent.add(statsHeader);
        centerContent.add(statsContainer);
        centerContent.add(Box.createVerticalGlue()); // Push everything up
        
        rightPanel.add(centerContent, BorderLayout.CENTER);
        
        // --- South: Reconnect Button ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        bottomPanel.setOpaque(false);
        
        JButton reconnectBtn = new JButton("\u21BA Reconnect"); // Circular arrow
        reconnectBtn.setForeground(new Color(0xFA6A6A));
        reconnectBtn.setBackground(new Color(0x1D2033));
        reconnectBtn.setFocusPainted(false);
        reconnectBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2A2D3E), 1, true),
            new EmptyBorder(8, 40, 8, 40)
        ));
        reconnectBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reconnectBtn.addActionListener(e -> {
            if (!isConnected) {
                onConnectionLost();
            }
        });
        
        bottomPanel.add(reconnectBtn);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);
    }

    private void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(0x3A3D4E);
                this.trackColor = BG_MAIN;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
        scrollBar.setPreferredSize(new Dimension(8, 0));
    }

    private void setConnectionStatus(boolean connected) {
        this.isConnected = connected;
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                connectionStatusDot.setForeground(STATUS_ONLINE);
                connectionStatusText.setText("Connected");
            } else {
                connectionStatusDot.setForeground(STATUS_OFFLINE);
                connectionStatusText.setText("Disconnected");
            }
        });
    }

    private void disconnect() {
        if (networkManager != null) {
            new Thread(() -> networkManager.disconnect(), "NexChat-Disconnect").start();
        }
        setConnectionStatus(false);
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog(null);
            ConnectionConfig newConfig = login.showDialog();
            if (newConfig != null) {
                launchWithConnection(newConfig);
            } else {
                System.exit(0);
            }
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty() && isConnected) {
            String timestamp = TimeUtil.now();
            MessageBubble.appendMessage(chatArea, config.getUsername(), text, MessageBubble.MessageType.SENT, timestamp);
            if (networkManager != null) {
                networkManager.sendMessage(text);
            }
            inputField.setText("");
            scrollToBottom();
        }
    }

    public void showSystemMessage(String text) {
        String timestamp = TimeUtil.now();
        SwingUtilities.invokeLater(() -> {
            MessageBubble.appendMessage(chatArea, "System", text, MessageBubble.MessageType.SYSTEM, timestamp);
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private static void launchWithConnection(ConnectionConfig config) {
        new Thread(() -> {
            NetworkManager nm = new NetworkManager();
            try {
                nm.connect(config);
                SwingUtilities.invokeLater(() -> {
                    ChatGUI chatGui = new ChatGUI(config);
                    chatGui.setNetworkManager(nm);
                    chatGui.setVisible(true);
                    nm.startReceiving(chatGui);
                });
            } catch (IOException e) {
                System.err.println("[ChatGUI] Connection failed: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null,
                            "Could not connect to " + config.getHost() + ":" + config.getPort() + "\n" + e.getMessage(),
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                    LoginDialog login = new LoginDialog(null);
                    ConnectionConfig retry = login.showDialog();
                    if (retry != null) {
                        launchWithConnection(retry);
                    } else {
                        System.exit(0);
                    }
                });
            }
        }, "NexChat-Connect").start();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog(null);
            ConnectionConfig config = login.showDialog();

            if (config != null) {
                launchWithConnection(config);
            } else {
                System.exit(0);
            }
        });
    }
}
