package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import java.util.HashSet;
import java.util.Set;

/**
 * An element matcher that matches a super type.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class HasSuperTypeMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to any super type of the matched type.
     */
    private final ElementMatcher<? super TypeDescription.Generic> matcher;

    /**
     * Creates a new matcher for a super type.
     *
     * @param matcher The matcher to apply to any super type of the matched type.
     */
    public HasSuperTypeMatcher(ElementMatcher<? super TypeDescription.Generic> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        Set<TypeDescription> checkedInterfaces = new HashSet<TypeDescription>();
        for (TypeDefinition typeDefinition : target) {
            if (matcher.matches(typeDefinition.asGenericType()) || hasInterface(typeDefinition, checkedInterfaces)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Matches a type's interfaces against the provided matcher.
     *
     * @param typeDefinition    The type for which to check all implemented interfaces.
     * @param checkedInterfaces The interfaces that have already been checked.
     * @return {@code true} if any interface matches the supplied matcher.
     */
    private boolean hasInterface(TypeDefinition typeDefinition, Set<TypeDescription> checkedInterfaces) {
        for (TypeDefinition interfaceType : typeDefinition.getInterfaces()) {
            if (checkedInterfaces.add(interfaceType.asErasure()) && (matcher.matches(interfaceType.asGenericType()) || hasInterface(interfaceType, checkedInterfaces))) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "hasSuperType(" + matcher + ")";
    }
}
