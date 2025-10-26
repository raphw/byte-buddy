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
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * Assigns the value of a field of the instrumented type to the annotated parameter. For a binding to be valid,
 * the instrumented type must be able to access a field of the given name. Also, the parameter's type must be
 * assignable to the given field. For attempting a type casting, the {@link RuntimeType} annotation can be
 * applied to the parameter.
 * </p>
 * <p>
 * Setting {@link FieldValue#value()} is optional. If the value is not set, the field value attempts to bind a setter's
 * or getter's field if the intercepted method is an accessor method. Otherwise, the binding renders the target method
 * to be an illegal candidate for binding.
 * </p>
 * <p>
 * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.asm.Advice.FieldValue} or
 * {@link net.bytebuddy.asm.MemberSubstitution.FieldValue}. This annotation should be used with
 * {@link net.bytebuddy.implementation.MethodDelegation} only.
 * </p>
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 * @see net.bytebuddy.implementation.bind.annotation.RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FieldValue {

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
     * Binds a {@link FieldValue} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldValue> {

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
            MethodList<MethodDescription.InDefinedShape> methodList = TypeDescription.ForLoadedType.of(FieldValue.class).getDeclaredMethods();
            DECLARING_TYPE = methodList.filter(named("declaringType")).getOnly();
            FIELD_NAME = methodList.filter(named("value")).getOnly();
        }

        /**
         * A delegate parameter binder responsible for binding the parameter.
         */
        private final TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldValue> delegate;

        /**
         * Creates a new binder for a {@link FieldValue}.
         *
         * @param delegate A delegate parameter binder responsible for binding the parameter.
         */
        Binder(TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldValue> delegate) {
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        public Class<FieldValue> getHandledType() {
            return delegate.getHandledType();
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<FieldValue> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            return delegate.bind(annotation, source, target, implementationTarget, assigner, typing);
        }

        /**
         * A delegate implementation for the {@link FieldValue.Binder}.
         */
        protected static class Delegate extends ForFieldBinding<FieldValue> {

            /**
             * {@inheritDoc}
             */
            public Class<FieldValue> getHandledType() {
                return FieldValue.class;
            }

            @Override
            protected String fieldName(AnnotationDescription.Loadable<FieldValue> annotation) {
                return annotation.getValue(FIELD_NAME).resolve(String.class);
            }

            @Override
            protected TypeDescription declaringType(AnnotationDescription.Loadable<FieldValue> annotation) {
                return annotation.getValue(DECLARING_TYPE).resolve(TypeDescription.class);
            }

            @Override
            protected MethodDelegationBinder.ParameterBinding<?> bind(FieldDescription fieldDescription,
                                                                      AnnotationDescription.Loadable<FieldValue> annotation,
                                                                      MethodDescription source,
                                                                      ParameterDescription target,
                                                                      Implementation.Target implementationTarget,
                                                                      Assigner assigner) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        fieldDescription.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.loadThis(),
                        FieldAccess.forField(fieldDescription).read(),
                        assigner.assign(fieldDescription.getType(), target.getType(), RuntimeType.Verifier.check(target))
                );
                return stackManipulation.isValid()
                        ? new MethodDelegationBinder.ParameterBinding.Anonymous(stackManipulation)
                        : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
        }
    }
}
