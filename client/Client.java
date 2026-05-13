package client;

import common.Protocol;
import common.TimeUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Console-based chat client for testing without the GUI.
 * Connects to the server, sends the username, then enters a send/receive loop.
 *
 * Usage: java client.Client &lt;host&gt; &lt;port&gt; &lt;username&gt;
 */
public class Client {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final String host;
    private final int port;
    private final String username;
    private volatile boolean running = true;
    private volatile boolean reconnected = false;

    /**
     * Constructs a Client with the given connection parameters.
     *
     * @param host     the server hostname or IP
     * @param port     the server port
     * @param username the display name for this client
     */
    public Client(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    /**
     * Connects to the server and enters the main message loop.
     * On connection failure, attempts automatic reconnection.
     */
    public void start() {
        try {
            connect();
            printBanner();
            startReceiverThread();
            readInputLoop();
        } catch (IOException e) {
            System.err.println("[Client] Connection failed: " + e.getMessage());
            if (attemptReconnect()) {
                startReceiverThread();
                readInputLoop();
            }
        } finally {
            // Only disconnect if we're truly done (not after a successful reconnect)
            if (!reconnected) {
                disconnect();
            }
        }
    }

    /**
     * Prints the connection banner to the console.
     */
    private void printBanner() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  NexChat Console Client                  ║");
        System.out.println("║  Connected as: " + padRight(username, 25) + "║");
        System.out.println("║  Server: " + padRight(host + ":" + port, 31) + "║");
        System.out.println("║  Type /quit to disconnect                ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    /**
     * Starts the background thread that reads messages from the server.
     */
    private void startReceiverThread() {
        Thread receiver = new Thread(new ReceiverThread(), "ConsoleClient-Receiver");
        receiver.setDaemon(true);
        receiver.start();
    }

    /**
     * Reads user input from stdin and sends chat messages to the server.
     * Blocks until the user types /quit or the connection drops.
     */
    private void readInputLoop() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("/quit")) {
                    System.out.println("[Client] Disconnecting...");
                    running = false;
                    break;
                }
                if (!input.isEmpty() && out != null) {
                    out.println(Protocol.chatMessage(username, input, TimeUtil.now()));
                }
            }
        }
    }

    /**
     * Opens the socket and sets up I/O streams. Sends username handshake.
     *
     * @throws IOException if connection or stream setup fails
     */
    private void connect() throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        out.println(Protocol.usernameMessage(username));
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
     *
     * @return true if reconnection succeeded, false if all attempts failed
     */
    private boolean attemptReconnect() {
        int maxAttempts = 10;
        int delay = 3000;

        for (int i = 1; i <= maxAttempts; i++) {
            System.out.println("[Client] Reconnect attempt " + i + "/" + maxAttempts + "...");
            try {
                Thread.sleep(delay);
                connect();
                System.out.println("[Client] Reconnected successfully!");
                reconnected = true;
                return true;

            } catch (IOException e) {
                System.err.println("[Client] Attempt " + i + " failed: " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("[Client] Reconnect interrupted.");
                Thread.currentThread().interrupt();
                return false;
            }
        }

        System.err.println("[Client] Could not reconnect after " + maxAttempts + " attempts. Exiting.");
        running = false;
        return false;
    }

    /**
     * Inner class: background thread that reads messages from the server
     * and formats them for console display.
     */
    private class ReceiverThread implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    String[] parts = line.split("\\|", -1);

                    switch (parts[0]) {
                        case Protocol.CHAT:
                            if (parts.length >= 4) {
                                System.out.println("[" + parts[3] + "] " + parts[1] + ": " + parts[2]);
                            }
                            break;

                        case Protocol.SYSTEM:
                            if (parts.length >= 3) {
                                System.out.println("  ** " + parts[1] + " [" + parts[2] + "] **");
                            }
                            break;

                        case Protocol.USERLIST:
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
                    if (attemptReconnect()) {
                        startReceiverThread();
                        readInputLoop();
                    }
                }
            }
        }
    }

    /**
     * Entry point for the console client.
     *
     * @param args expects: host, port, username
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java client.Client <host> <port> <username>");
            System.out.println("Example: java client.Client localhost 20443 Fahd");
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

    /**
     * Right-pads a string to a given length for console formatting.
     *
     * @param s the string to pad
     * @param n the target width
     * @return the padded string
     */
    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
