/*
 * Copyright 2014 - 2019 Rafael Winterhalter
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
package net.bytebuddy.agent;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * An implementation for attachment on a virtual machine. This interface mimics the tooling API's virtual
 * machine interface to allow for similar usage by {@link ByteBuddyAgent} where all calls are made via
 * reflection such that this structural typing suffices for interoperability.
 * </p>
 * <p>
 * <b>Note</b>: Implementations are required to declare a static method {@code attach(String)} returning an
 * instance of a class that declares the methods defined by {@link VirtualMachine}.
 * </p>
 */
public interface VirtualMachine {

    /**
     * Loads an agent into the represented virtual machine.
     *
     * @param jarFile  The jar file to attach.
     * @param argument The argument to provide or {@code null} if no argument should be provided.
     * @throws IOException If an I/O exception occurs.
     */
    void loadAgent(String jarFile, String argument) throws IOException;

    /**
     * Loads a native agent into the represented virtual machine.
     *
     * @param library  The agent library.
     * @param argument The argument to provide or {@code null} if no argument should be provided.
     * @throws IOException If an I/O exception occurs.
     */
    void loadAgentPath(String library, String argument) throws IOException;

    /**
     * Detaches this virtual machine representation.
     *
     * @throws IOException If an I/O exception occurs.
     */
    void detach() throws IOException;

    /**
     * A virtual machine implementation for a HotSpot VM or any compatible VM.
     */
    class Default implements VirtualMachine {

        /**
         * The UTF-8 charset name.
         */
        private static final String UTF_8 = "UTF-8";

        /**
         * The protocol version to use for communication.
         */
        private static final String PROTOCOL_VERSION = "1";

        /**
         * The {@code load} command.
         */
        private static final String LOAD_COMMAND = "load";

        /**
         * The {@code instrument} command.
         */
        private static final String INSTRUMENT_COMMAND = "instrument";

        /**
         * A delimiter to be used for attachment.
         */
        private static final String ARGUMENT_DELIMITER = "=";

        /**
         * A blank line argument.
         */
        private static final byte[] BLANK = new byte[]{0};

        /**
         * The virtual machine connection.
         */
        private final Connection connection;

        /**
         * Creates a default virtual machine implementation.
         *
         * @param connection The virtual machine connection.
         */
        protected Default(Connection connection) {
            this.connection = connection;
        }

        /**
         * Asserts if this virtual machine type is available on the current VM.
         *
         * @return The virtual machine type if available.
         * @throws Throwable If this virtual machine is not available.
         */
        public static Class<?> assertAvailability() throws Throwable {
            if (Platform.isWindows() || Platform.isWindowsCE()) {
                throw new IllegalStateException("POSIX sockets are not available on Windows");
            } else {
                Class.forName(Native.class.getName()); // Attempt loading the JNA class to check availability.
                return Default.class;
            }
        }

        /**
         * Attaches to the supplied process id using the default JNA implementation.
         *
         * @param processId The process id.
         * @return A suitable virtual machine implementation.
         * @throws IOException If an IO exception occurs during establishing the connection.
         */
        public static VirtualMachine attach(String processId) throws IOException {
            return attach(processId, new Connection.ForJnaPosixSocket.Factory(10, 100, TimeUnit.MILLISECONDS));
        }

        /**
         * Attaches to the supplied process id using the supplied connection factory.
         *
         * @param processId         The process id.
         * @param connectionFactory The connection factory to use.
         * @return A suitable virtual machine implementation.
         * @throws IOException If an IO exception occurs during establishing the connection.
         */
        public static VirtualMachine attach(String processId, Connection.Factory connectionFactory) throws IOException {
            return new Default(connectionFactory.connect(processId));
        }

        /**
         * {@inheritDoc}
         */
        public void loadAgent(String jarFile, String argument) throws IOException {
            load(jarFile, false, argument);
        }

