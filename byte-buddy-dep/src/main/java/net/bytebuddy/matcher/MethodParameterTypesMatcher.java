package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;

import java.util.List;

/**
 * An element matcher that matches a method's parameter types.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class MethodParameterTypesMatcher<T extends ParameterList<?>> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the type of the parameter.
     */
    private final ElementMatcher<? super List<? extends TypeDescription.Generic>> matcher;

    /**
     * Creates a new matcher for a method's parameter types.
     *
     * @param matcher The matcher to apply to the type of the parameter.
     */
    public MethodParameterTypesMatcher(ElementMatcher<? super List<? extends TypeDescription.Generic>> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.asTypeList());
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "hasTypes(" + matcher + ")";
    }
}
