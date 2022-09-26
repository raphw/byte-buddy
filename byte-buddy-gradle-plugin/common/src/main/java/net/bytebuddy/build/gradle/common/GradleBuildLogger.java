package net.bytebuddy.build.gradle.common;

import net.bytebuddy.build.BuildLogger;
import org.gradle.api.logging.Logger;

/**
 * A {@link BuildLogger} implementation for a Gradle {@link Logger}.
 */
public class GradleBuildLogger implements BuildLogger {

    /**
     * The logger to delegate to.
     */
    private final Logger logger;

    /**
     * Creates a new Gradle build logger.
     *
     * @param logger The logger to delegate to.
     */
    public GradleBuildLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void debug(String message) {
        logger.debug(message);
    }

    /**
     * {@inheritDoc}
     */
    public void debug(String message, Throwable throwable) {
        logger.debug(message, throwable);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void info(String message) {
        logger.info(message);
    }

    /**
     * {@inheritDoc}
     */
    public void info(String message, Throwable throwable) {
        logger.info(message, throwable);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void warn(String message) {
        logger.warn(message);
    }

    /**
     * {@inheritDoc}
     */
    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void error(String message) {
        logger.error(message);
    }

    /**
     * {@inheritDoc}
     */
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
}

