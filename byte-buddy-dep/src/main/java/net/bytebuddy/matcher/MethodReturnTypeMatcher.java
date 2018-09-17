package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches its argument's return type against a given type matcher.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class MethodReturnTypeMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to apply to the matched element's return type.
     */
    private final ElementMatcher<? super TypeDescription.Generic> matcher;

    /**
     * Creates a new matcher for a matched element's return type.
     *
     * @param matcher The type matcher to apply to the matched element's return type.
     */
    public MethodReturnTypeMatcher(ElementMatcher<? super TypeDescription.Generic> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getReturnType());
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "returns(" + matcher + ")";
    }
}
