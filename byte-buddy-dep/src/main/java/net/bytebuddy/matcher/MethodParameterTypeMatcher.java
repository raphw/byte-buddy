package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.type.TypeList;

/**
 * An element matcher that matches a method's parameter types.
 *
 * @param <T> The type of the matched entity.
 */
public class MethodParameterTypeMatcher<T extends ParameterList> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the parameter types.
     */
    private final ElementMatcher<? super TypeList> parameterMatcher;

    /**
     * Creates a new matcher for a method's parameter types.
     *
     * @param parameterMatcher The matcher to apply to the parameter types.
     */
    public MethodParameterTypeMatcher(ElementMatcher<? super TypeList> parameterMatcher) {
        this.parameterMatcher = parameterMatcher;
    }

    @Override
    public boolean matches(T target) {
        return parameterMatcher.matches(target.asTypeList());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && parameterMatcher.equals(((MethodParameterTypeMatcher<?>) other).parameterMatcher);
    }

    @Override
    public int hashCode() {
        return parameterMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "types(" + parameterMatcher + ")";
    }
}
