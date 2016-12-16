package net.bytebuddy.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
         * The UTF-8 charset.
         */
        private static final Charset UTF_8 = Charset.forName("UTF-8");

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

        @Override
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
        public static class OnUnix extends ForHotSpot {

            /**
             * The default amount of attempts to connect.
             */
            private static final int DEFAULT_ATTEMPTS = 10;

            /**
             * The default pause between two attempts.
             */
            private static final long DEFAULT_PAUSE = 200;

            /**
             * The default socket timeout.
             */
            private static final long DEFAULT_TIMEOUT = 5000;

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
             * The Unix socket to use for communication. The containing object is supposed to be an instance
             * of {@link AFUNIXSocket} which is however not set to avoid eager loading
             */
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
             * The socket timeout.
             */
            private final long timeout;

            /**
             * The time unit of the pause time.
             */
            private final TimeUnit timeUnit;

            /**
             * Creates a new VM implementation for a HotSpot VM running on Unix.
             *
             * @param processId The process id of the target VM.
             * @param socket    The Unix socket to use for communication.
             * @param attempts  The number of attempts to connect.
             * @param pause     The pause time between two VMs.
             * @param timeout   The socket timeout.
             * @param timeUnit  The time unit of the pause time.
             */
            public OnUnix(String processId, Object socket, int attempts, long pause, long timeout, TimeUnit timeUnit) {
                super(processId);
                this.socket = socket;
                this.attempts = attempts;
                this.pause = pause;
                this.timeout = timeout;
                this.timeUnit = timeUnit;
            }

            /**
             * Asserts the availability of this virtual machine implementation. If the Unix socket library is missing or
             * if this VM does not support Unix socket communication, a {@link Throwable} is thrown.
             *
             * @return This virtual machine type.
             * @throws Throwable If this VM does not support POSIX sockets or is not running on a HotSpot VM.
             */
            public static Class<?> assertAvailability() throws Throwable {
                if (!AFUNIXSocket.isSupported()) {
                    throw new IllegalStateException("POSIX sockets are not supported on the current system");
                } else if (!System.getProperty("java.vm.name").toLowerCase(Locale.US).contains("hotspot")) {
                    throw new IllegalStateException("Cannot apply attachment on non-Hotspot compatible VM");
                } else {
                    return OnUnix.class;
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
                return new OnUnix(processId, AFUNIXSocket.newInstance(), DEFAULT_ATTEMPTS, DEFAULT_PAUSE, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            }

            @Override
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
                        throw new IllegalStateException("Interrupted during wait for process", exception);
                    } finally {
                        if (!attachFile.delete()) {
                            Logger.getLogger("net.bytebuddy").warning("Could not delete attach file: " + attachFile);
                        }
                    }
                }
                ((AFUNIXSocket) socket).setSoTimeout((int) timeUnit.toMillis(timeout));
                ((AFUNIXSocket) socket).connect(new AFUNIXSocketAddress(socketFile));
            }

            @Override
            public int read(byte[] buffer) throws IOException {
                return ((AFUNIXSocket) this.socket).getInputStream().read(buffer);
            }

            @Override
            public void write(byte[] buffer) throws IOException {
                ((AFUNIXSocket) this.socket).getOutputStream().write(buffer);
            }

            @Override
            public void detach() throws IOException {
                ((AFUNIXSocket) this.socket).close();
            }

            @Override
            public String toString() {
                return "VirtualMachine.ForHotSpot.OnUnix{" +
                        "processId=" + processId +
                        ", socket=" + socket +
                        ", attempts=" + attempts +
                        ", pause=" + pause +
                        ", timeout=" + timeout +
                        ", timeUnit=" + timeUnit +
                        '}';
            }
        }
    }
}
