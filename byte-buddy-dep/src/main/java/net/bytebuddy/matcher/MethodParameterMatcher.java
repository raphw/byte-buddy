package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterList;

/**
 * An element matcher that matches a method's parameters.
 *
 * @param <T> The type of the matched entity.
 */
public class MethodParameterMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the parameters.
     */
    private final ElementMatcher<? super ParameterList> parameterMatcher;

    /**
     * Creates a new matcher for a method's parameters.
     *
     * @param parameterMatcher The matcher to apply to the parameters.
     */
    public MethodParameterMatcher(ElementMatcher<? super ParameterList> parameterMatcher) {
        this.parameterMatcher = parameterMatcher;
    }

    @Override
    public boolean matches(T target) {
        return parameterMatcher.matches(target.getParameters());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && parameterMatcher.equals(((MethodParameterMatcher<?>) other).parameterMatcher);
    }

    @Override
    public int hashCode() {
        return parameterMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "hasParameter(" + parameterMatcher + ")";
    }
}
