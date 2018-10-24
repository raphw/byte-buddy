/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An API that can be implemented by logging build systems to allow plugins to log information without depending on
 * a build system-specific logging API.
 */
public interface BuildLogger {

    /**
     * Returns {@code true} if the debug log level is enabled.
     *
     * @return {@code true} if the debug log level is enabled.
     */
    boolean isDebugEnabled();

    /**
     * Logs a message on the debug level.
     *
     * @param message The message to log.
     */
    void debug(String message);

    /**
     * Logs a message on the debug level.
     *
     * @param message   The message to log.
     * @param throwable A throwable that is attached to the message.
     */
    void debug(String message, Throwable throwable);

    /**
     * Returns {@code true} if the info log level is enabled.
     *
     * @return {@code true} if the info log level is enabled.
     */
    boolean isInfoEnabled();

    /**
     * Logs a message on the info level.
     *
     * @param message The message to log.
     */
    void info(String message);

    /**
     * Logs a message on the info level.
     *
     * @param message   The message to log.
     * @param throwable A throwable that is attached to the message.
     */
    void info(String message, Throwable throwable);

    /**
     * Returns {@code true} if the warn log level is enabled.
     *
     * @return {@code true} if the warn log level is enabled.
     */
    boolean isWarnEnabled();

    /**
     * Logs a message on the warn level.
     *
     * @param message The message to log.
     */
    void warn(String message);

    /**
     * Logs a message on the warn level.
     *
     * @param message   The message to log.
     * @param throwable A throwable that is attached to the message.
     */
    void warn(String message, Throwable throwable);

    /**
     * Returns {@code true} if the error log level is enabled.
     *
     * @return {@code true} if the error log level is enabled.
     */
    boolean isErrorEnabled();

    /**
     * Logs a message on the error level.
     *
     * @param message The message to log.
     */
    void error(String message);

    /**
     * Logs a message on the error level.
     *
     * @param message   The message to log.
     * @param throwable A throwable that is attached to the message.
     */
    void error(String message, Throwable throwable);

    /**
     * A non-operational build logger that discards all statements.
     */
    enum NoOp implements BuildLogger {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public boolean isDebugEnabled() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message, Throwable throwable) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInfoEnabled() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message, Throwable throwable) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public boolean isWarnEnabled() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message, Throwable throwable) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public boolean isErrorEnabled() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message, Throwable throwable) {
            /* empty */
        }
    }

    /**
     * An abstract adapter implementation for a build logger.
     */
    abstract class Adapter implements BuildLogger {

        /**
         * {@inheritDoc}
         */
        public boolean isDebugEnabled() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message, Throwable throwable) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInfoEnabled() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message, Throwable throwable) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public boolean isWarnEnabled() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message, Throwable throwable) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public boolean isErrorEnabled() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message) {
            /* empty */
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message, Throwable throwable) {
            /* empty */
        }
    }

    /**
     * A build logger that writes all statements to a {@link PrintStream}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class StreamWriting implements BuildLogger {

        /**
         * The target for writing statements.
         */
        private final PrintStream printStream;

        /**
         * Creates a new stream writing build logger.
         *
         * @param printStream The target for writing statements.
         */
        public StreamWriting(PrintStream printStream) {
            this.printStream = printStream;
        }

        /**
         * Creates a build logger that writes to {@link System#out}.
         *
         * @return A build logger that writes to {@link System#out}.
         */
        public static BuildLogger toSystemOut() {
            return new StreamWriting(System.out);
        }

        /**
         * Creates a build logger that writes to {@link System#err}.
         *
         * @return A build logger that writes to {@link System#err}.
         */
        public static BuildLogger toSystemError() {
            return new StreamWriting(System.err);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isDebugEnabled() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message) {
            printStream.print(message);
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message, Throwable throwable) {
            synchronized (printStream) {
                printStream.print(message);
                throwable.printStackTrace(printStream);
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInfoEnabled() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message) {
            printStream.print(message);
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message, Throwable throwable) {
            synchronized (printStream) {
                printStream.print(message);
                throwable.printStackTrace(printStream);
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isWarnEnabled() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message) {
            printStream.print(message);
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message, Throwable throwable) {
            synchronized (printStream) {
                printStream.print(message);
                throwable.printStackTrace(printStream);
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isErrorEnabled() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message) {
            printStream.print(message);
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message, Throwable throwable) {
            synchronized (printStream) {
                printStream.print(message);
                throwable.printStackTrace(printStream);
            }
        }
    }

    /**
     * A compound build logger.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements BuildLogger {

        /**
         * The build loggers to delegate to.
         */
        private final List<BuildLogger> buildLoggers;

        /**
         * Creates a new compound build logger.
         *
         * @param buildLogger The build loggers to delegate to.
         */
        public Compound(BuildLogger... buildLogger) {
            this(Arrays.asList(buildLogger));
        }

        /**
         * Creates a new compound build logger.
         *
         * @param buildLoggers The build loggers to delegate to.
         */
        public Compound(List<? extends BuildLogger> buildLoggers) {
            this.buildLoggers = new ArrayList<BuildLogger>();
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger instanceof Compound) {
                    this.buildLoggers.addAll(((Compound) buildLogger).buildLoggers);
                } else if (!(buildLogger instanceof NoOp)) {
                    this.buildLoggers.add(buildLogger);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isDebugEnabled() {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isDebugEnabled()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message) {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isDebugEnabled()) {
                    buildLogger.debug(message);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message, Throwable throwable) {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isDebugEnabled()) {
                    buildLogger.debug(message, throwable);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInfoEnabled() {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isInfoEnabled()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message) {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isInfoEnabled()) {
                    buildLogger.info(message);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message, Throwable throwable) {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isInfoEnabled()) {
                    buildLogger.info(message, throwable);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isWarnEnabled() {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isWarnEnabled()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message) {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isWarnEnabled()) {
                    buildLogger.warn(message);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message, Throwable throwable) {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isWarnEnabled()) {
                    buildLogger.warn(message, throwable);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isErrorEnabled() {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isErrorEnabled()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message) {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isErrorEnabled()) {
                    buildLogger.error(message);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message, Throwable throwable) {
            for (BuildLogger buildLogger : buildLoggers) {
                if (buildLogger.isErrorEnabled()) {
                    buildLogger.error(message, throwable);
                }
            }
        }
    }
}
