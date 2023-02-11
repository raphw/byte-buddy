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

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A parameter with this annotation is assigned a method handle for invoking a default method that fits the intercepted method.
 * If no suitable default method for the intercepted method can be identified, the target method with the annotated
 * parameter is considered to be unbindable, unless a {@code null} value is requested by setting a property.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DefaultCallHandle {

    /**
     * If this parameter is not explicitly set, a parameter with the {@link DefaultCallHandle} is only bound to a
     * source method if this source method directly represents an unambiguous, invokable default method. On the other
     * hand, if a method is not defined unambiguously by an interface, not setting this parameter will exclude
     * the target method with the annotated parameter from a binding to the source method.
     * <p>&nbsp;</p>
     * If this parameter is however set to an explicit interface type, a default method is always invoked on this given
     * type as long as this type defines a method with a compatible signature. If this is not the case, the target
     * method with the annotated parameter is no longer considered as a possible binding candidate of a source method.
     *
     * @return The target interface that a default method invocation is to be defined upon. If no such explicit target
     * is set, this parameter should not be defined as the predefined {@code void} type encodes an implicit resolution.
     */
    Class<?> targetType() default void.class;

    /**
     * Assigns {@code null} to the parameter if it is impossible to invoke the super method or a possible dominant default method, if permitted.
     *
     * @return {@code true} if a {@code null} constant should be assigned to this parameter in case that a legal binding is impossible.
     */
    boolean nullIfImpossible() default false;

    /**
     * A binder for handling the {@link DefaultCallHandle} annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<DefaultCallHandle> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A reference to the target type method of the default call annotation.
         */
        private static final MethodDescription.InDefinedShape TARGET_TYPE;

        /**
         * A reference to the null if possible method of the default call annotation.
         */
        private static final MethodDescription.InDefinedShape NULL_IF_IMPOSSIBLE;

        /*
         * Looks up method constants of the default call annotation.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> annotationProperties = TypeDescription.ForLoadedType.of(DefaultCallHandle.class).getDeclaredMethods();
            TARGET_TYPE = annotationProperties.filter(named("targetType")).getOnly();
            NULL_IF_IMPOSSIBLE = annotationProperties.filter(named("nullIfImpossible")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<DefaultCallHandle> getHandledType() {
            return DefaultCallHandle.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<DefaultCallHandle> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                throw new IllegalStateException("Cannot assign MethodHandle type to " + target);
            } else if (source.isConstructor()) {
                return annotation.getValue(NULL_IF_IMPOSSIBLE).resolve(Boolean.class)
                        ? new MethodDelegationBinder.ParameterBinding.Anonymous(NullConstant.INSTANCE)
                        : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            TypeDescription typeDescription = annotation.getValue(TARGET_TYPE).resolve(TypeDescription.class);
            Implementation.SpecialMethodInvocation specialMethodInvocation = (typeDescription.represents(void.class)
                    ? DefaultMethodLocator.Implicit.INSTANCE
                    : new DefaultMethodLocator.Explicit(typeDescription)).resolve(implementationTarget, source).withCheckedCompatibilityTo(source.asTypeToken());
            StackManipulation stackManipulation;
            if (specialMethodInvocation.isValid()) {
                List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(3 + source.getParameters().size() * 3);
                stackManipulations.add(specialMethodInvocation.toMethodHandle().toStackManipulation());
                stackManipulations.add(MethodVariableAccess.loadThis());
                stackManipulations.add(MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLE.getTypeStub(), new MethodDescription.Token("bindTo",
                        Opcodes.ACC_PUBLIC,
                        JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                        new TypeList.Generic.Explicit(TypeDefinition.Sort.describe(Object.class))))));
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

        /**
         * A default method locator is responsible for looking up a default method to a given source method.
         */
        protected interface DefaultMethodLocator {

            /**
             * Locates the correct default method to a given source method.
             *
             * @param implementationTarget The current implementation target.
             * @param source               The source method for which a default method should be looked up.
             * @return A special method invocation of the default method or an illegal special method invocation,
             * if no suitable invocation could be located.
             */
            Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source);

            /**
             * An implicit default method locator that only permits the invocation of a default method if the source
             * method itself represents a method that was defined on a default method interface.
             */
            enum Implicit implements DefaultMethodLocator {

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
             * An explicit default method locator attempts to look up a default method in the specified interface type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Explicit implements DefaultMethodLocator {

                /**
                 * A description of the type on which the default method should be invoked.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new explicit default method locator.
                 *
                 * @param typeDescription The actual target interface as explicitly defined by
                 *                        {@link DefaultCallHandle#targetType()}.
                 */
                public Explicit(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source) {
                    if (!typeDescription.isInterface()) {
                        throw new IllegalStateException(source + " method carries default method call parameter on non-interface type");
                    }
                    return implementationTarget.invokeDefault(source.asSignatureToken(), typeDescription);
                }
            }
        }
    }
}
