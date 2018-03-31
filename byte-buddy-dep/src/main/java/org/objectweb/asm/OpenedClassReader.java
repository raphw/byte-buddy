package org.objectweb.asm;

/**
 * A {@link ClassReader} that does not apply a class file version check.
 */
public class OpenedClassReader extends ClassReader {

    /**
     * Creates a new opened class reader.
     *
     * @param binaryRepresentation The byte array containing the class file.
     */
    public OpenedClassReader(byte[] binaryRepresentation) {
        super(binaryRepresentation, 0, false);
    }
}
