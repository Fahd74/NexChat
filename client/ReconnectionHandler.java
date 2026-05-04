package client;

import ui.ChatGUI;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.Socket;

/**
 * Handles automatic reconnection to the server after a connection drop.
 * Runs on a background thread to avoid blocking the EDT.
 * Tries up to {@link #MAX_ATTEMPTS} times with a {@link #RETRY_DELAY_MS}ms
 * delay between attempts, updating the GUI status on each attempt.
 */
public class ReconnectionHandler {

    private static final int MAX_ATTEMPTS = 10;
    private static final int RETRY_DELAY_MS = 3000; // 3 seconds

    private final String host;
    private final int port;
    private final ChatGUI gui;
    private final NetworkManager networkManager;

    /**
     * Constructs a ReconnectionHandler.
     *
     * @param host           the server host
     * @param port           the server port
     * @param username       the username to reconnect with (used by NetworkManager internally)
     * @param gui            the ChatGUI to update during reconnection
     * @param networkManager the NetworkManager to replace the connection on
     */
    public ReconnectionHandler(String host, int port, String username, ChatGUI gui, NetworkManager networkManager) {
        this.host = host;
        this.port = port;
        this.gui = gui;
        this.networkManager = networkManager;
    }

    /**
     * Attempts reconnection in a loop on a background thread.
     * On success: replaces the connection in NetworkManager and notifies the GUI.
     * On failure after all attempts: notifies the GUI of permanent failure.
     */
    public void attemptReconnect() {
        Thread reconnectThread = new Thread(() -> {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                final int currentAttempt = attempt;
                SwingUtilities.invokeLater(() -> {
                    gui.onReconnectAttempt(currentAttempt, MAX_ATTEMPTS);
                });
                System.out.println("[Reconnect] Attempt " + attempt + "/" + MAX_ATTEMPTS + "...");

                try {
                    Socket newSocket = new Socket(host, port);
                    // Success — replace the connection in NetworkManager
                    networkManager.replaceConnection(newSocket);
                    System.out.println("[Reconnect] Reconnected successfully on attempt " + attempt);

                    // Notify GUI on EDT, but start receiver on a background thread
                    gui.onReconnected();
                    return; // Exit the loop on success

                } catch (IOException e) {
                    System.err.println("[Reconnect] Attempt " + attempt + " failed: " + e.getMessage());
                }

                // Wait before next attempt
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    System.err.println("[Reconnect] Interrupted during wait: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // All attempts exhausted
            System.err.println("[Reconnect] Failed to reconnect after " + MAX_ATTEMPTS + " attempts.");
            SwingUtilities.invokeLater(() -> {
                gui.onReconnectFailed();
            });
        }, "NexChat-Reconnect");
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }
}