        /**
         * {@inheritDoc}
         */
        public void loadAgentPath(String library, String argument) throws IOException {
            load(library, true, argument);
        }

        /**
         * Loads an agent by the given command.
         *
         * @param file     The Java agent or library to be loaded.
         * @param isNative {@code true} if the agent is native.
         * @param argument The argument to the agent or {@code null} if no argument is given.
         * @throws IOException If an I/O exception occurs.
         */
        protected void load(String file, boolean isNative, String argument) throws IOException {
            connection.write(PROTOCOL_VERSION.getBytes(UTF_8));
            connection.write(BLANK);
            connection.write(LOAD_COMMAND.getBytes(UTF_8));
            connection.write(BLANK);
            connection.write(INSTRUMENT_COMMAND.getBytes(UTF_8));
            connection.write(BLANK);
            connection.write(Boolean.toString(isNative).getBytes(UTF_8));
            connection.write(BLANK);
            connection.write((argument == null
                    ? file
                    : file + ARGUMENT_DELIMITER + argument).getBytes(UTF_8));
            connection.write(BLANK);
            byte[] buffer = new byte[1];
            StringBuilder stringBuilder = new StringBuilder();
            int length;
            while ((length = connection.read(buffer)) != -1) {
                if (length > 0) {
                    if (buffer[0] == 10) {
                        break;
                    }
                    stringBuilder.append((char) buffer[0]);
                }
            }
            switch (Integer.parseInt(stringBuilder.toString())) {
                case 0:
                    return;
                case 101:
                    throw new IOException("Protocol mismatch with target VM");
                default:
                    buffer = new byte[1024];
                    stringBuilder = new StringBuilder();
                    while ((length = connection.read(buffer)) != -1) {
                        stringBuilder.append(new String(buffer, 0, length, UTF_8));
                    }
                    throw new IllegalStateException(stringBuilder.toString());
            }
        }

        /**
         * {@inheritDoc}
         */
        public void detach() throws IOException {
            connection.close();
        }

        /**
         * Represents a connection to a virtual machine.
         */
        public interface Connection extends Closeable {

            /**
             * Reads from the connected virtual machine.
             *
             * @param buffer The buffer to read from.
             * @return The amount of bytes that were read.
             * @throws IOException If an I/O exception occurs during reading.
             */
            int read(byte[] buffer) throws IOException;

            /**
             * Writes to the connected virtual machine.
             *
             * @param buffer The buffer to write to.
             * @throws IOException If an I/O exception occurs during writing.
             */
            void write(byte[] buffer) throws IOException;

            /**
             * A factory for creating connections to virtual machines.
             */
            interface Factory {

                /**
                 * Connects to the supplied process.
                 *
                 * @param processId The process id.
                 * @return The connection to the virtual machine with the supplied process id.
                 * @throws IOException If an I/O exception occurs during connecting to the targeted VM.
                 */
                Connection connect(String processId) throws IOException;

                /**
                 * A factory for attaching on a POSIX-compatible VM.
                 */
                abstract class ForPosixSocket implements Factory {

                    /**
                     * The temporary directory on Unix systems.
                     */
                    private static final String TEMPORARY_DIRECTORY = "/tmp";

                    /**
                     * The name prefix for a socket.
                     */
                    private static final String SOCKET_FILE_PREFIX = ".java_pid";

                    /**
                     * The name prefix for an attachment file indicator.
                     */
                    private static final String ATTACH_FILE_PREFIX = ".attach_pid";

                    /**
                     * The maximum amount of attempts to establish a POSIX socket connection to the target VM.
                     */
                    private final int attempts;

                    /**
                     * The pause between two checks for an existing socket.
                     */
                    private final long pause;

                    /**
                     * The time unit of the pause time.
                     */
                    private final TimeUnit timeUnit;

