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
import net.bytebuddy.implementation.auxiliary.MethodCallProxy;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Parameters that are annotated with this annotation will be assigned a proxy for calling the instrumented method's
 * {@code super} implementation.
 * <p>&nbsp;</p>
 * The proxy will both implement the {@link java.util.concurrent.Callable} and the {@link java.lang.Runnable} interfaces
 * such that the annotated parameter must be assignable to any of those interfaces or be of the {@link java.lang.Object}
 * type.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SuperCall {

    /**
     * Determines if the generated proxy should be {@link java.io.Serializable}.
     *
     * @return {@code true} if the generated proxy should be {@link java.io.Serializable}.
     */
    boolean serializableProxy() default false;

    /**
     * Determines if the injected proxy should invoke the default method to the intercepted method if a common
     * super method invocation is not applicable. For this to be possible, the default method must not be ambiguous.
     *
     * @return {@code true} if the invocation should fall back to invoking the default method.
     */
    boolean fallbackToDefault() default true;

    /**
     * Assigns {@code null} to the parameter if it is impossible to invoke the super method or a possible dominant default method, if permitted.
     *
     * @return {@code true} if a {@code null} constant should be assigned to this parameter in case that a legal binding is impossible.
     */
    boolean nullIfImpossible() default false;

    /**
     * A binder for handling the
     * {@link net.bytebuddy.implementation.bind.annotation.SuperCall}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperCall> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A description of the {@link SuperCall#serializableProxy()} method.
         */
        private static final MethodDescription.InDefinedShape SERIALIZABLE_PROXY;

        /**
         * A description of the {@link SuperCall#fallbackToDefault()} method.
         */
        private static final MethodDescription.InDefinedShape FALLBACK_TO_DEFAULT;

        /**
         * A description of the {@link SuperCall#nullIfImpossible()} method.
         */
        private static final MethodDescription.InDefinedShape NULL_IF_IMPOSSIBLE;

        /*
         * Resolves annotation properties.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(SuperCall.class).getDeclaredMethods();
            SERIALIZABLE_PROXY = methods.filter(named("serializableProxy")).getOnly();
            FALLBACK_TO_DEFAULT = methods.filter(named("fallbackToDefault")).getOnly();
            NULL_IF_IMPOSSIBLE = methods.filter(named("nullIfImpossible")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<SuperCall> getHandledType() {
            return SuperCall.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<SuperCall> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            TypeDescription targetType = target.getType().asErasure();
            if (!targetType.represents(Runnable.class) && !targetType.represents(Callable.class) && !targetType.represents(Object.class)) {
                throw new IllegalStateException("A super method call proxy can only be assigned to Runnable or Callable types: " + target);
            } else if (source.isConstructor()) {
                return annotation.getValue(NULL_IF_IMPOSSIBLE).resolve(Boolean.class)
                        ? new MethodDelegationBinder.ParameterBinding.Anonymous(NullConstant.INSTANCE)
                        : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            Implementation.SpecialMethodInvocation specialMethodInvocation = (annotation.getValue(FALLBACK_TO_DEFAULT).resolve(Boolean.class)
                    ? implementationTarget.invokeDominant(source.asSignatureToken())
                    : implementationTarget.invokeSuper(source.asSignatureToken())).withCheckedCompatibilityTo(source.asTypeToken());
            StackManipulation stackManipulation;
            if (specialMethodInvocation.isValid()) {
                stackManipulation = new MethodCallProxy.AssignableSignatureCall(specialMethodInvocation, annotation.getValue(SERIALIZABLE_PROXY).resolve(Boolean.class));
            } else if (annotation.getValue(NULL_IF_IMPOSSIBLE).resolve(Boolean.class)) {
                stackManipulation = NullConstant.INSTANCE;
            } else {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(stackManipulation);
        }
    }
}
