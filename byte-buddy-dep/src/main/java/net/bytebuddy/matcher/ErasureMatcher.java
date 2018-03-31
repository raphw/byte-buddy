package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches its argument's {@link TypeDescription.Generic} raw type against the
 * given matcher for a {@link TypeDescription}. As a wildcard does not define an erasure, a runtime
 * exception is thrown when this matcher is applied to a wildcard.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ErasureMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the raw type of the matched element.
     */
    private final ElementMatcher<? super TypeDescription> matcher;

    /**
     * Creates a new raw type matcher.
     *
     * @param matcher The matcher to apply to the raw type.
     */
    public ErasureMatcher(ElementMatcher<? super TypeDescription> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asErasure());
    }

    @Override
    public String toString() {
        return "erasure(" + matcher + ")";
    }
}
