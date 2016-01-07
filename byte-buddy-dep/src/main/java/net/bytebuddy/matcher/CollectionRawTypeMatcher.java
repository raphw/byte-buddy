package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import java.util.ArrayList;
import java.util.List;

/**
 * An element matcher that matches a collection of types by their raw types.
 *
 * @param <T> The type of the matched entity.
 */
public class CollectionRawTypeMatcher<T extends Iterable<? extends TypeDefinition>> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to be applied to the raw types.
     */
    private final ElementMatcher<? super Iterable<? extends TypeDescription>> matcher;

    /**
     * Creates a new raw type matcher.
     *
     * @param matcher The matcher to be applied to the raw types.
     */
    public CollectionRawTypeMatcher(ElementMatcher<? super Iterable<? extends TypeDescription>> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>();
        for (TypeDefinition typeDefinition : target) {
            typeDescriptions.add(typeDefinition.asErasure());
        }
        return matcher.matches(typeDescriptions);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((CollectionRawTypeMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "rawTypes(" + matcher + ')';
    }
}
