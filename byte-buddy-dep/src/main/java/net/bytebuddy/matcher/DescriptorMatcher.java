package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.ByteCodeElement;

/**
 * An element matcher that matches a Java descriptor.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class DescriptorMatcher<T extends ByteCodeElement> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * A matcher to apply to the descriptor.
     */
    private final ElementMatcher<String> matcher;

    /**
     * Creates a new matcher for an element's descriptor.
     *
     * @param matcher A matcher to apply to the descriptor.
     */
    public DescriptorMatcher(ElementMatcher<String> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getDescriptor());
    }

    @Override
    public String toString() {
        return "hasDescriptor(" + matcher + ")";
    }
}
