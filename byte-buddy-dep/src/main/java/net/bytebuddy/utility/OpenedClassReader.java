package net.bytebuddy.utility;

import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.security.AccessController;

/**
 * A {@link ClassReader} that does not apply a class file version check if the {@code net.bytebuddy.experimental} property is set.
 */
public class OpenedClassReader {

    /**
     * Indicates that Byte Buddy should not validate the maximum supported class file version.
     */
    public static final String EXPERIMENTAL_PROPERTY = "net.bytebuddy.experimental";

    /**
     * {@code true} if Byte Buddy is executed in experimental mode.
     */
    public static final boolean EXPERIMENTAL;

    /*
     * Checks the experimental property.
     */
    static {
        boolean experimental;
        try {
            experimental = Boolean.parseBoolean(AccessController.doPrivileged(new GetSystemPropertyAction(EXPERIMENTAL_PROPERTY)));
        } catch (Exception ignored) {
            experimental = false;
        }
        EXPERIMENTAL = experimental;
    }

    /**
     * Creates a class reader for the given binary representation of a class file.
     *
     * @param binaryRepresentation The binary representation of a class file to read.
     * @return An appropriate class reader.
     */
    public static ClassReader of(byte[] binaryRepresentation) {
        if (EXPERIMENTAL) {
            byte[] actualVersion = new byte[]{binaryRepresentation[4], binaryRepresentation[5], binaryRepresentation[6], binaryRepresentation[7]};
            binaryRepresentation[4] = (byte) (Opcodes.V10 >>> 24);
            binaryRepresentation[5] = (byte) (Opcodes.V10 >>> 16);
            binaryRepresentation[6] = (byte) (Opcodes.V10 >>> 8);
            binaryRepresentation[7] = (byte) Opcodes.V10;
            ClassReader classReader = new ClassReader(binaryRepresentation);
            System.arraycopy(actualVersion, 0, binaryRepresentation, 4, actualVersion.length);
            return classReader;
        } else {
            return new ClassReader(binaryRepresentation);
        }
    }

    /**
     * Not intended for construction.
     */
    private OpenedClassReader() {
        throw new UnsupportedOperationException("This is a utility class that should not be invoked");
    }
}
