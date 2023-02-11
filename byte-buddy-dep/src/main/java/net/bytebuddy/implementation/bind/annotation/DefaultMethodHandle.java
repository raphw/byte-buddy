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

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.utility.JavaType;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A parameter with this annotation is assigned an instance of {@code java.lang.invoke.MethodHandle} which invokes a
 * default method implementation of this method. If such a method is not available, this annotation causes that this
 * delegation target cannot be bound unless {@link DefaultMethodHandle#nullIfImpossible()} is set to {@code true}.
 * Note that requesting such a method exposes the default method to reflective access.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DefaultMethodHandle {

    /**
     * Specifies an explicit type that declares the default method to invoke.
     *
     * @return The type declaring the method to invoke or {@link TargetType} to indicate that the instrumented method declared the method.
     */
    Class<?> targetType() default void.class;

    /**
     * Indicates that {@code null} should be assigned to this parameter if no default method is invokable.
     *
     * @return {@code true} if {@code null} should be assigned if no valid method can be assigned.
     */
    boolean nullIfImpossible() default false;

    /**
     * A binder for the {@link DefaultMethodHandle} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<DefaultMethodHandle> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * The {@link DefaultMethodHandle#targetType()} property.
         */
        private static final MethodDescription.InDefinedShape TARGET_TYPE;

        /**
         * The {@link DefaultMethodHandle#nullIfImpossible()} property.
         */
        private static final MethodDescription.InDefinedShape NULL_IF_IMPOSSIBLE;

        /*
         * Locates method constants for properties of the default method annotation.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methodList = TypeDescription.ForLoadedType.of(DefaultMethodHandle.class).getDeclaredMethods();
            TARGET_TYPE = methodList.filter(named("targetType")).getOnly();
            NULL_IF_IMPOSSIBLE = methodList.filter(named("nullIfImpossible")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<DefaultMethodHandle> getHandledType() {
            return DefaultMethodHandle.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(final AnnotationDescription.Loadable<DefaultMethodHandle> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                throw new IllegalStateException("Cannot assign MethodHandle type to " + target);
            } else if (source.isMethod()) {
                TypeDescription typeDescription = annotation.getValue(TARGET_TYPE).resolve(TypeDescription.class);
                Implementation.SpecialMethodInvocation specialMethodInvocation = (typeDescription.represents(void.class)
                        ? MethodLocator.ForImplicitType.INSTANCE
                        : new MethodLocator.ForExplicitType(typeDescription)).resolve(implementationTarget, source).withCheckedCompatibilityTo(source.asTypeToken());
                if (specialMethodInvocation.isValid()) {
                    return new MethodDelegationBinder.ParameterBinding.Anonymous(specialMethodInvocation.toMethodHandle().toStackManipulation());
                } else if (annotation.getValue(NULL_IF_IMPOSSIBLE).resolve(Boolean.class)) {
                    return new MethodDelegationBinder.ParameterBinding.Anonymous(NullConstant.INSTANCE);
                } else {
                    return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
                }
            } else if (annotation.getValue(NULL_IF_IMPOSSIBLE).resolve(Boolean.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(NullConstant.INSTANCE);
            } else {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
        }

        /**
         * A method locator is responsible for creating the super method call.
         */
        protected interface MethodLocator {

            /**
             * Resolves the special method invocation to this target.
             *
             * @param implementationTarget The implementation target.
             * @param source               The method being instrumented.
             * @return A special method invocation that represents the super call of this binding.
             */
            Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source);

            /**
             * A method locator for an implicit target type.
             */
            enum ForImplicitType implements MethodLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source) {
                    return implementationTarget.invokeDefault(source.asSignatureToken());
                }
            }

            /**
             * A method locator for an explicit target type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForExplicitType implements MethodLocator {

                /**
                 * The explicit target type.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a method locator for an explicit target.
                 *
                 * @param typeDescription The explicit target type.
                 */
                protected ForExplicitType(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source) {
                    if (!typeDescription.isInterface()) {
                        throw new IllegalStateException(source + " method carries default method call parameter on non-interface type");
                    }
                    return implementationTarget.invokeDefault(source.asSignatureToken(), TargetType.resolve(typeDescription, implementationTarget.getInstrumentedType()));
                }
            }
        }
    }
}