                    /**
                     * Creates a new connection factory for creating a connection to a JVM via a POSIX socket.
                     *
                     * @param attempts The maximum amount of attempts to establish a POSIX socket connection to the target VM.
                     * @param pause    The pause between two checks for an existing socket.
                     * @param timeUnit The time unit of the pause time.
                     */
                    protected ForPosixSocket(int attempts, long pause, TimeUnit timeUnit) {
                        this.attempts = attempts;
                        this.pause = pause;
                        this.timeUnit = timeUnit;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME", justification = "File name convention is specified.")
                    public Connection connect(String processId) throws IOException {
                        File socket = new File(TEMPORARY_DIRECTORY, SOCKET_FILE_PREFIX + processId);
                        if (!socket.exists()) {
                            String target = ATTACH_FILE_PREFIX + processId, path = "/proc/" + processId + "/cwd/" + target;
                            File attachFile = new File(path);
                            try {
                                if (!attachFile.createNewFile() && !attachFile.isFile()) {
                                    throw new IllegalStateException("Could not create attach file: " + attachFile);
                                }
                            } catch (IOException ignored) {
                                attachFile = new File(TEMPORARY_DIRECTORY, target);
                                if (!attachFile.createNewFile() && !attachFile.isFile()) {
                                    throw new IllegalStateException("Could not create attach file: " + attachFile);
                                }
                            }
                            try {
                                // The HotSpot attachment API attempts to send the signal to all children of a process
                                Process process = Runtime.getRuntime().exec("kill -3 " + processId);
                                int attempts = this.attempts;
                                boolean killed = false;
                                do {
                                    try {
                                        if (process.exitValue() != 0) {
                                            throw new IllegalStateException("Error while sending signal to target VM: " + processId);
                                        }
                                        killed = true;
                                        break;
                                    } catch (IllegalThreadStateException ignored) {
                                        attempts -= 1;
                                        Thread.sleep(timeUnit.toMillis(pause));
                                    }
                                } while (attempts > 0);
                                if (!killed) {
                                    throw new IllegalStateException("Target VM did not respond to signal: " + processId);
                                }
                                attempts = this.attempts;
                                while (attempts-- > 0 && !socket.exists()) {
                                    Thread.sleep(timeUnit.toMillis(pause));
                                }
                                if (!socket.exists()) {
                                    throw new IllegalStateException("Target VM did not respond: " + processId);
                                }
                            } catch (InterruptedException exception) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("Interrupted during wait for process", exception);
                            } finally {
                                if (!attachFile.delete()) {
                                    attachFile.deleteOnExit();
                                }
                            }
                        }
                        return doConnect(socket);
                    }

                    /**
                     * Connects to the supplied POSIX socket.
                     *
                     * @param socket The socket to connect to.
                     * @return An active connection to the supplied socket.
                     * @throws IOException If an error occurs during connection.
                     */
                    protected abstract Connection doConnect(File socket) throws IOException;
                }
            }

            /**
             * Implements a connection for a Posix socket in JNA.
             */
            class ForJnaPosixSocket implements Connection {

                /**
                 * The JNA library to use.
                 */
                private final PosixSocketLibrary library;

                /**
                 * The socket's handle.
                 */
                private final int handle;

                /**
                 * Creates a new connection for a Posix socket.
                 *
                 * @param library The JNA library to use.
                 * @param handle  The socket's handle.
                 */
                protected ForJnaPosixSocket(PosixSocketLibrary library, int handle) {
                    this.library = library;
                    this.handle = handle;
                }

                /**
                 * {@inheritDoc}
                 */
                public int read(byte[] buffer) {
                    return library.read(handle, ByteBuffer.wrap(buffer), buffer.length);
                }

