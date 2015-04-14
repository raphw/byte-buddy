package net.bytebuddy.description;

/**
 * Represents a Java element with a name.
 */
public interface NamedElement {

    /**
     * Represents an element without a name in the source code.
     */
    String EMPTY_NAME = "";

    /**
     * Returns the internalName of this byte code element.
     *
     * @return The internalName of this byte code element as visible from within a running Java application.
     */
    String getName();

    /**
     * Returns the internal internalName of this byte code element.
     *
     * @return The internal internalName of this byte code element as used within the Java class file format.
     */
    String getInternalName();

    /**
     * Returns the name of this byte code element as it is defined in Java source code. This means:
     * <ul>
     * <li>For type descriptions, the main distinction is the display of arrays whose actual names are blended
     * with internal names when calling {@link ByteCodeElement#getName()}.</li>
     * <li>For method descriptions, representations of constructors and the type initializer, return the
     * empty string.</li>
     * </ul>
     *
     * @return The name of this type as represented in Java source code.
     */
    String getSourceCodeName();
}
