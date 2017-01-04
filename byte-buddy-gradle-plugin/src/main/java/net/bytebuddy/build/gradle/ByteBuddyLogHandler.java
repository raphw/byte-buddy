package net.bytebuddy.build.gradle;

import lombok.EqualsAndHashCode;
import org.gradle.api.Project;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * A log handler for only printing Byte Buddy specific log-messages if debug logging is enabled for a plugin.
 */
@EqualsAndHashCode(callSuper = false)
public class ByteBuddyLogHandler extends Handler {

    /**
     * The current project.
     */
    private final Project project;

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
     * @param project           The current project.
     * @param logger            The Byte Buddy logger target.
     * @param useParentHandlers {@code true} if parent handler delegation was originally enabled.
     */
    protected ByteBuddyLogHandler(Project project, Logger logger, boolean useParentHandlers) {
        this.project = project;
        this.logger = logger;
        this.useParentHandlers = useParentHandlers;
        setFormatter(new SimpleFormatter());
    }

    /**
     * Initializes the Byte Buddy log handler.
     *
     * @param project The current project.
     * @return The registered log handler.
     */
    public static ByteBuddyLogHandler initialize(Project project) {
        Logger logger = Logger.getLogger("net.bytebuddy");
        ByteBuddyLogHandler handler = new ByteBuddyLogHandler(project, logger, logger.getUseParentHandlers());
        try {
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        } catch (SecurityException exception) {
            project.getLogger().warn("Cannot configure Byte Buddy logging", exception);
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
            project.getLogger().warn("Cannot configure Byte Buddy logging", exception);
        }
    }

    @Override
    public void publish(LogRecord record) {
        if (project.getLogger().isDebugEnabled()) {
            project.getLogger().debug(getFormatter().format(record));
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
    public String toString() {
        return "ByteBuddyLogHandler{" +
                "project=" + project +
                " ,logger=" + logger +
                " ,useParentHandlers=" + useParentHandlers +
                '}';
    }
}
