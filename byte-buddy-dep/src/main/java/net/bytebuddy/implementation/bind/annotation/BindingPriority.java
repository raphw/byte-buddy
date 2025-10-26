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
package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Defines a binding priority for a target method. If two target methods can be bound to a source method,
 * the one with the higher priority will be selected.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BindingPriority {

    /**
     * The default priority for methods not carrying the
     * {@link net.bytebuddy.implementation.bind.annotation.BindingPriority}
     * annotation.
     */
    int DEFAULT = 1;

    /**
     * The binding priority for the annotated method. A method of higher priority will be preferred over a method
     * of lower priority.
     *
     * @return The priority for the annotated method.
     */
    int value();

    /**
     * An ambiguity resolver that considers the priority of a method as defined by the
     * {@link net.bytebuddy.implementation.bind.annotation.BindingPriority}
     * annotation.
     */
    enum Resolver implements MethodDelegationBinder.AmbiguityResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A description of the {@link BindingPriority#value()} method.
         */
        private static final MethodDescription.InDefinedShape VALUE = TypeDescription.ForLoadedType.of(BindingPriority.class)
                .getDeclaredMethods()
                .filter(named("value"))
                .getOnly();

        /**
         * Resolves the explicitly stated binding priority of a method or returns the default value if no such
         * explicit information can be found.
         *
         * @param bindingPriority The annotation of the method or {@code null} if no such annotation was found.
         * @return The factual priority of the method under investigation.
         */
        private static int resolve(@MaybeNull AnnotationDescription.Loadable<BindingPriority> bindingPriority) {
            return bindingPriority == null
                    ? BindingPriority.DEFAULT
                    : bindingPriority.getValue(VALUE).resolve(Integer.class);
        }

        /**
         * {@inheritDoc}
         */
        public Resolution resolve(MethodDescription source,
                                  MethodDelegationBinder.MethodBinding left,
                                  MethodDelegationBinder.MethodBinding right) {
            int leftPriority = resolve(left.getTarget().getDeclaredAnnotations().ofType(BindingPriority.class));
            int rightPriority = resolve(right.getTarget().getDeclaredAnnotations().ofType(BindingPriority.class));
            if (leftPriority == rightPriority) {
                return Resolution.AMBIGUOUS;
            } else if (leftPriority < rightPriority) {
                return Resolution.RIGHT;
            } else {
                return Resolution.LEFT;
            }
        }
    }
}
