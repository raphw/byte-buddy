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
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.utility.JavaType;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A parameter with this annotation is assigned an instance of {@code java.lang.invoke.MethodHandle} which invokes the super
 * implementation of this method. If such a method is not available, this annotation causes that this delegation target cannot
 * be bound unless {@link SuperMethodHandle#nullIfImpossible()} is set to {@code true}.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SuperMethodHandle {

    /**
     * Indicates that the assigned method should attempt the invocation of an unambiguous default method if no super method is available.
     *
     * @return {@code true} if a default method should be invoked if it is not ambiguous and no super class method is available.
     */
    boolean fallbackToDefault() default true;

    /**
     * Indicates that {@code null} should be assigned to this parameter if no super method is invokable.
     *
     * @return {@code true} if {@code null} should be assigned if no valid method can be assigned.
     */
    boolean nullIfImpossible() default false;

    /**
     * A binder for the {@link SuperMethodHandle} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperMethodHandle> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A description of the {@link SuperMethodHandle#fallbackToDefault()} method.
         */
        private static final MethodDescription.InDefinedShape FALLBACK_TO_DEFAULT;

        /**
         * A description of the {@link SuperMethodHandle#nullIfImpossible()} method.
         */
        private static final MethodDescription.InDefinedShape NULL_IF_IMPOSSIBLE;

        /*
         * Resolves annotation properties.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(SuperMethodHandle.class).getDeclaredMethods();
            FALLBACK_TO_DEFAULT = methods.filter(named("fallbackToDefault")).getOnly();
            NULL_IF_IMPOSSIBLE = methods.filter(named("nullIfImpossible")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<SuperMethodHandle> getHandledType() {
            return SuperMethodHandle.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<SuperMethodHandle> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                throw new IllegalStateException("Cannot assign MethodHandle type to " + target);
            } else if (source.isMethod()) {
                Implementation.SpecialMethodInvocation specialMethodInvocation = (annotation.getValue(FALLBACK_TO_DEFAULT).resolve(Boolean.class)
                        ? implementationTarget.invokeDominant(source.asSignatureToken())
                        : implementationTarget.invokeSuper(source.asSignatureToken())).withCheckedCompatibilityTo(source.asTypeToken());
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
    }
}
