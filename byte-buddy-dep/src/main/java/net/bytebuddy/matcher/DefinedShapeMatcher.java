package net.bytebuddy.matcher;

import com.google.auto.value.AutoValue;
import net.bytebuddy.description.ByteCodeElement;

/**
 * An element matcher that matches a byte code's element's token against a matcher for such a token.
 *
 * @param <T> The type of the matched entity.
 * @param <S> The type of the defined shape of the matched entity.
 */
@AutoValue
public class DefinedShapeMatcher<T extends ByteCodeElement.TypeDependant<S, ?>, S extends ByteCodeElement.TypeDependant<?, ?>>
        extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply onto the defined shape of the matched entity.
     */
    private final ElementMatcher<? super S> matcher;

    /**
     * Creates a new matcher for a byte code element's defined shape.
     *
     * @param matcher The matcher to apply onto the defined shape of the matched entity.
     */
    public DefinedShapeMatcher(ElementMatcher<? super S> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asDefined());
    }

    @Override
    public String toString() {
        return "isDefinedAs(" + matcher + ')';
    }
}
