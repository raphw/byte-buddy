package net.bytebuddy.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * A utility for draining the contents of an {@link java.io.InputStream} into a {@code byte} array.
 */
public class StreamDrainer {

    /**
     * The default size of the buffer for draining a stream.
     */
    public static final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * A convenience constant referring to the value representing the end of a stream.
     */
    private static final int END_OF_STREAM = -1;

    /**
     * A convenience constant referring to the value representing the start of a stream.
     */
    private static final int FROM_BEGINNING = 0;

    /**
     * The buffer size for reading from a given stream.
     */
    private final int bufferSize;

    /**
     * Creates a stream drainer with the default buffer size.
     */
    public StreamDrainer() {
        this(DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a stream drainer with the given buffer size.
     *
     * @param bufferSize The buffer size for reading from a given stream.
     */
    public StreamDrainer(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Drains an input stream into a byte array. The given input stream is not closed.
     *
     * @param inputStream The input stream to drain.
     * @return A byte array containing the content of the input stream.
     * @throws IOException If the stream reading causes an error.
     */
    public byte[] drain(InputStream inputStream) throws IOException {
        List<byte[]> previousBytes = new LinkedList<byte[]>();
        byte[] currentArray = new byte[bufferSize];
        int currentIndex = 0;
        int currentRead;
        do {
            currentRead = inputStream.read(currentArray, currentIndex, bufferSize - currentIndex);
            currentIndex += currentRead > 0 ? currentRead : 0;
            if (currentIndex == bufferSize) {
                previousBytes.add(currentArray);
                currentArray = new byte[bufferSize];
                currentIndex = 0;
            }
        } while (currentRead != END_OF_STREAM);
        byte[] result = new byte[previousBytes.size() * bufferSize + currentIndex];
        int arrayIndex = 0;
        for (byte[] previousByte : previousBytes) {
            System.arraycopy(previousByte, FROM_BEGINNING, result, arrayIndex++ * bufferSize, bufferSize);
        }
        System.arraycopy(currentArray, FROM_BEGINNING, result, arrayIndex * bufferSize, currentIndex);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && bufferSize == ((StreamDrainer) other).bufferSize;
    }

    @Override
    public int hashCode() {
        return bufferSize;
    }

    @Override
    public String toString() {
        return "StreamDrainer{bufferSize=" + bufferSize + '}';
    }
}
