package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * A plugin that allows for the application of Byte Buddy transformations during a build process. This plugin's
 * transformation is applied to any type matching this plugin's type matcher. Plugin types must be public,
 * non-abstract and must declare a public default constructor to work.
 */
public interface Plugin extends ElementMatcher<TypeDescription> {

    /**
     * Applies this plugin.
     *
     * @param builder         The builder to use as a basis for the applied transformation.
     * @param typeDescription The type being transformed.
     * @return The supplied builder with additional transformations registered.
     */
    DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription);
}
