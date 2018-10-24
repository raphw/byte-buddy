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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatchers;
import org.objectweb.asm.MethodVisitor;

import java.io.Serializable;
import java.lang.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * <p>
 * A target method parameter that is annotated with this annotation allows to forward an intercepted method
 * invocation to another instance. The instance to which a method call is forwarded must be of the most specific
 * type that declares the intercepted method on the intercepted type.
 * </p>
 * <p>
 * Unfortunately, before Java 8, the Java Class Library does not define any interface type which takes a single
 * {@link java.lang.Object} type and returns another {@link java.lang.Object} type. For this reason, a
 * {@link net.bytebuddy.implementation.bind.annotation.Pipe.Binder} needs to be installed explicitly
 * and registered on a {@link net.bytebuddy.implementation.MethodDelegation}. The installed type is allowed to be an
 * interface without any super types that declares a single method which maps an {@link java.lang.Object} type to
 * a another {@link java.lang.Object} type as a result value. It is however not prohibited to use generics in the
 * process.
 * </p>
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Pipe {

    /**
     * Determines if the generated proxy should be {@link java.io.Serializable}.
     *
     * @return {@code true} if the generated proxy should be {@link java.io.Serializable}.
     */
    boolean serializableProxy() default false;

    /**
     * A {@link net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder.ParameterBinder}
     * for binding the {@link net.bytebuddy.implementation.bind.annotation.Pipe} annotation.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> {

        /**
         * The method which implements the behavior of forwarding a method invocation. This method needs to define
         * a single non-static method with an {@link java.lang.Object} to {@link java.lang.Object} mapping.
         */
        private final MethodDescription forwardingMethod;

        /**
         * Creates a new binder. This constructor is not doing any validation of the forwarding method and its
         * declaring type. Such validation is normally performed by the
         * {@link net.bytebuddy.implementation.bind.annotation.Pipe.Binder#install(Class)}
         * method.
         *
         * @param forwardingMethod The method which implements the behavior of forwarding a method invocation. This
         *                         method needs to define a single non-static method with an {@link java.lang.Object}
         *                         to {@link java.lang.Object} mapping.
         */
        protected Binder(MethodDescription forwardingMethod) {
            this.forwardingMethod = forwardingMethod;
        }

        /**
         * Installs a given type for use on a {@link net.bytebuddy.implementation.bind.annotation.Pipe}
         * annotation. The given type must be an interface without any super interfaces and a single method which
         * maps an {@link java.lang.Object} type to another {@link java.lang.Object} type. The use of generics is
         * permitted.
         *
         * @param type The type to install.
         * @return A binder for the {@link net.bytebuddy.implementation.bind.annotation.Pipe}
         * annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> install(Class<?> type) {
            return install(TypeDescription.ForLoadedType.of(type));
        }

        /**
         * Installs a given type for use on a {@link net.bytebuddy.implementation.bind.annotation.Pipe}
         * annotation. The given type must be an interface without any super interfaces and a single method which
         * maps an {@link java.lang.Object} type to another {@link java.lang.Object} type. The use of generics is
         * permitted.
         *
         * @param typeDescription The type to install.
         * @return A binder for the {@link net.bytebuddy.implementation.bind.annotation.Pipe}
         * annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> install(TypeDescription typeDescription) {
            return new Binder(onlyMethod(typeDescription));
        }

        /**
         * Locates the only method of a type that is compatible to being overridden for invoking the proxy.
         *
         * @param typeDescription The type that is being installed.
         * @return Its only method after validation.
         */
        private static MethodDescription onlyMethod(TypeDescription typeDescription) {
            if (!typeDescription.isInterface()) {
                throw new IllegalArgumentException(typeDescription + " is not an interface");
            } else if (!typeDescription.getInterfaces().isEmpty()) {
                throw new IllegalArgumentException(typeDescription + " must not extend other interfaces");
            } else if (!typeDescription.isPublic()) {
                throw new IllegalArgumentException(typeDescription + " is mot public");
            }
            MethodList<?> methodCandidates = typeDescription.getDeclaredMethods().filter(isAbstract());
            if (methodCandidates.size() != 1) {
                throw new IllegalArgumentException(typeDescription + " must declare exactly one abstract method");
            }
            MethodDescription methodDescription = methodCandidates.getOnly();
            if (!methodDescription.getReturnType().asErasure().represents(Object.class)) {
                throw new IllegalArgumentException(methodDescription + " does not return an Object-type");
            } else if (methodDescription.getParameters().size() != 1 || !methodDescription.getParameters().getOnly().getType().asErasure().represents(Object.class)) {
                throw new IllegalArgumentException(methodDescription + " does not take a single Object-typed argument");
            }
            return methodDescription;
        }

        /**
         * {@inheritDoc}
         */
        public Class<Pipe> getHandledType() {
            return Pipe.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Pipe> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().asErasure().equals(forwardingMethod.getDeclaringType())) {
                throw new IllegalStateException("Illegal use of @Pipe for " + target + " which was installed for " + forwardingMethod.getDeclaringType());
            } else if (source.isStatic()) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(new Redirection(forwardingMethod.getDeclaringType().asErasure(),
                    source,
                    assigner,
                    annotation.loadSilent().serializableProxy()));
        }

        /**
         * An auxiliary type for performing the redirection of a method invocation as requested by the
         * {@link net.bytebuddy.implementation.bind.annotation.Pipe} annotation.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Redirection implements AuxiliaryType, StackManipulation {

            /**
             * The prefix for naming fields to store method arguments.
             */
            private static final String FIELD_NAME_PREFIX = "argument";

            /**
             * The type that declares the method for forwarding a method invocation.
             */
            private final TypeDescription forwardingType;

            /**
             * The method that is to be forwarded.
             */
            private final MethodDescription sourceMethod;

            /**
             * The assigner to use.
             */
            private final Assigner assigner;

            /**
             * Determines if the generated proxy should be {@link java.io.Serializable}.
             */
            private final boolean serializableProxy;

            /**
             * Creates a new redirection.
             *
             * @param forwardingType    The type that declares the method for forwarding a method invocation.
             * @param sourceMethod      The method that is to be forwarded.
             * @param assigner          The assigner to use.
             * @param serializableProxy Determines if the generated proxy should be {@link java.io.Serializable}.
             */
            protected Redirection(TypeDescription forwardingType,
                                  MethodDescription sourceMethod,
                                  Assigner assigner,
                                  boolean serializableProxy) {
                this.forwardingType = forwardingType;
                this.sourceMethod = sourceMethod;
                this.assigner = assigner;
                this.serializableProxy = serializableProxy;
            }

            /**
             * Extracts all parameters of a method to fields.
             *
             * @param methodDescription The method to extract the parameters from.
             * @return A linked hash map of field names to the types of these fields representing all parameters of the
             * given method.
             */
            private static LinkedHashMap<String, TypeDescription> extractFields(MethodDescription methodDescription) {
                TypeList parameterTypes = methodDescription.getParameters().asTypeList().asErasures();
                LinkedHashMap<String, TypeDescription> typeDescriptions = new LinkedHashMap<String, TypeDescription>();
                int currentIndex = 0;
                for (TypeDescription parameterType : parameterTypes) {
                    typeDescriptions.put(fieldName(currentIndex++), parameterType);
                }
                return typeDescriptions;
            }

            /**
             * Creates a new field name.
             *
             * @param index The index of the field.
             * @return The field name that corresponds to the index.
             */
            private static String fieldName(int index) {
                return FIELD_NAME_PREFIX + index;
            }

            /**
             * {@inheritDoc}
             */
            public DynamicType make(String auxiliaryTypeName,
                                    ClassFileVersion classFileVersion,
                                    MethodAccessorFactory methodAccessorFactory) {
                LinkedHashMap<String, TypeDescription> parameterFields = extractFields(sourceMethod);
                DynamicType.Builder<?> builder = new ByteBuddy(classFileVersion)
                        .with(TypeValidation.DISABLED)
                        .subclass(forwardingType, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .name(auxiliaryTypeName)
                        .modifiers(DEFAULT_TYPE_MODIFIER)
                        .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                        .method(ElementMatchers.<MethodDescription>isAbstract().and(isDeclaredBy(forwardingType)))
                        .intercept(new MethodCall(sourceMethod, assigner))
                        .defineConstructor().withParameters(parameterFields.values())
                        .intercept(ConstructorCall.INSTANCE);
                for (Map.Entry<String, TypeDescription> field : parameterFields.entrySet()) {
                    builder = builder.defineField(field.getKey(), field.getValue(), Visibility.PRIVATE);
                }
                return builder.make();
            }

            /**
             * {@inheritDoc}
             */
            public boolean isValid() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                TypeDescription forwardingType = implementationContext.register(this);
                return new Compound(
                        TypeCreation.of(forwardingType),
                        Duplication.SINGLE,
                        MethodVariableAccess.allArgumentsOf(sourceMethod),
                        MethodInvocation.invoke(forwardingType.getDeclaredMethods().filter(isConstructor()).getOnly())
                ).apply(methodVisitor, implementationContext);
            }

            /**
             * The implementation to implement a
             * {@link net.bytebuddy.implementation.bind.annotation.Pipe.Binder.Redirection}'s
             * constructor.
             */
            @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Fields of enumerations are never serialized")
            protected enum ConstructorCall implements Implementation {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * A reference of the {@link Object} type default constructor.
                 */
                private final MethodDescription.InDefinedShape objectTypeDefaultConstructor;

                /**
                 * Creates the constructor call singleton.
                 */
                ConstructorCall() {
                    objectTypeDefaultConstructor = TypeDescription.OBJECT.getDeclaredMethods().filter(isConstructor()).getOnly();
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                /**
                 * {@inheritDoc}
                 */
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new Appender(implementationTarget.getInstrumentedType());
                }

                /**
                 * The appender for implementing the
                 * {@link net.bytebuddy.implementation.bind.annotation.Pipe.Binder.Redirection.ConstructorCall}.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                private static class Appender implements ByteCodeAppender {

                    /**
                     * The instrumented type being created.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * Creates a new appender.
                     *
                     * @param instrumentedType The instrumented type that is being created.
                     */
                    private Appender(TypeDescription instrumentedType) {
                        this.instrumentedType = instrumentedType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                        FieldList<?> fieldList = instrumentedType.getDeclaredFields();
                        StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                        int index = 0;
                        for (FieldDescription fieldDescription : fieldList) {
                            fieldLoading[index] = new StackManipulation.Compound(
                                    MethodVariableAccess.loadThis(),
                                    MethodVariableAccess.load(instrumentedMethod.getParameters().get(index)),
                                    FieldAccess.forField(fieldDescription).write()
                            );
                            index++;
                        }
                        StackManipulation.Size stackSize = new StackManipulation.Compound(
                                MethodVariableAccess.loadThis(),
                                MethodInvocation.invoke(ConstructorCall.INSTANCE.objectTypeDefaultConstructor),
                                new StackManipulation.Compound(fieldLoading),
                                MethodReturn.VOID
                        ).apply(methodVisitor, implementationContext);
                        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                    }
                }
            }

            /**
             * The implementation to implement a
             * {@link net.bytebuddy.implementation.bind.annotation.Pipe.Binder.Redirection}'s
             * forwarding method.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class MethodCall implements Implementation {

                /**
                 * The method that is invoked by the implemented method.
                 */
                private final MethodDescription redirectedMethod;

                /**
                 * The assigner to be used for invoking the forwarded method.
                 */
                private final Assigner assigner;

                /**
                 * Creates a new method call implementation.
                 *
                 * @param redirectedMethod The method that is invoked by the implemented method.
                 * @param assigner         The assigner to be used for invoking the forwarded method.
                 */
                private MethodCall(MethodDescription redirectedMethod, Assigner assigner) {
                    this.redirectedMethod = redirectedMethod;
                    this.assigner = assigner;
                }

                /**
                 * {@inheritDoc}
                 */
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                /**
                 * {@inheritDoc}
                 */
                public ByteCodeAppender appender(Target implementationTarget) {
                    if (!redirectedMethod.isAccessibleTo(implementationTarget.getInstrumentedType())) {
                        throw new IllegalStateException("Cannot invoke " + redirectedMethod + " from outside of class via @Pipe proxy");
                    }
                    return new Appender(implementationTarget.getInstrumentedType());
                }

                /**
                 * The appender for implementing the
                 * {@link net.bytebuddy.implementation.bind.annotation.Pipe.Binder.Redirection.MethodCall}.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                private class Appender implements ByteCodeAppender {

                    /**
                     * The instrumented type that is implemented.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * Creates a new appender.
                     *
                     * @param instrumentedType The instrumented type to be implemented.
                     */
                    private Appender(TypeDescription instrumentedType) {
                        this.instrumentedType = instrumentedType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Size apply(MethodVisitor methodVisitor,
                                      Context implementationContext,
                                      MethodDescription instrumentedMethod) {
                        FieldList<?> fieldList = instrumentedType.getDeclaredFields();
                        StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                        int index = 0;
                        for (FieldDescription fieldDescription : fieldList) {
                            fieldLoading[index++] = new StackManipulation.Compound(MethodVariableAccess.loadThis(), FieldAccess.forField(fieldDescription).read());
                        }
                        StackManipulation.Size stackSize = new StackManipulation.Compound(
                                MethodVariableAccess.REFERENCE.loadFrom(1),
                                assigner.assign(TypeDescription.Generic.OBJECT, redirectedMethod.getDeclaringType().asGenericType(), Assigner.Typing.DYNAMIC),
                                new StackManipulation.Compound(fieldLoading),
                                MethodInvocation.invoke(redirectedMethod),
                                assigner.assign(redirectedMethod.getReturnType(), instrumentedMethod.getReturnType(), Assigner.Typing.DYNAMIC),
                                MethodReturn.REFERENCE
                        ).apply(methodVisitor, implementationContext);
                        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                    }
                }
            }
        }
    }
}
