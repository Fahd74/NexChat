package common;

/**
 * Shared protocol constants used by both the server and client.
 * Defines the pipe-delimited message format prefixes to avoid
 * hardcoded strings and reduce the risk of typo-related bugs.
 *
 * Protocol messages:
 *   USERNAME|name            — Handshake (client → server)
 *   CHAT|sender|text|HH:mm  — Chat message (bidirectional)
 *   SYSTEM|text|HH:mm       — System event (server → client)
 *   USERLIST|user1,user2     — Online users (server → client)
 */
public final class Protocol {

    /** Prefix for username handshake messages. */
    public static final String USERNAME = "USERNAME";

    /** Prefix for chat messages. */
    public static final String CHAT = "CHAT";

    /** Prefix for system event messages. */
    public static final String SYSTEM = "SYSTEM";

    /** Prefix for online users list messages. */
    public static final String USERLIST = "USERLIST";

    /** The pipe delimiter character used to separate message fields. */
    public static final String DELIMITER = "|";

    /** Private constructor prevents instantiation of this utility class. */
    private Protocol() {}

    /**
     * Builds a USERNAME handshake message.
     *
     * @param username the username to register
     * @return the formatted handshake string
     */
    public static String usernameMessage(String username) {
        return USERNAME + DELIMITER + username;
    }

    /**
     * Builds a CHAT message.
     *
     * @param sender    the message sender
     * @param text      the message body
     * @param timestamp the formatted time (HH:mm)
     * @return the formatted chat string
     */
    public static String chatMessage(String sender, String text, String timestamp) {
        return CHAT + DELIMITER + sender + DELIMITER + text + DELIMITER + timestamp;
    }

    /**
     * Builds a SYSTEM message.
     *
     * @param text      the event description
     * @param timestamp the formatted time (HH:mm)
     * @return the formatted system string
     */
    public static String systemMessage(String text, String timestamp) {
        return SYSTEM + DELIMITER + text + DELIMITER + timestamp;
    }

    /**
     * Builds a USERLIST message.
     *
     * @param csvUsers comma-separated list of online usernames
     * @return the formatted userlist string
     */
    public static String userListMessage(String csvUsers) {
        return USERLIST + DELIMITER + csvUsers;
    }
}
