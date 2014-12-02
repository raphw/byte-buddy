package net.bytebuddy.instrumentation;

/**
 * Represents a Java element with a name.
 */
public interface NamedElement {

    /**
     * Returns the internalName of this byte code element.
     *
     * @return The internalName of this byte code element as visible from within a running Java application.
     */
    String getName();
}
