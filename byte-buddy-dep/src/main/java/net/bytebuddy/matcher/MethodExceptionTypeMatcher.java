package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.util.List;

public class MethodExceptionTypeMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<? super List<? extends TypeDescription>> exceptionMatcher;

    public MethodExceptionTypeMatcher(ElementMatcher<? super List<? extends TypeDescription>> exceptionMatcher) {
        this.exceptionMatcher = exceptionMatcher;
    }

    @Override
    public boolean matches(T target) {
        return exceptionMatcher.matches(target.getExceptionTypes());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && exceptionMatcher.equals(((MethodExceptionTypeMatcher) other).exceptionMatcher);
    }

    @Override
    public int hashCode() {
        return exceptionMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "hasException(" + exceptionMatcher + ")";
    }
}
