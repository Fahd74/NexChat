package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Phase 2: Main server entry point.
 * Accepts client connections and spawns a ClientHandler thread for each.
 *
 * Usage: java Server <port>
 * Port validation: if > 65535, uses last 4 digits; if < 1024, adds 10000.
 */
public class Server {

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    /**
     * Constructs the server on the specified port.
     *
     * @param port the validated port number
     */
    public Server(int port) {
        this.port = port;
    }

    /**
     * Starts the server: binds the ServerSocket and enters the accept() loop.
     * Each accepted connection is delegated to a new ClientHandler thread.
     */
    public void start() {
        // Register shutdown hook for graceful SIGINT handling
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Server] Shutdown signal received. Cleaning up...");
            running = false;
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("[Server] Error during shutdown: " + e.getMessage());
            }
            System.out.println("[Server] Server stopped.");
        }));

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║         NexChat Server v1.0              ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  Status:  RUNNING                        ║");
            System.out.println("║  Port:    " + padRight(String.valueOf(port), 30) + "║");
            System.out.println("║  Waiting for connections...              ║");
            System.out.println("╚══════════════════════════════════════════╝");

            // Main accept() loop
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[Server] New connection from: " + clientSocket.getRemoteSocketAddress());
                    System.out.println("[Server] Active clients: " + (UsersRegistry.getInstance().getUserCount() + 1));

                    // Spawn a new handler thread for this client
                    ClientHandler handler = new ClientHandler(clientSocket);
                    Thread thread = new Thread(handler);
                    thread.setDaemon(true); // Allow JVM to exit even if client threads are alive
                    thread.start();

                } catch (IOException e) {
                    if (running) {
                        System.err.println("[Server] Error accepting connection: " + e.getMessage());
                    }
                    // If not running, the serverSocket was closed by the shutdown hook — expected
                }
            }

        } catch (IOException e) {
            System.err.println("[Server] FATAL — Could not start server on port " + port + ": " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Entry point. Reads port from args[0] and applies validation rules.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Server <port>");
            System.out.println("Example: java Server 8080");
            System.exit(1);
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("[Server] Invalid port number: " + args[0]);
            System.exit(1);
            return;
        }

        // Port validation rules from spec
        if (port > 65535) {
            // Use last 4 digits
            String portStr = String.valueOf(port);
            port = Integer.parseInt(portStr.substring(portStr.length() - 4));
            System.out.println("[Server] Port > 65535 — using last 4 digits: " + port);
        }
        if (port < 1024) {
            // Add 10000 to bring it into a safe range
            port += 10000;
            System.out.println("[Server] Port < 1024 — adjusted to: " + port);
        }

        Server server = new Server(port);
        server.start();
    }

    /** Right-pads a string to a given length for console formatting. */
    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
