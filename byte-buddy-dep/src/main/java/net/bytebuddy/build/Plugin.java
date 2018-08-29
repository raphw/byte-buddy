package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * A plugin that allows for the application of Byte Buddy transformations during a build process. This plugin's
 * transformation is applied to any type matching this plugin's type matcher. Plugin types must be public,
 * non-abstract and must declare a public default constructor to work.
 */
@HashCodeAndEqualsPlugin.Enhance
public interface Plugin extends ElementMatcher<TypeDescription> {

    /**
     * Applies this plugin.
     *
     * @param builder         The builder to use as a basis for the applied transformation.
     * @param typeDescription The type being transformed.
     * @return The supplied builder with additional transformations registered.
     */
    DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription);

    /**
     * An abstract base for a {@link Plugin} that matches types by a given {@link ElementMatcher}.
     */
    abstract class ForElementMatcher implements Plugin {

        /**
         * The element matcher to apply.
         */
        private final ElementMatcher<? super TypeDescription> matcher;

        /**
         * Creates a new plugin that matches types using an element matcher.
         *
         * @param matcher The element matcher to apply.
         */
        protected ForElementMatcher(ElementMatcher<? super TypeDescription> matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean matches(TypeDescription target) {
            return matcher.matches(target);
        }
    }
}
