package org.objectweb.asm;

import net.bytebuddy.utility.privilege.GetSystemPropertyAction;

import java.security.AccessController;

/**
 * A {@link ClassReader} that does not apply a class file version check.
 */
public class OpenedClassReader extends ClassReader {

    /**
     * Indicates that Byte Buddy should not validate the maximum supported class file version.
     */
    public static final String EXPERIMENTAL_PROPERTY = "net.bytebuddy.experimental";

    /**
     * {@code true} if the class file version should be validated.
     */
    private static final boolean VALIDATE_CLASS_VERSION;

    /**
     * Indicates that an array should be read from the start.
     */
    private static final int FROM_START = 0;

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
        VALIDATE_CLASS_VERSION = !experimental;
    }

    /**
     * Creates a new opened class reader.
     *
     * @param binaryRepresentation The byte array containing the class file.
     */
    public OpenedClassReader(byte[] binaryRepresentation) {
        super(binaryRepresentation, FROM_START, VALIDATE_CLASS_VERSION);
    }
}
