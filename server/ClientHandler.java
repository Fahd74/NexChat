package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Phase 2: Handles a single client connection on the server side.
 * Each instance runs in its own thread and manages reading messages
 * from one client, broadcasting them, and handling disconnection.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private final UsersRegistry registry = UsersRegistry.getInstance();

    /**
     * Constructs a handler for the given client socket.
     *
     * @param socket the accepted client socket
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Set up I/O streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // autoFlush

            // --- Handshake: first line must be "USERNAME|john" ---
            String firstLine = in.readLine();
            if (firstLine == null || !firstLine.startsWith("USERNAME|")) {
                System.err.println("[ClientHandler] Invalid handshake from " + socket.getRemoteSocketAddress());
                socket.close();
                return;
            }

            username = firstLine.substring("USERNAME|".length()).trim();
            if (username.isEmpty()) {
                System.err.println("[ClientHandler] Empty username received, closing connection.");
                socket.close();
                return;
            }

            // Check for duplicate username
            if (registry.isUsernameTaken(username)) {
                out.println("SYSTEM|Username '" + username + "' is already taken. Connection refused.|" + timestamp());
                socket.close();
                return;
            }

            // Register the user
            registry.register(username, out);
            System.out.println("[Server] " + username + " connected from " + socket.getRemoteSocketAddress());

            // Broadcast join event and updated user list
            registry.broadcast("SYSTEM|" + username + " joined the chat|" + timestamp());
            broadcastUserList();

            // --- Main message loop ---
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("CHAT|")) {
                    // Forward the chat message to all users
                    registry.broadcast(line);
                } else {
                    System.err.println("[ClientHandler] Unknown message format from " + username + ": " + line);
                }
            }

        } catch (IOException e) {
            System.err.println("[ClientHandler] IOException for user '" + username + "': " + e.getMessage());
        } finally {
            // --- Cleanup on disconnect ---
            handleDisconnect();
        }
    }

    /**
     * Handles cleanup when a client disconnects (gracefully or via exception).
     */
    private void handleDisconnect() {
        if (username != null) {
            registry.unregister(username);
            registry.broadcast("SYSTEM|" + username + " left the chat|" + timestamp());
            broadcastUserList();
            System.out.println("[Server] " + username + " disconnected.");
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error closing socket: " + e.getMessage());
        }
    }

    /**
     * Broadcasts the current user list to all connected clients.
     * Format: "USERLIST|alice,bob,john"
     */
    private void broadcastUserList() {
        List<String> users = registry.getUserList();
        String userListMsg = "USERLIST|" + String.join(",", users);
        registry.broadcast(userListMsg);
    }

    /**
     * @return current time formatted as HH:mm
     */
    private String timestamp() {
        return new SimpleDateFormat("HH:mm").format(new Date());
    }
}
