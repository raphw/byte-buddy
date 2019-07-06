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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
    @SuppressWarnings("unused")
    void loadAgent(String jarFile, String argument) throws IOException;

    /**
     * Detaches this virtual machine representation.
     *
     * @throws IOException If an I/O exception occurs.
     */
    @SuppressWarnings("unused")
    void detach() throws IOException;

    /**
     * A virtual machine implementation for a HotSpot VM or any compatible VM.
     */
    abstract class ForHotSpot implements VirtualMachine {

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
         * The target process's id.
         */
        protected final String processId;

        /**
         * Creates a new HotSpot-compatible VM implementation.
         *
         * @param processId The target process's id.
         */
        protected ForHotSpot(String processId) {
            this.processId = processId;
        }

        /**
         * {@inheritDoc}
         */
        public void loadAgent(String jarFile, String argument) throws IOException {
            connect();
            write(PROTOCOL_VERSION.getBytes(UTF_8));
            write(BLANK);
            write(LOAD_COMMAND.getBytes(UTF_8));
            write(BLANK);
            write(INSTRUMENT_COMMAND.getBytes(UTF_8));
            write(BLANK);
            write(Boolean.FALSE.toString().getBytes(UTF_8));
            write(BLANK);
            write((argument == null
                    ? jarFile
                    : jarFile + ARGUMENT_DELIMITER + argument).getBytes(UTF_8));
            write(BLANK);
            byte[] buffer = new byte[1];
            StringBuilder stringBuilder = new StringBuilder();
            int length;
            while ((length = read(buffer)) != -1) {
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
                    while ((length = read(buffer)) != -1) {
                        stringBuilder.append(new String(buffer, 0, length, UTF_8));
                    }
                    throw new IllegalStateException(stringBuilder.toString());
            }
        }

        /**
         * Connects to the target VM.
         *
         * @throws IOException If an I/O exception occurs.
         */
        protected abstract void connect() throws IOException;

        /**
         * Reads from the communication channel.
         *
         * @param buffer The buffer to read into.
         * @return The amount of bytes read.
         * @throws IOException If an I/O exception occurs.
         */
        protected abstract int read(byte[] buffer) throws IOException;

        /**
         * Writes to the communication channel.
         *
         * @param buffer The buffer to write from.
         * @throws IOException If an I/O exception occurs.
         */
        protected abstract void write(byte[] buffer) throws IOException;

        /**
         * A virtual machine implementation for a HotSpot VM running on Unix.
         */
        public static class OnPosix extends ForHotSpot {

            /**
             * The default amount of attempts to connect.
             */
            private static final int DEFAULT_ATTEMPTS = 10;

            /**
             * The default pause between two attempts.
             */
            private static final long DEFAULT_PAUSE = 200;

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

            private final Object socket;

            /**
             * The number of attempts to connect.
             */
            private final int attempts;

            /**
             * The time to pause between attempts.
             */
            private final long pause;

            /**
             * The time unit of the pause time.
             */
            private final TimeUnit timeUnit;

            private int address;

            /**
             * Creates a new VM implementation for a HotSpot VM running on Unix.
             *
             * @param processId The process id of the target VM.
             * @param socket    The Unix socket to use for communication.
             * @param attempts  The number of attempts to connect.
             * @param pause     The pause time between two checks against the target VM.
             * @param timeUnit  The time unit of the pause time.
             */
            public OnPosix(String processId, Object socket, int attempts, long pause, TimeUnit timeUnit) {
                super(processId);
                this.socket = socket;
                this.attempts = attempts;
                this.pause = pause;
                this.timeUnit = timeUnit;
            }

            /**
             * Asserts the availability of this virtual machine implementation. If the Unix socket library is missing or
             * if this VM does not support Unix socket communication, a {@link Throwable} is thrown.
             *
             * @return This virtual machine type.
             * @throws Throwable If this attachment method is not available.
             */
            public static Class<?> assertAvailability() throws Throwable {
                try {
                    Class<?> moduleType = Class.forName("java.lang.Module");
                    Method getModule = Class.class.getMethod("getModule"), canRead = moduleType.getMethod("canRead", moduleType);
                    Object thisModule = getModule.invoke(OnPosix.class), otherModule = getModule.invoke(Platform.class);
                    if (!(Boolean) canRead.invoke(thisModule, otherModule)) {
                        moduleType.getMethod("addReads", moduleType).invoke(thisModule, otherModule);
                    }
                    return doAssertAvailability();
                } catch (ClassNotFoundException ignored) {
                    return doAssertAvailability();
                }
            }

            /**
             * Asserts the availability of this virtual machine implementation.
             *
             * @return This virtual machine type.
             */
            private static Class<?> doAssertAvailability() {
                if (Platform.isWindows() || Platform.isWindowsCE()) {
                    throw new IllegalStateException("POSIX sockets are not supported on the current system");
                } else if (!System.getProperty("java.vm.name").toLowerCase(Locale.US).contains("hotspot")) {
                    throw new IllegalStateException("Cannot apply attachment on non-Hotspot compatible VM");
                } else {
                    return OnPosix.class;
                }
            }

            /**
             * Attaches to the supplied VM process.
             *
             * @param processId The process id of the target VM.
             * @return An appropriate virtual machine implementation.
             * @throws IOException If an I/O exception occurs.
             */
            public static VirtualMachine attach(String processId) throws IOException {
                return new OnPosix(processId, Native.loadLibrary("c", JNAUnixSocket.class), DEFAULT_ATTEMPTS, DEFAULT_PAUSE, TimeUnit.MILLISECONDS);
            }

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME", justification = "This is a Unix-specific implementation")
            protected void connect() throws IOException {
                File socketFile = new File(TEMPORARY_DIRECTORY, SOCKET_FILE_PREFIX + processId);
                if (!socketFile.exists()) {
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
                        while (attempts-- > 0 && !socketFile.exists()) {
                            Thread.sleep(timeUnit.toMillis(pause));
                        }
                        if (!socketFile.exists()) {
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
                JNAUnixSocket socket = (JNAUnixSocket) this.socket;
                address = socket.socket(1, 1, 0);
                if (address == 0) {
                    throw new IllegalStateException();
                }
                JNAUnixSocket.SockAddr sockAddr = new JNAUnixSocket.SockAddr();
                sockAddr.setPath(socketFile.getAbsolutePath());
                if (socket.connect(address, sockAddr, sockAddr.size()) != 0) {
                    throw new IllegalStateException();
                }
            }

            /**
             * {@inheritDoc}
             */
            public int read(byte[] buffer) throws IOException {
                return ((JNAUnixSocket) this.socket).read(address, ByteBuffer.wrap(buffer), buffer.length);
            }

            /**
             * {@inheritDoc}
             */
            public void write(byte[] buffer) throws IOException {
                int write = ((JNAUnixSocket) this.socket).write(address, ByteBuffer.wrap(buffer), buffer.length);
                if (write != buffer.length) {
                    throw new IllegalStateException();
                }
            }

            /**
             * {@inheritDoc}
             */
            public void detach() throws IOException {
                ((JNAUnixSocket) this.socket).close(address);
            }

            interface JNAUnixSocket extends Library {

                class SockAddr extends Structure {

                    public short family = 1;

                    public byte[] path = new byte[100];

                    public void setPath(String path) {
                        System.arraycopy(path.getBytes(), 0, this.path, 0, path.length());
                        System.arraycopy(new byte[]{0}, 0, this.path, path.length(), 1);
                    }

                    @Override
                    protected List<String> getFieldOrder() {
                        return Arrays.asList("family", "path");
                    }
                }

                int socket(int domain, int type, int protocol);

                int connect(int sockfd, SockAddr sockaddr, int addrlen);

                int read(int fd, ByteBuffer buffer, int count);

                int write(int fd, ByteBuffer buffer, int count);

                int close(int fd);

                String strerror(int errno);
            }
        }
    }
}
