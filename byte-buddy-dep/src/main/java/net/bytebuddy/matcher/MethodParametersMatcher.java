package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;

/**
 * An element matcher that matches a method's parameters.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class MethodParametersMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the parameters.
     */
    private final ElementMatcher<? super ParameterList<?>> matcher;

    /**
     * Creates a new matcher for a method's parameters.
     *
     * @param matcher The matcher to apply to the parameters.
     */
    public MethodParametersMatcher(ElementMatcher<? super ParameterList<? extends ParameterDescription>> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getParameters());
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "hasParameter(" + matcher + ")";
    }
}
