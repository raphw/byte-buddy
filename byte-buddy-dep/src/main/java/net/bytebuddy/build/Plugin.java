package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A plugin that allows for the application of Byte Buddy transformations during a build process. This plugin's
 * transformation is applied to any type matching this plugin's type matcher. Plugin types must be public,
 * non-abstract and must declare a public default constructor to work.
 */
public interface Plugin extends ElementMatcher<TypeDescription>, Closeable {

    /**
     * Applies this plugin.
     *
     * @param builder          The builder to use as a basis for the applied transformation.
     * @param typeDescription  The type being transformed.
     * @param classFileLocator A class file locator that can locate other types in the scope of the project.
     * @return The supplied builder with additional transformations registered.
     */
    DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator);

    /**
     * A non-operational plugin that does not instrument any type. This plugin does not need to be closed.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class NoOp implements Plugin {

        /**
         * {@inheritDoc}
         */
        public boolean matches(TypeDescription target) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            throw new IllegalStateException("Cannot apply non-operational plugin");
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }
    }

    /**
     * An abstract base for a {@link Plugin} that matches types by a given {@link ElementMatcher}.
     */
    @HashCodeAndEqualsPlugin.Enhance
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

        /**
         * {@inheritDoc}
         */
        public boolean matches(TypeDescription target) {
            return matcher.matches(target);
        }
    }

    /**
     * A compound plugin that applies several plugins in a row.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements Plugin {

        /**
         * The plugins to apply.
         */
        private final List<Plugin> plugins;

        /**
         * Creates a compound plugin.
         *
         * @param plugin The plugins to apply.
         */
        public Compound(Plugin... plugin) {
            this(Arrays.asList(plugin));
        }

        /**
         * Creates a compound plugin.
         *
         * @param plugins The plugins to apply.
         */
        public Compound(List<? extends Plugin> plugins) {
            this.plugins = new ArrayList<Plugin>();
            for (Plugin plugin : plugins) {
                if (plugin instanceof Compound) {
                    this.plugins.addAll(((Compound) plugin).plugins);
                } else if (!(plugin instanceof NoOp)) {
                    this.plugins.add(plugin);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean matches(TypeDescription target) {
            for (Plugin plugin : plugins) {
                if (plugin.matches(target)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            for (Plugin plugin : plugins) {
                if (plugin.matches(typeDescription)) {
                    builder = plugin.apply(builder, typeDescription, classFileLocator);
                }
            }
            return builder;
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            for (Plugin plugin : plugins) {
                plugin.close();
            }
        }
    }
}
