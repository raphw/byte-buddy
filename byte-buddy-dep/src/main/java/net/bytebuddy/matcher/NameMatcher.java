package net.bytebuddy.matcher;

import net.bytebuddy.description.NamedElement;

/**
 * A method matcher that matches a byte code element's source code name:
 * <ul>
 * <li>The source code name of types is equal to their binary name where arrays are appended a {@code []} by
 * their arity and where inner classes are appended by dots to their outer class's source name.</li>
 * <li>Constructors and the type initializer methods are represented by the empty string as they do not
 * represent a source code name.</li>
 * <li>Fields are named as in the source code.</li>
 * </ul>
 *
 * @param <T> The type of the matched entity.
 */
public class NameMatcher<T extends NamedElement> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher that is applied to a byte code element's source code name.
     */
    private final ElementMatcher<String> matcher;

    /**
     * Creates a new matcher for a byte code element's source name.
     *
     * @param matcher The matcher that is applied to a byte code element's source code name.
     */
    public NameMatcher(ElementMatcher<String> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getSourceCodeName());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((NameMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "name(" + matcher + ")";
    }
}
