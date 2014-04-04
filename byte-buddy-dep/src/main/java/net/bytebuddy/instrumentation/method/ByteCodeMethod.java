package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.ByteCodeElement;

/**
 * A byte code method is a representation of a method on byte code level where no distinctions is made between methods
 * and constructors.
 */
public interface ByteCodeMethod extends ByteCodeElement {

    /**
     * Returns the unique signature of a byte code method. A unique signature is usually a concatenation of
     * the internal internalName of the method / constructor and the method descriptor. Note that methods on byte code
     * level do consider two similar methods with different return type as distinct methods.
     *
     * @return A unique signature of this byte code level method.
     */
    String getUniqueSignature();
}
