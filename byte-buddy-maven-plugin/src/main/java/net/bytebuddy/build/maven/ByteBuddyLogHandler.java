package net.bytebuddy.build.maven;

import org.apache.maven.plugin.logging.Log;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * A log handler for only printing Byte Buddy specific log-messages if debug logging is enabled for a plugin.
 */
public class ByteBuddyLogHandler extends Handler {

    /**
     * The Maven logging target.
     */
    private final Log log;

    /**
     * The Byte Buddy logger target.
     */
    private final Logger logger;

    /**
     * {@code true} if parent handler delegation was originally enabled.
     */
    private final boolean useParentHandlers;

    /**
     * Creates a new log handler.
     *
     * @param log               The Maven logging target.
     * @param logger            The Byte Buddy logger target.
     * @param useParentHandlers {@code true} if parent handler delegation was originally enabled.
     */
    protected ByteBuddyLogHandler(Log log, Logger logger, boolean useParentHandlers) {
        this.log = log;
        this.logger = logger;
        this.useParentHandlers = useParentHandlers;
        setFormatter(new SimpleFormatter());
    }

    /**
     * Initializes the Byte Buddy log handler.
     *
     * @param log The Maven logging target.
     * @return The registered log handler.
     */
    public static ByteBuddyLogHandler initialize(Log log) {
        Logger logger = Logger.getLogger("net.bytebuddy");
        ByteBuddyLogHandler handler = new ByteBuddyLogHandler(log, logger, logger.getUseParentHandlers());
        try {
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        } catch (SecurityException exception) {
            log.warn("Cannot configure Byte Buddy logging", exception);
        }
        return handler;
    }

    /**
     * Resets the configuration to its original state.
     */
    public void reset() {
        try {
            logger.setUseParentHandlers(useParentHandlers);
            logger.removeHandler(this);
        } catch (SecurityException exception) {
            log.warn("Cannot configure Byte Buddy logging", exception);
        }
    }

    @Override
    public void publish(LogRecord record) {
        if (log.isDebugEnabled()) {
            log.debug(getFormatter().format(record));
        }
    }

    @Override
    public void flush() {
        /* do nothing */
    }

    @Override
    public void close() {
        /* do nothing */
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ByteBuddyLogHandler that = (ByteBuddyLogHandler) object;
        return useParentHandlers == that.useParentHandlers
                && logger.equals(that.logger)
                && log.equals(that.log);
    }

    @Override
    public int hashCode() {
        int result = log.hashCode();
        result = 31 * result + logger.hashCode();
        result = 31 * result + (useParentHandlers ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ByteBuddyLogHandler{" +
                "log=" + log +
                " ,logger=" + logger +
                " ,useParentHandlers=" + useParentHandlers +
                '}';
    }
}
