package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that validates that a given byte code element is accessible to a given type.
 *
 * @param <T>The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class AccessibilityMatcher<T extends ByteCodeElement> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type that is to be checked for its viewing rights.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a matcher that validates that a byte code element can be seen by a given type.
     *
     * @param typeDescription The type that is to be checked for its viewing rights.
     */
    public AccessibilityMatcher(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    @Override
    public boolean matches(T target) {
        return target.isAccessibleTo(typeDescription);
    }

    @Override
    public String toString() {
        return "isAccessibleTo(" + typeDescription + ")";
    }
}
