package net.bytebuddy.description;

import net.bytebuddy.description.annotation.AnnotatedElement;
import net.bytebuddy.description.type.DeclaredInType;
import net.bytebuddy.description.type.TypeDescription;

/**
 * Implementations describe an element represented in byte code, i.e. a type, a field or a method or a constructor.
 */
public interface ByteCodeElement extends NamedElement, ModifierReviewable, DeclaredInType, AnnotatedElement {

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
