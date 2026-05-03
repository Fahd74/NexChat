package client;

/**
 * Phase 1: UI Only - Connection Configuration POJO
 * Stores the connection details collected from the LoginDialog.
 */
public class ConnectionConfig {
    private final String username;
    private final String host;
    private final int port;

    /**
     * Constructs a new ConnectionConfig.
     *
     * @param username The chosen username
     * @param host     The server IP or hostname
     * @param port     The server port
     */
    public ConnectionConfig(String username, String host, int port) {
        this.username = username;
        this.host = host;
        this.port = port;
    }

    /**
     * @return the configured username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the configured server host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the configured server port
     */
    public int getPort() {
        return port;
    }
}
