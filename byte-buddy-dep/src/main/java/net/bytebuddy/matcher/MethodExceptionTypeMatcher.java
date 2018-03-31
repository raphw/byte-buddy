package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeList;

/**
 * An element matcher that matches the exceptions that are declared by a method.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class MethodExceptionTypeMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the matched method's exceptions.
     */
    private final ElementMatcher<? super TypeList.Generic> matcher;

    /**
     * Creates a new matcher for a method's exceptions.
     *
     * @param matcher The matcher to apply to the matched method's exceptions.
     */
    public MethodExceptionTypeMatcher(ElementMatcher<? super TypeList.Generic> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getExceptionTypes());
    }

    @Override
    public String toString() {
        return "exceptions(" + matcher + ")";
    }
}
