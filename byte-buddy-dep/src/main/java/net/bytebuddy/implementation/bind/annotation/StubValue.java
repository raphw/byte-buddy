/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;

import java.lang.annotation.*;

/**
 * A stub value represents the (boxed) default value of the intercepted method's return type. This value can
 * only be assigned to a {@link java.lang.Object} parameter. This annotation is useful to conditionally return a
 * default value from a method when using an {@link java.lang.Object} return type in combination with the
 * {@link net.bytebuddy.implementation.bind.annotation.RuntimeType} annotation. The value is either representing
 * {@code null} if a method returns a reference type or {@code void} or a boxed primitive of the return type
 * representing the numeric value {@code 0}.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface StubValue {

    /**
     * Binds the {@link net.bytebuddy.implementation.bind.annotation.StubValue} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<StubValue> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public Class<StubValue> getHandledType() {
            return StubValue.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<StubValue> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().represents(Object.class)) {
                throw new IllegalStateException(target + " uses StubValue annotation on non-Object type");
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(source.getReturnType().represents(void.class)
                    ? NullConstant.INSTANCE
                    : new StackManipulation.Compound(DefaultValue.of(source.getReturnType().asErasure()),
                    assigner.assign(source.getReturnType(), TypeDescription.Generic.OBJECT, typing)));
        }
    }
}
