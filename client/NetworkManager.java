package client;

import common.Protocol;
import common.TimeUtil;
import ui.ChatGUI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Bridges the Swing GUI and the network layer.
 * Manages the Socket, I/O streams, and a background receiver thread.
 * All network operations run OFF the Event Dispatch Thread (EDT).
 */
public class NetworkManager {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private volatile boolean connected = false;
    private Thread receiverThread;

    /**
     * Connects to the server using the provided configuration.
     * Sends the USERNAME handshake as the first message.
     *
     * @param config the connection configuration from LoginDialog
     * @throws IOException if the connection or stream setup fails
     */
    public void connect(ConnectionConfig config) throws IOException {
        this.username = config.getUsername();
        socket = new Socket(config.getHost(), config.getPort());
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true); // autoFlush

        // Send username handshake
        out.println(Protocol.usernameMessage(username));

        connected = true;
        System.out.println("[NetworkManager] Connected to " + config.getHost() + ":" + config.getPort() + " as " + username);
    }

    /**
     * Starts a background thread that reads messages from the server
     * and dispatches them to the GUI via the appropriate callback methods.
     *
     * @param gui the ChatGUI instance to update
     */
    public void startReceiving(ChatGUI gui) {
        receiverThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    handleServerMessage(line, gui);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("[NetworkManager] Connection lost: " + e.getMessage());
                    connected = false;
                    // Trigger reconnection
                    gui.onConnectionLost();
                }
            }
        }, "NexChat-Receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    /**
     * Parses and routes a server message to the appropriate GUI handler.
     *
     * Protocol messages:
     *   CHAT|sender|text|HH:mm
     *   SYSTEM|text|HH:mm
     *   USERLIST|alice,bob,john
     *
     * @param raw the raw line from the server
     * @param gui the GUI to update
     */
    private void handleServerMessage(String raw, ChatGUI gui) {
        String[] parts = raw.split("\\|", -1); // -1 keeps trailing empties

        if (parts.length >= 1) {
            switch (parts[0]) {
                case Protocol.CHAT:
                    if (parts.length >= 4) {
                        String sender = parts[1];
                        String text = parts[2];
                        String time = parts[3];
                        // Don't echo our own messages — the GUI already rendered them locally
                        if (!sender.equals(username)) {
                            gui.onMessageReceived(sender, text, time);
                        }
                    }
                    break;

                case Protocol.SYSTEM:
                    if (parts.length >= 3) {
                        gui.onSystemMessage(parts[1], parts[2]);
                    }
                    break;

                case Protocol.USERLIST:
                    if (parts.length >= 2) {
                        String[] users = parts[1].split(",");
                        gui.onUserListUpdated(users);
                    }
                    break;

                default:
                    System.err.println("[NetworkManager] Unknown message type: " + raw);
                    break;
            }
        }
    }

    /**
     * Sends a chat message to the server.
     * Format: "CHAT|username|text|HH:mm"
     *
     * @param text the message text to send
     */
    public void sendMessage(String text) {
        if (connected && out != null) {
            out.println(Protocol.chatMessage(username, text, TimeUtil.now()));
        }
    }

    /**
     * Cleanly disconnects: closes streams, socket, and stops the receiver thread.
     */
    public void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[NetworkManager] Error during disconnect: " + e.getMessage());
        }
        System.out.println("[NetworkManager] Disconnected.");
    }

    /**
     * @return true if the socket is connected and streams are open
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * @return the username used for this session
     */
    public String getUsername() {
        return username;
    }

    /**
     * Replaces the current socket/streams with a fresh connection.
     * Used by ReconnectionHandler after successful reconnect.
     *
     * @param newSocket the newly connected socket
     * @throws IOException if stream setup fails
     */
    public void replaceConnection(Socket newSocket) throws IOException {
        // Close old resources silently
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}

        this.socket = newSocket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Re-send username handshake
        out.println(Protocol.usernameMessage(username));
        connected = true;
    }
}
