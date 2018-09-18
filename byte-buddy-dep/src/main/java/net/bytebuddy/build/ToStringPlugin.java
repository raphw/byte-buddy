package net.bytebuddy.build;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.ToStringMethod;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isToString;

/**
 * A build tool plugin that adds a {@link Object#toString()} and method to a class if the {@link Enhance} annotation is present and no
 * explicit method declaration was added. This plugin does not need to be closed.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ToStringPlugin implements Plugin {

    /**
     * {@inheritDoc}
     */
    public boolean matches(TypeDescription target) {
        return target.getDeclaredAnnotations().isAnnotationPresent(Enhance.class);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        Enhance enhance = typeDescription.getDeclaredAnnotations().ofType(Enhance.class).loadSilent();
        if (typeDescription.getDeclaredMethods().filter(isToString()).isEmpty()) {
            builder = builder.method(isToString()).intercept(ToStringMethod.prefixedBy(enhance.prefix().getPrefixResolver())
                    .withIgnoredFields(enhance.includeSyntheticFields()
                            ? ElementMatchers.<FieldDescription>none()
                            : ElementMatchers.<FieldDescription>isSynthetic())
                    .withIgnoredFields(isAnnotatedWith(Exclude.class)));
        }
        return builder;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }

    /**
     * Instructs the {@link ToStringPlugin} to generate a {@link Object#toString()} method for the annotated class unless this method
     * is already declared explicitly.
     */
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enhance {

        /**
         * Determines the prefix to be used for the string representation prior to adding field values.
         *
         * @return The prefix to use.
         */
        Prefix prefix() default Prefix.SIMPLE;

        /**
         * Determines if synthetic fields should be included in the string representation.
         *
         * @return {@code true} if synthetic fields should be included.
         */
        boolean includeSyntheticFields() default false;

        /**
         * A strategy for defining a prefix.
         */
        enum Prefix {

            /**
             * Determines the use of a fully qualified class name as a prefix.
             */
            FULLY_QUALIFIED(ToStringMethod.PrefixResolver.Default.FULLY_QUALIFIED_CLASS_NAME),

            /**
             * Determines the use of the canonical class name as a prefix.
             */
            CANONICAL(ToStringMethod.PrefixResolver.Default.CANONICAL_CLASS_NAME),

            /**
             * Determines the use of the simple class name as a prefix.
             */
            SIMPLE(ToStringMethod.PrefixResolver.Default.SIMPLE_CLASS_NAME);

            /**
             * The prefix resolver to use.
             */
            private final ToStringMethod.PrefixResolver.Default prefixResolver;

            /**
             * Creates a new prefix.
             *
             * @param prefixResolver The prefix resolver to use.
             */
            Prefix(ToStringMethod.PrefixResolver.Default prefixResolver) {
                this.prefixResolver = prefixResolver;
            }

            /**
             * Returns the prefix resolver to use.
             *
             * @return The prefix resolver to use.
             */
            protected ToStringMethod.PrefixResolver.Default getPrefixResolver() {
                return prefixResolver;
            }
        }
    }

    /**
     * Determines that the annotated field is excluded from a string representation of the {@link ToStringPlugin}.
     */
    @Documented
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Exclude {
        /* does not declare any properties */
    }
}
