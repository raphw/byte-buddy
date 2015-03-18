package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeList;

/**
 * An element matcher that matches a method' parameter types.
 *
 * @param <T> The type of the matched entity.
 */
public class MethodParameterTypesMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the parameter types.
     */
    private final ElementMatcher<? super TypeList> parameterMatcher;

    /**
     * Creates a new matcher for a method's parameter types.
     *
     * @param parameterMatcher The matcher to apply to the parameter types.
     */
    public MethodParameterTypesMatcher(ElementMatcher<? super TypeList> parameterMatcher) {
        this.parameterMatcher = parameterMatcher;
    }

    @Override
    public boolean matches(T target) {
        return parameterMatcher.matches(target.getParameters().asTypeList());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && parameterMatcher.equals(((MethodParameterTypesMatcher) other).parameterMatcher);
    }

    @Override
    public int hashCode() {
        return parameterMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "parameters(" + parameterMatcher + ")";
    }
}
