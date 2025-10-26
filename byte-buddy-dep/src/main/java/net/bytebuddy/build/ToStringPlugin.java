/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.ToStringMethod;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isToString;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A build tool plugin that adds a {@link Object#toString()} and method to a class if the {@link Enhance} annotation is present and no
 * explicit method declaration was added. This plugin does not need to be closed.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ToStringPlugin implements Plugin, Plugin.Factory {

    /**
     * A description of the {@link Enhance#prefix()} method.
     */
    private static final MethodDescription.InDefinedShape ENHANCE_PREFIX;

    /**
     * A description of the {@link Enhance#includeSyntheticFields()} method.
     */
    private static final MethodDescription.InDefinedShape ENHANCE_INCLUDE_SYNTHETIC_FIELDS;

    /*
     * Resolves annotation properties.
     */
    static {
        MethodList<MethodDescription.InDefinedShape> enhanceMethods = TypeDescription.ForLoadedType.of(Enhance.class).getDeclaredMethods();
        ENHANCE_PREFIX = enhanceMethods.filter(named("prefix")).getOnly();
        ENHANCE_INCLUDE_SYNTHETIC_FIELDS = enhanceMethods.filter(named("includeSyntheticFields")).getOnly();
    }

    /**
     * {@inheritDoc}
     */
    public Plugin make() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(@MaybeNull TypeDescription target) {
        return target != null && target.getDeclaredAnnotations().isAnnotationPresent(Enhance.class);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        AnnotationDescription.Loadable<Enhance> enhance = typeDescription.getDeclaredAnnotations().ofType(Enhance.class);
        if (typeDescription.getDeclaredMethods().filter(isToString()).isEmpty()) {
            builder = builder.method(isToString()).intercept(ToStringMethod.prefixedBy(enhance.getValue(ENHANCE_PREFIX)
                            .load(Enhance.class.getClassLoader())
                            .resolve(Enhance.Prefix.class)
                            .getPrefixResolver())
                    .withIgnoredFields(enhance.getValue(ENHANCE_INCLUDE_SYNTHETIC_FIELDS).resolve(Boolean.class)
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
