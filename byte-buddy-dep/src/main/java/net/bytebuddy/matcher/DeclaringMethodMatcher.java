package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

/**
 * An element matcher that checks if a type description declares methods of a given property.
 *
 * @param <T> The exact type of the annotated element that is matched.
 */
public class DeclaringMethodMatcher<T extends GenericTypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The field matcher to apply to the declared fields of the matched type description.
     */
    private final ElementMatcher<? super MethodList<?>> methodMatcher;

    /**
     * Creates a new matcher for a type's declared methods.
     *
     * @param methodMatcher The method matcher to apply to the declared methods of the matched type description.
     */
    public DeclaringMethodMatcher(ElementMatcher<? super MethodList<? extends MethodDescription>> methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    @Override
    public boolean matches(T target) {
        return methodMatcher.matches(target.getDeclaredMethods());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && methodMatcher.equals(((DeclaringMethodMatcher<?>) other).methodMatcher);
    }

    @Override
    public int hashCode() {
        return methodMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "declaresMethods(" + methodMatcher + ")";
    }
}
