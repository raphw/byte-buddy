package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;

/**
 * An element matcher that matches a Java descriptor.
 *
 * @param <T> The type of the matched entity.
 */
public class DescriptorMatcher<T extends ByteCodeElement> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * A matcher to apply to the descriptor.
     */
    private final ElementMatcher<String> descriptorMatcher;

    /**
     * Creates a new matcher for an element's descriptor.
     *
     * @param descriptorMatcher A matcher to apply to the descriptor.
     */
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
                && descriptorMatcher.equals(((DescriptorMatcher<?>) other).descriptorMatcher);
    }

    @Override
    public int hashCode() {
        return descriptorMatcher.hashCode();
    }

    @Override
    public String toString() {
        return "hasDescriptor(" + descriptorMatcher + ")";
    }
}
