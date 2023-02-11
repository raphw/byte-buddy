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
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * Parameters that are annotated with this annotation will be assigned a {@code java.lang.invoke.MethodHandle} invoking
 * the {@code super} implementation.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SuperHandle {

    /**
     * Determines if the method handle should invoke the default method to the intercepted method if a common
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
     * A binder for handling the {@link SuperHandle} annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperHandle> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A description of the {@link SuperHandle#fallbackToDefault()} method.
         */
        private static final MethodDescription.InDefinedShape FALLBACK_TO_DEFAULT;

        /**
         * A description of the {@link SuperHandle#nullIfImpossible()} method.
         */
        private static final MethodDescription.InDefinedShape NULL_IF_IMPOSSIBLE;

        /**
         * A description of the {@code java.lang.invoke.MethodHandle#bindTo(Object)} method.
         */
        @MaybeNull
        private static final MethodDescription BIND_TO;

        /**
         * A description of the {@code java.lang.invoke.MethodHandles#insertArguments(MethodHandle,int,Object[])} method.
         */
        @MaybeNull
        private static final MethodDescription INSERT_ARGUMENTS;

        /*
         * Resolves annotation properties.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(SuperHandle.class).getDeclaredMethods();
            FALLBACK_TO_DEFAULT = methods.filter(named("fallbackToDefault")).getOnly();
            NULL_IF_IMPOSSIBLE = methods.filter(named("nullIfImpossible")).getOnly();
            MethodDescription bindTo, insertArguments;
            try {
                bindTo = JavaType.METHOD_HANDLE.loadAsDescription()
                        .getDeclaredMethods()
                        .filter(named("bindTo").and(takesArguments(Object.class)))
                        .getOnly();
                insertArguments = JavaType.METHOD_HANDLES.loadAsDescription()
                        .getDeclaredMethods()
                        .filter(named("insertArguments").and(takesArguments(JavaType.METHOD_HANDLE.load(), int.class, Object[].class)))
                        .getOnly();
            } catch (Exception ignored) {
                bindTo = null;
                insertArguments = null;
            }
            BIND_TO = bindTo;
            INSERT_ARGUMENTS = insertArguments;
        }

        /**
         * {@inheritDoc}
         */
        public Class<SuperHandle> getHandledType() {
            return SuperHandle.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<SuperHandle> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (BIND_TO == null || INSERT_ARGUMENTS == null) {
                throw new IllegalStateException("The current VM does not support method handles");
            } else if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                throw new IllegalStateException("A method handle for a super method invocation cannot be assigned to " + target);
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
                List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(1
                        + (source.isStatic() ? 0 : 2)
                        + source.getParameters().size() * 3);
                stackManipulations.add(specialMethodInvocation.toMethodHandle().toStackManipulation());
                if (!source.isStatic()) {
                    stackManipulations.add(MethodVariableAccess.loadThis());
                    stackManipulations.add(MethodInvocation.invoke(BIND_TO));
                }
                if (!source.getParameters().isEmpty()) {
                    List<StackManipulation> values = new ArrayList<StackManipulation>(source.getParameters().size());
                    for (ParameterDescription parameterDescription : source.getParameters()) {
                        values.add(parameterDescription.getType().isPrimitive() ? new StackManipulation.Compound(MethodVariableAccess.load(parameterDescription), assigner.assign(parameterDescription.getType(),
                                parameterDescription.getType().asErasure().asBoxed().asGenericType(),
                                typing)) : MethodVariableAccess.load(parameterDescription));
                    }
                    stackManipulations.add(IntegerConstant.forValue(0));
                    stackManipulations.add(ArrayFactory.forType(TypeDescription.ForLoadedType.of(Object.class).asGenericType()).withValues(values));
                    stackManipulations.add(MethodInvocation.invoke(INSERT_ARGUMENTS));
                }
                stackManipulation = new StackManipulation.Compound(stackManipulations);
            } else if (annotation.getValue(NULL_IF_IMPOSSIBLE).resolve(Boolean.class)) {
                stackManipulation = NullConstant.INSTANCE;
            } else {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(stackManipulation);
        }
    }
}
