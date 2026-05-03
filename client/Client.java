package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * Phase 2: Console-based chat client for testing without the GUI.
 * Connects to the server, sends the username, then enters a send/receive loop.
 *
 * Usage: java Client <host> <port> <username>
 */
public class Client {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final String host;
    private final int port;
    private final String username;
    private volatile boolean running = true;

    /**
     * Constructs a Client with the given connection parameters.
     */
    public Client(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    /**
     * Connects to the server and starts the message loop.
     */
    public void start() {
        try {
            connect();
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║  NexChat Console Client                  ║");
            System.out.println("║  Connected as: " + padRight(username, 25) + "║");
            System.out.println("║  Server: " + padRight(host + ":" + port, 31) + "║");
            System.out.println("║  Type /quit to disconnect                ║");
            System.out.println("╚══════════════════════════════════════════╝");

            // Start the background receiver thread
            Thread receiver = new Thread(new ReceiverThread(), "ConsoleClient-Receiver");
            receiver.setDaemon(true);
            receiver.start();

            // Main thread: read user input from stdin and send to server
            try (Scanner scanner = new Scanner(System.in)) {
                while (running) {
                    String input = scanner.nextLine().trim();
                    if (input.equalsIgnoreCase("/quit")) {
                        System.out.println("[Client] Disconnecting...");
                        running = false;
                        break;
                    }
                    if (!input.isEmpty()) {
                        String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
                        out.println("CHAT|" + username + "|" + input + "|" + timestamp);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Client] Connection failed: " + e.getMessage());
            attemptReconnect();
        } finally {
            disconnect();
        }
    }

    /**
     * Opens the socket and sets up I/O streams. Sends username handshake.
     */
    private void connect() throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        out.println("USERNAME|" + username);
    }

    /**
     * Closes all resources cleanly.
     */
    private void disconnect() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[Client] Error during disconnect: " + e.getMessage());
        }
        System.out.println("[Client] Disconnected.");
    }

    /**
     * Attempts to reconnect after a connection loss.
     * Max 10 attempts, 3 seconds between each.
     */
    private void attemptReconnect() {
        int maxAttempts = 10;
        int delay = 3000;

        for (int i = 1; i <= maxAttempts; i++) {
            System.out.println("[Client] Reconnect attempt " + i + "/" + maxAttempts + "...");
            try {
                Thread.sleep(delay);
                connect();
                System.out.println("[Client] Reconnected successfully!");

                // Re-start receiver
                Thread receiver = new Thread(new ReceiverThread(), "ConsoleClient-Receiver");
                receiver.setDaemon(true);
                receiver.start();
                return;

            } catch (IOException e) {
                System.err.println("[Client] Attempt " + i + " failed: " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("[Client] Reconnect interrupted.");
                Thread.currentThread().interrupt();
                return;
            }
        }

        System.err.println("[Client] Could not reconnect after " + maxAttempts + " attempts. Exiting.");
        running = false;
    }

    /**
     * Inner class: background thread that reads messages from the server.
     */
    private class ReceiverThread implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    String[] parts = line.split("\\|", -1);

                    switch (parts[0]) {
                        case "CHAT":
                            if (parts.length >= 4) {
                                System.out.println("[" + parts[3] + "] " + parts[1] + ": " + parts[2]);
                            }
                            break;

                        case "SYSTEM":
                            if (parts.length >= 3) {
                                System.out.println("  ** " + parts[1] + " [" + parts[2] + "] **");
                            }
                            break;

                        case "USERLIST":
                            if (parts.length >= 2) {
                                System.out.println("  [Online: " + parts[1] + "]");
                            }
                            break;

                        default:
                            System.out.println("[Server] " + line);
                            break;
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("[Client] Lost connection to server: " + e.getMessage());
                    running = false;
                    attemptReconnect();
                }
            }
        }
    }

    /**
     * Entry point.
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Client <host> <port> <username>");
            System.out.println("Example: java Client localhost 8080 Alice");
            System.exit(1);
        }

        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("[Client] Invalid port: " + args[1]);
            System.exit(1);
            return;
        }
        String username = args[2];

        Client client = new Client(host, port, username);
        client.start();
    }

    /** Right-pads a string for console formatting. */
    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