                /**
                 * {@inheritDoc}
                 */
                public void write(byte[] buffer) {
                    int write = library.write(handle, ByteBuffer.wrap(buffer), buffer.length);
                    if (write != buffer.length) {
                        throw new IllegalStateException("Could not write entire buffer to socket");
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    library.close(handle);
                }

                /**
                 * A JNA library binding for Posix sockets.
                 */
                protected interface PosixSocketLibrary extends Library {

                    /**
                     * Represents an address for a POSIX socket.
                     */
                    class SocketAddress extends Structure {

                        /**
                         * The socket family.
                         */
                        @SuppressWarnings("unused")
                        @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Field required by native implementation.")
                        public short family = 1;

                        /**
                         * The socket path.
                         */
                        public byte[] path = new byte[100];

                        /**
                         * Sets the socket path.
                         *
                         * @param path The socket path.
                         */
                        public void setPath(String path) {
                            try {
                                System.arraycopy(path.getBytes("UTF-8"), 0, this.path, 0, path.length());
                                System.arraycopy(new byte[]{0}, 0, this.path, path.length(), 1);
                            } catch (UnsupportedEncodingException exception) {
                                throw new IllegalStateException(exception);
                            }
                        }

                        @Override
                        protected List<String> getFieldOrder() {
                            return Arrays.asList("family", "path");
                        }
                    }

                    /**
                     * Creates a POSIX socket connection.
                     *
                     * @param domain   The socket's domain.
                     * @param type     The socket's type.
                     * @param protocol The protocol version.
                     * @return A handle to the socket that was created or {@code 0} if no socket could be created.
                     */
                    int socket(int domain, int type, int protocol);

                    /**
                     * Connects a socket connection.
                     *
                     * @param handle  The socket's handle.
                     * @param address The address of the POSIX socket.
                     * @param length  The length of the socket value.
                     * @return {@code 0} if the socket was connected or an error code.
                     */
                    int connect(int handle, SocketAddress address, int length);

                    /**
                     * Reads from a POSIX socket.
                     *
                     * @param handle The socket's handle.
                     * @param buffer The buffer to read from.
                     * @param count  The bytes being read.
                     * @return The amount of bytes that could be read.
                     */
                    int read(int handle, ByteBuffer buffer, int count);

                    /**
                     * Writes to a POSIX socket.
                     *
                     * @param handle The socket's handle.
                     * @param buffer The buffer to write to.
                     * @param count  The bytes being written.
                     * @return The amount of bytes that could be written.
                     */
                    int write(int handle, ByteBuffer buffer, int count);

                    /**
                     * Closes the socket connection.
                     *
                     * @param handle The handle of the connection.
                     * @return {@code 0} if the socket was closed or an error code.
                     */
                    int close(int handle);
                }

                /**
                 * A factory for a POSIX socket connection to a JVM using JNA.
                 */
                public static class Factory extends Connection.Factory.ForPosixSocket {

                    /**
                     * The socket library API.
                     */
                    private final ForJnaPosixSocket.PosixSocketLibrary library;

                    /**
                     * Creates a new connection factory for creating a connection to a JVM via a POSIX socket using JNA.
                     *
                     * @param attempts The maximum amount of attempts to establish a POSIX socket connection to the target VM.
                     * @param pause    The pause between two checks for an existing socket.
                     * @param timeUnit The time unit of the pause time.
                     */
                    public Factory(int attempts, long pause, TimeUnit timeUnit) {
                        super(attempts, pause, timeUnit);
                        library = Native.load("c", ForJnaPosixSocket.PosixSocketLibrary.class);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Connection doConnect(File socket) {
                        int handle = library.socket(1, 1, 0);
                        if (handle == 0) {
                            throw new IllegalStateException("Could not open POSIX socket");
                        }
                        ForJnaPosixSocket.PosixSocketLibrary.SocketAddress address = new ForJnaPosixSocket.PosixSocketLibrary.SocketAddress();
                        address.setPath(socket.getAbsolutePath());
                        if (library.connect(handle, address, address.size()) != 0) {
                            throw new IllegalStateException("Could not connect to POSIX socket on " + socket);
                        }
                        return new Connection.ForJnaPosixSocket(library, handle);
                    }
                }
            }
        }
    }
}
