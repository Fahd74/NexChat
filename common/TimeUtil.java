package common;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Shared time formatting utility used across server and client code.
 * Centralizes the timestamp format to ensure consistency.
 */
public final class TimeUtil {

    /** The standard time format used throughout the protocol. */
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    /** Private constructor prevents instantiation of this utility class. */
    private TimeUtil() {}

    /**
     * Returns the current time formatted as HH:mm.
     *
     * @return the current timestamp string
     */
    public static synchronized String now() {
        return TIME_FORMAT.format(new Date());
    }
}
