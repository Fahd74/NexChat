package server;

import common.Protocol;
import common.TimeUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * Handles a single client connection on the server side.
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
            if (firstLine == null || !firstLine.startsWith(Protocol.USERNAME + Protocol.DELIMITER)) {
                System.err.println("[ClientHandler] Invalid handshake from " + socket.getRemoteSocketAddress());
                socket.close();
                return;
            }

            username = firstLine.substring((Protocol.USERNAME + Protocol.DELIMITER).length()).trim();
            if (username.isEmpty()) {
                System.err.println("[ClientHandler] Empty username received, closing connection.");
                socket.close();
                return;
            }

            // Atomic check-and-register to prevent duplicate username race condition
            if (!registry.registerIfAbsent(username, out)) {
                out.println(Protocol.systemMessage(
                        "Username '" + username + "' is already taken. Connection refused.",
                        TimeUtil.now()));
                socket.close();
                return;
            }

            System.out.println("[Server] " + username + " connected from " + socket.getRemoteSocketAddress());

            // Broadcast join event and updated user list
            registry.broadcast(Protocol.systemMessage(username + " joined the chat", TimeUtil.now()));
            broadcastUserList();

            // --- Main message loop ---
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith(Protocol.CHAT + Protocol.DELIMITER)) {
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
            registry.broadcast(Protocol.systemMessage(username + " left the chat", TimeUtil.now()));
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
        registry.broadcast(Protocol.userListMessage(String.join(",", users)));
    }
}
