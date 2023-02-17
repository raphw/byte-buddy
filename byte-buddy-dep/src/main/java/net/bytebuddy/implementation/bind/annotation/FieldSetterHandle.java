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
import net.bytebuddy.description.field.FieldDescription;
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
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * Assigns a {@code java.lang.invoke.MethodHandle} to the annotated parameter which represents a getter of the represented field.
 * For a binding to be valid, the instrumented type must be able to access a field of the given name.
 * </p>
 * <p>
 * Setting {@link FieldSetterHandle#value()} is optional. If the value is not set, the field value attempts to bind a setter's
 * or getter's field if the intercepted method is an accessor method. Otherwise, the binding renders the target method
 * to be an illegal candidate for binding.
 * </p>
 * <p>
 * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.asm.Advice.FieldSetterHandle} or
 * {@link net.bytebuddy.asm.MemberSubstitution.FieldSetterHandle}. This annotation should be used with
 * {@link net.bytebuddy.implementation.MethodDelegation} only.
 * </p>
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 * @see RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FieldSetterHandle {

    /**
     * The name of the field to be accessed.
     *
     * @return The name of the field.
     */
    String value() default TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFieldBinding.BEAN_PROPERTY;

    /**
     * Defines the type on which the field is declared. If this value is not set, the most specific type's field is read,
     * if two fields with the same name exist in the same type hierarchy.
     *
     * @return The type that declares the accessed field.
     */
    Class<?> declaringType() default void.class;

    /**
     * Binds a {@link FieldSetterHandle} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldSetterHandle> {

        /**
         * The singleton instance.
         */
        INSTANCE(new Delegate());

        /**
         * The annotation method that for the defining type.
         */
        private static final MethodDescription.InDefinedShape DECLARING_TYPE;

        /**
         * The annotation method for the field's name.
         */
        private static final MethodDescription.InDefinedShape FIELD_NAME;

        /*
         * Initializes the methods of the annotation that is read by this binder.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methodList = TypeDescription.ForLoadedType.of(FieldSetterHandle.class).getDeclaredMethods();
            DECLARING_TYPE = methodList.filter(named("declaringType")).getOnly();
            FIELD_NAME = methodList.filter(named("value")).getOnly();
        }

        /**
         * A delegate parameter binder responsible for binding the parameter.
         */
        private final TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldSetterHandle> delegate;

        /**
         * Creates a new binder for a {@link FieldSetterHandle}.
         *
         * @param delegate A delegate parameter binder responsible for binding the parameter.
         */
        Binder(TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldSetterHandle> delegate) {
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        public Class<FieldSetterHandle> getHandledType() {
            return delegate.getHandledType();
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<FieldSetterHandle> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            return delegate.bind(annotation, source, target, implementationTarget, assigner, typing);
        }

        /**
         * A delegate implementation for the {@link FieldSetterHandle.Binder}.
         */
        protected static class Delegate extends ForFieldBinding<FieldSetterHandle> {

            /**
             * {@inheritDoc}
             */
            public Class<FieldSetterHandle> getHandledType() {
                return FieldSetterHandle.class;
            }

            @Override
            protected String fieldName(AnnotationDescription.Loadable<FieldSetterHandle> annotation) {
                return annotation.getValue(FIELD_NAME).resolve(String.class);
            }

            @Override
            protected TypeDescription declaringType(AnnotationDescription.Loadable<FieldSetterHandle> annotation) {
                return annotation.getValue(DECLARING_TYPE).resolve(TypeDescription.class);
            }

            @Override
            protected MethodDelegationBinder.ParameterBinding<?> bind(FieldDescription fieldDescription,
                                                                      AnnotationDescription.Loadable<FieldSetterHandle> annotation,
                                                                      MethodDescription source,
                                                                      ParameterDescription target,
                                                                      Implementation.Target implementationTarget,
                                                                      Assigner assigner) {
                if (!target.getType().asErasure().isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())) {
                    throw new IllegalStateException("Cannot assign method handle to " + target);
                } else if (fieldDescription.isStatic()) {
                    return new MethodDelegationBinder.ParameterBinding.Anonymous(JavaConstant.MethodHandle.ofSetter(fieldDescription.asDefined()).toStackManipulation());
                } else {
                    return new MethodDelegationBinder.ParameterBinding.Anonymous(new StackManipulation.Compound(
                            JavaConstant.MethodHandle.ofSetter(fieldDescription.asDefined()).toStackManipulation(),
                            MethodVariableAccess.loadThis(),
                            MethodInvocation.invoke(new MethodDescription.Latent(JavaType.METHOD_HANDLE.getTypeStub(), new MethodDescription.Token("bindTo",
                                    Opcodes.ACC_PUBLIC,
                                    JavaType.METHOD_HANDLE.getTypeStub().asGenericType(),
                                    new TypeList.Generic.Explicit(TypeDefinition.Sort.describe(Object.class)))))));
                }
            }
        }
    }
}
