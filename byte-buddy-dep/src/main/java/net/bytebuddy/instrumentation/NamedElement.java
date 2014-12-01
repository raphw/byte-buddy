package net.bytebuddy.instrumentation;

public interface NamedElement {

    /**
     * Returns the internalName of this byte code element.
     *
     * @return The internalName of this byte code element as visible from within a running Java application.
     */
    String getName();
}
