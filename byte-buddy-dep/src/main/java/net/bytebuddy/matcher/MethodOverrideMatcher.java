package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.isVirtual;

/**
 * A matcher that checks if any super type of a type declares a method with the same shape of a matched method.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class MethodOverrideMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher that is to be applied to the type that declares a method of the same shape.
     */
    private final ElementMatcher<? super TypeDescription.Generic> matcher;

    /**
     * Creates a new method override matcher.
     *
     * @param matcher The matcher that is to be applied to the type that declares a method of the same shape.
     */
    public MethodOverrideMatcher(ElementMatcher<? super TypeDescription.Generic> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        Set<TypeDescription> duplicates = new HashSet<TypeDescription>();
        for (TypeDefinition typeDefinition : target.getDeclaringType()) {
            if (matches(target, typeDefinition) || matches(target, typeDefinition.getInterfaces(), duplicates)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Matches a method against a list of types.
     *
     * @param target          The method that is matched as a target.
     * @param typeDefinitions The type definitions to check if they declare a method with the same signature as {@code target}.
     * @param duplicates      A set containing duplicate interfaces that do not need to be revisited.
     * @return {@code true} if any type defines a method with the same signature as the {@code target} method.
     */
    private boolean matches(MethodDescription target, List<? extends TypeDefinition> typeDefinitions, Set<TypeDescription> duplicates) {
        for (TypeDefinition anInterface : typeDefinitions) {
            if (duplicates.add(anInterface.asErasure()) && (matches(target, anInterface) || matches(target, anInterface.getInterfaces(), duplicates))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a type declares a method with the same signature as {@code target}.
     *
     * @param target         The method to be checked.
     * @param typeDefinition The type to check for declaring a method with the same signature as {@code target}.
     * @return {@code true} if the supplied type declares a compatible method.
     */
    private boolean matches(MethodDescription target, TypeDefinition typeDefinition) {
        for (MethodDescription methodDescription : typeDefinition.getDeclaredMethods().filter(isVirtual())) {
            if (methodDescription.asSignatureToken().equals(target.asSignatureToken())) {
                if (matcher.matches(typeDefinition.asGenericType())) {
                    return true;
                } else {
                    break;
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "isOverriddenFrom(" + matcher + ")";
    }
}
