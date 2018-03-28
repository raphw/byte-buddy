package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import java.util.ArrayList;
import java.util.List;

/**
 * An element matcher that matches a collection of types by their erasures.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class CollectionErasureMatcher<T extends Iterable<? extends TypeDefinition>> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to be applied to the raw types.
     */
    private final ElementMatcher<? super Iterable<? extends TypeDescription>> matcher;

    /**
     * Creates a new raw type matcher.
     *
     * @param matcher The matcher to be applied to the raw types.
     */
    public CollectionErasureMatcher(ElementMatcher<? super Iterable<? extends TypeDescription>> matcher) {
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
    public String toString() {
        return "erasures(" + matcher + ')';
    }
}
