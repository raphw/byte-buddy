package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.util.List;

public class MethodParameterTypeMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super List<? extends TypeDescription>> parameterMatcher;

    public MethodParameterTypeMatcher(ElementMatcher<? super List<? extends TypeDescription>> parameterMatcher) {
        this.parameterMatcher = parameterMatcher;
    }

    @Override
    public boolean matches(T target) {
        return parameterMatcher.matches(target.getParameterTypes());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && parameterMatcher.equals(((MethodParameterTypeMatcher) other).parameterMatcher);
    }

    @Override
    public int hashCode() {
        return parameterMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "takesArguments(" + parameterMatcher + ")";
    }
}
