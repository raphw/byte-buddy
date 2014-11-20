package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.ByteCodeElement;

public class DescriptorMatcher<T extends ByteCodeElement> extends ElementMatcher.Junction.AbstractBase<T> {

    private final ElementMatcher<String> descriptorMatcher;

    public DescriptorMatcher(ElementMatcher<String> descriptorMatcher) {
        this.descriptorMatcher = descriptorMatcher;
    }

    @Override
    public boolean matches(T target) {
        return descriptorMatcher.matches(target.getDescriptor());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && descriptorMatcher.equals(((DescriptorMatcher) other).descriptorMatcher);
    }

    @Override
    public int hashCode() {
        return descriptorMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "descriptor(" + descriptorMatcher + ")";
    }
}
