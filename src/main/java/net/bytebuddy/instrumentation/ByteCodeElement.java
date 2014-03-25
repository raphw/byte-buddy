package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.type.TypeDescription;

/**
 * Implementations describe an element represented in byte code, i.e. a type, a field or a method or a constructor.
 */
public interface ByteCodeElement {

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
     * Returns the descriptor of this byte code element.
     *
     * @return The descriptor of this byte code element.
     */
    String getDescriptor();

    /**
     * Checks if this element is visible from a given type.
     *
     * @param typeDescription The type which is checked for its access of this element.
     * @return {@code true} if this element is visible for {@code typeDescription}.
     */
    boolean isVisibleTo(TypeDescription typeDescription);
}
