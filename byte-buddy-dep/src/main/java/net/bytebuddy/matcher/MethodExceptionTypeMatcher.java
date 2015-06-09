package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;

/**
 * An element matcher that matches the exceptions that are declared by a method.
 *
 * @param <T> The type of the matched entity.
 */
public class MethodExceptionTypeMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the matched method's exceptions.
     */
    private final ElementMatcher<? super GenericTypeList> exceptionMatcher;

    /**
     * Creates a new matcher for a method's exceptions.
     *
     * @param exceptionMatcher The matcher to apply to the matched method's exceptions.
     */
    public MethodExceptionTypeMatcher(ElementMatcher<? super GenericTypeList> exceptionMatcher) {
        this.exceptionMatcher = exceptionMatcher;
    }

    @Override
    public boolean matches(T target) {
        return exceptionMatcher.matches(target.getExceptionTypes());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && exceptionMatcher.equals(((MethodExceptionTypeMatcher<?>) other).exceptionMatcher);
    }

    @Override
    public int hashCode() {
        return exceptionMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "exceptions(" + exceptionMatcher + ")";
    }
}
