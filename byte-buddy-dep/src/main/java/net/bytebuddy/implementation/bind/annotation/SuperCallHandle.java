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
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
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
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;

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
public @interface SuperCallHandle {

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
     * A binder for handling the {@link SuperCallHandle} annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperCallHandle> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A description of the {@link SuperCallHandle#fallbackToDefault()} method.
         */
        private static final MethodDescription.InDefinedShape FALLBACK_TO_DEFAULT;

        /**
         * A description of the {@link SuperCallHandle#nullIfImpossible()} method.
         */
        private static final MethodDescription.InDefinedShape NULL_IF_IMPOSSIBLE;

        /*
         * Resolves annotation properties.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(SuperCallHandle.class).getDeclaredMethods();
            FALLBACK_TO_DEFAULT = methods.filter(named("fallbackToDefault")).getOnly();
            NULL_IF_IMPOSSIBLE = methods.filter(named("nullIfImpossible")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<SuperCallHandle> getHandledType() {
            return SuperCallHandle.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<SuperCallHandle> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
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
                    stackManipulations.add(MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLE.getTypeStub(), new MethodDescription.Token("bindTo",
                            Opcodes.ACC_PUBLIC,
                            JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                            new TypeList.Generic.Explicit(TypeDefinition.Sort.describe(Object.class))))));
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
                    stackManipulations.add(MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLES.getTypeStub(), new MethodDescription.Token("insertArguments",
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                            JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                            new TypeList.Generic.Explicit(JavaType.METHOD_HANDLE.getTypeStub(), TypeDefinition.Sort.describe(int.class), TypeDefinition.Sort.describe(Object[].class))))));
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
