package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;

public class MethodReturnTypeMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription> {

    private final ElementMatcher<? super TypeDescription> typeMatcher;

    public MethodReturnTypeMatcher(ElementMatcher<? super TypeDescription> typeMatcher) {
        this.typeMatcher = typeMatcher;
    }

    @Override
    public boolean matches(MethodDescription target) {
        return typeMatcher.matches(target.getReturnType());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typeMatcher.equals(((MethodReturnTypeMatcher) other).typeMatcher);
    }

    @Override
    public int hashCode() {
        return typeMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "returns(" + typeMatcher + ");";
    }
}
