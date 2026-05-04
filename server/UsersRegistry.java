package server;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe singleton registry of all connected users.
 * Maps each username to their socket output stream (PrintWriter).
 * Uses ConcurrentHashMap for safe concurrent access and synchronized
 * methods for compound operations that require atomicity.
 */
public class UsersRegistry {

    // Singleton instance
    private static final UsersRegistry INSTANCE = new UsersRegistry();

    // Thread-safe map: username -> their output stream
    private final ConcurrentHashMap<String, PrintWriter> onlineUsers = new ConcurrentHashMap<>();

    /** Private constructor enforces singleton pattern. */
    private UsersRegistry() {}

    /**
     * @return the single shared UsersRegistry instance
     */
    public static UsersRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Atomically registers a new user only if the username is not already taken.
     * This prevents the race condition where two clients with the same username
     * could both pass a check-then-register sequence.
     *
     * @param username the username to register
     * @param writer   the PrintWriter connected to the user's socket
     * @return true if registration succeeded, false if the username was already taken
     */
    public synchronized boolean registerIfAbsent(String username, PrintWriter writer) {
        if (onlineUsers.containsKey(username)) {
            return false;
        }
        onlineUsers.put(username, writer);
        System.out.println("[Registry] Registered: " + username + " | Total online: " + onlineUsers.size());
        return true;
    }

    /**
     * Unregisters a user (e.g. on disconnect).
     *
     * @param username the username to remove
     */
    public synchronized void unregister(String username) {
        onlineUsers.remove(username);
        System.out.println("[Registry] Unregistered: " + username + " | Total online: " + onlineUsers.size());
    }

    /**
     * Broadcasts a message string to ALL connected users.
     * Each user's PrintWriter is flushed automatically (autoFlush=true).
     *
     * @param message the fully formatted protocol message to send
     */
    public void broadcast(String message) {
        for (PrintWriter writer : onlineUsers.values()) {
            writer.println(message);
        }
    }

    /**
     * Sends a private message to a specific user.
     *
     * @param targetUsername the recipient's username
     * @param message       the fully formatted protocol message
     */
    public void sendTo(String targetUsername, String message) {
        PrintWriter writer = onlineUsers.get(targetUsername);
        if (writer != null) {
            writer.println(message);
        } else {
            System.err.println("[Registry] sendTo failed — user not found: " + targetUsername);
        }
    }

    /**
     * @return a snapshot List of all currently online usernames
     */
    public List<String> getUserList() {
        return new ArrayList<>(onlineUsers.keySet());
    }

    /**
     * @return the number of currently connected users
     */
    public int getUserCount() {
        return onlineUsers.size();
    }
}
