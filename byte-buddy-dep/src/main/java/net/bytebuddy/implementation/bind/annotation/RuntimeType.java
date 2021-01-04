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

import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.annotation.*;

/**
 * Parameters that are annotated with this annotation will be assigned by also considering the runtime type of the
 * target parameter. The same is true for a method's return type if a target method is annotated with this annotation.
 * <p>&nbsp;</p>
 * For example, if a source method {@code foo(Object)} is attempted to be bound to
 * {@code bar(@RuntimeType String)}, the binding will attempt to cast the argument of {@code foo} to a {@code String}
 * type before calling {@code bar} with this argument. If this is not possible, a {@link java.lang.ClassCastException}
 * will be thrown at runtime. Similarly, if a method {@code foo} returns a type {@code String} but is bound to a method
 * that returns a type {@code Object}, annotating the target method with {@code @RuntimeType} results in the
 * {@code foo} method casting the target's method return value to {@code String} before returning a value itself.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface RuntimeType {

    /**
     * A non-instantiable type that allows to check if a method or parameter should consider a runtime type.
     */
    final class Verifier {

        /**
         * As this is merely a utility method, the constructor is not supposed to be invoked.
         */
        private Verifier() {
            throw new UnsupportedOperationException();
        }

        /**
         * Checks if an annotated element should be assigned a value by considering the runtime type.
         *
         * @param annotationSource The annotated element of interest.
         * @return Indicates if dynamic type castings should be attempted for incompatible assignments.
         */
        public static Assigner.Typing check(AnnotationSource annotationSource) {
            return Assigner.Typing.of(annotationSource.getDeclaredAnnotations().isAnnotationPresent(RuntimeType.class));
        }
    }
}
