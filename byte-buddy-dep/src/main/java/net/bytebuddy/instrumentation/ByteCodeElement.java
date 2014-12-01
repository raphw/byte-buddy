package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.type.DeclaredInType;
import net.bytebuddy.instrumentation.type.TypeDescription;

/**
 * Implementations describe an element represented in byte code, i.e. a type, a field or a method or a constructor.
 */
public interface ByteCodeElement extends NamedElement, ModifierReviewable, DeclaredInType, AnnotatedElement {

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
     * with internal names when calling {@link net.bytebuddy.instrumentation.ByteCodeElement#getName()}.</li>
     * <li>For method descriptions, representations of constructors and the type initializer, return the
     * empty string.</li>
     * </ul>
     *
     * @return The name of this type as represented in Java source code.
     */
    String getSourceCodeName();

    /**
     * Returns the descriptor of this byte code element.
     *
     * @return The descriptor of this byte code element.
     */
    String getDescriptor();

    /**
     * Returns the generic signature of this byte code element.
     *
     * @return The generic signature or {@code null} if this element is not generic.
     */
    String getGenericSignature();

    /**
     * Checks if this element is visible from a given type.
     *
     * @param typeDescription The type which is checked for its access of this element.
     * @return {@code true} if this element is visible for {@code typeDescription}.
     */
    boolean isVisibleTo(TypeDescription typeDescription);
}
