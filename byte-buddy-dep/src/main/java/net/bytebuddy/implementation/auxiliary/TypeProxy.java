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
package net.bytebuddy.implementation.auxiliary;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.implementation.bytecode.*;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatchers;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.Serializable;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A type proxy creates accessor methods for all overridable methods of a given type by subclassing the given type and
 * delegating all method calls to accessor methods of the instrumented type it was created for.
 */
@HashCodeAndEqualsPlugin.Enhance
public class TypeProxy implements AuxiliaryType {

    /**
     * The name of the {@code static} method that is added to this auxiliary type for creating instances by using the
     * Oracle JDK's {@link sun.reflect.ReflectionFactory}.
     */
    public static final String REFLECTION_METHOD = "make";

    /**
     * The name of the field that stores the delegation instance.
     */
    public static final String INSTANCE_FIELD = "target";

    /**
     * The type that is proxied, i.e. the original instrumented type this proxy is created for.
     */
    private final TypeDescription proxiedType;

    /**
     * The implementation target of the proxied type.
     */
    private final Implementation.Target implementationTarget;

    /**
     * The invocation factory for creating special method invocations.
     */
    private final InvocationFactory invocationFactory;

    /**
     * {@code true} if the finalizer method should not be instrumented.
     */
    private final boolean ignoreFinalizer;

    /**
     * Determines if the proxy should be serializable.
     */
    private final boolean serializableProxy;

    /**
     * Creates a new type proxy.
     *
     * @param proxiedType          The type this proxy should implement which can either be a non-final class or an interface.
     * @param implementationTarget The implementation target this type proxy is created for.
     * @param invocationFactory    The invocation factory for creating special method invocations.
     * @param ignoreFinalizer      {@code true} if any finalizer methods should be ignored for proxying.
     * @param serializableProxy    Determines if the proxy should be serializable.
     */
    public TypeProxy(TypeDescription proxiedType,
                     Implementation.Target implementationTarget,
                     InvocationFactory invocationFactory,
                     boolean ignoreFinalizer,
                     boolean serializableProxy) {
        this.proxiedType = proxiedType;
        this.implementationTarget = implementationTarget;
        this.invocationFactory = invocationFactory;
        this.ignoreFinalizer = ignoreFinalizer;
        this.serializableProxy = serializableProxy;
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        return new ByteBuddy(classFileVersion)
                .with(TypeValidation.DISABLED)
                .ignore(ignoreFinalizer ? isFinalizer() : ElementMatchers.<MethodDescription>none())
                .subclass(proxiedType)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                .method(any()).intercept(new MethodCall(methodAccessorFactory))
                .defineMethod(REFLECTION_METHOD, TargetType.class, Ownership.STATIC).intercept(SilentConstruction.INSTANCE)
                .make();
    }

    /**
     * A stack manipulation that throws an abstract method error in case that a given super method cannot be invoked.
     */
    protected enum AbstractMethodErrorThrow implements StackManipulation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * The stack manipulation that throws the abstract method error.
         */
        private final StackManipulation implementation;

        /**
         * Creates the singleton instance.
         */
        @SuppressFBWarnings(value = "SE_BAD_FIELD_STORE", justification = "Fields of enumerations are never serialized")
        AbstractMethodErrorThrow() {
            TypeDescription abstractMethodError = TypeDescription.ForLoadedType.of(AbstractMethodError.class);
            MethodDescription constructor = abstractMethodError.getDeclaredMethods()
                    .filter(isConstructor().and(takesArguments(0))).getOnly();
            implementation = new Compound(TypeCreation.of(abstractMethodError),
                    Duplication.SINGLE,
                    MethodInvocation.invoke(constructor),
                    Throw.INSTANCE);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return implementation.isValid();
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return implementation.apply(methodVisitor, implementationContext);
        }
    }

    /**
     * An implementation of a <i>silent construction</i> of a given type by using the non-standardized
     * {@link sun.reflect.ReflectionFactory}. This way, a constructor invocation can be avoided. However, this comes
     * at the cost of potentially breaking compatibility as the reflection factory is not standardized.
     */
    protected enum SilentConstruction implements Implementation {

        /**
         * The singleton instance.
         */
        INSTANCE;

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
         * The appender for implementing a {@link net.bytebuddy.implementation.auxiliary.TypeProxy.SilentConstruction}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Appender implements ByteCodeAppender {

            /**
             * The internal name of the reflection factory class.
             */
            public static final String REFLECTION_FACTORY_INTERNAL_NAME = "sun/reflect/ReflectionFactory";

            /**
             * The name of the factory method for getting hold of an instance of the reflection factory class.
             */
            public static final String GET_REFLECTION_FACTORY_METHOD_NAME = "getReflectionFactory";

            /**
             * The descriptor of the factory method for getting hold of an instance of the reflection factory class.
             */
            public static final String GET_REFLECTION_FACTORY_METHOD_DESCRIPTOR = "()Lsun/reflect/ReflectionFactory;";

            /**
             * The name of the method for creating a new serialization constructor.
             */
            public static final String NEW_CONSTRUCTOR_FOR_SERIALIZATION_METHOD_NAME = "newConstructorForSerialization";

            /**
             * The descriptor of the method for creating a new serialization constructor.
             */
            public static final String NEW_CONSTRUCTOR_FOR_SERIALIZATION_METHOD_DESCRIPTOR =
                    "(Ljava/lang/Class;Ljava/lang/reflect/Constructor;)Ljava/lang/reflect/Constructor;";

            /**
             * The descriptor of the {@link java.lang.Object} class.
             */
            public static final String JAVA_LANG_OBJECT_DESCRIPTOR = "Ljava/lang/Object;";

            /**
             * The internal name of the {@link java.lang.Object} class.
             */
            public static final String JAVA_LANG_OBJECT_INTERNAL_NAME = "java/lang/Object";

            /**
             * The internal name of the {@link java.lang.reflect.Constructor} class.
             */
            public static final String JAVA_LANG_CONSTRUCTOR_INTERNAL_NAME = "java/lang/reflect/Constructor";

            /**
             * The internal name of the {@link java.lang.reflect.Constructor#newInstance(Object...)} method.
             */
            public static final String NEW_INSTANCE_METHOD_NAME = "newInstance";

            /**
             * The descriptor of the {@link java.lang.reflect.Constructor#newInstance(Object...)} method.
             */
            public static final String NEW_INSTANCE_METHOD_DESCRIPTOR = "([Ljava/lang/Object;)Ljava/lang/Object;";

            /**
             * The internal name of the {@link java.lang.Class} class.
             */
            public static final String JAVA_LANG_CLASS_INTERNAL_NAME = "java/lang/Class";

            /**
             * The internal name of the {@link Class#getDeclaredClasses()} method.
             */
            public static final String GET_DECLARED_CONSTRUCTOR_METHOD_NAME = "getDeclaredConstructor";

            /**
             * The descriptor of the {@link Class#getDeclaredClasses()} method.
             */
            public static final String GET_DECLARED_CONSTRUCTOR_METHOD_DESCRIPTOR =
                    "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;";

            /**
             * The instrumented type that this factory method is created for.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new appender.
             *
             * @param instrumentedType The instrumented type that the factory method is created for.
             */
            private Appender(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        REFLECTION_FACTORY_INTERNAL_NAME,
                        GET_REFLECTION_FACTORY_METHOD_NAME,
                        GET_REFLECTION_FACTORY_METHOD_DESCRIPTOR,
                        false);
                methodVisitor.visitLdcInsn(Type.getType(instrumentedType.getDescriptor()));
                methodVisitor.visitLdcInsn(Type.getType(JAVA_LANG_OBJECT_DESCRIPTOR));
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, JAVA_LANG_CLASS_INTERNAL_NAME);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        JAVA_LANG_CLASS_INTERNAL_NAME,
                        GET_DECLARED_CONSTRUCTOR_METHOD_NAME,
                        GET_DECLARED_CONSTRUCTOR_METHOD_DESCRIPTOR,
                        false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        REFLECTION_FACTORY_INTERNAL_NAME,
                        NEW_CONSTRUCTOR_FOR_SERIALIZATION_METHOD_NAME,
                        NEW_CONSTRUCTOR_FOR_SERIALIZATION_METHOD_DESCRIPTOR,
                        false);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, JAVA_LANG_OBJECT_INTERNAL_NAME);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JAVA_LANG_CONSTRUCTOR_INTERNAL_NAME,
                        NEW_INSTANCE_METHOD_NAME,
                        NEW_INSTANCE_METHOD_DESCRIPTOR,
                        false);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, instrumentedType.getInternalName());
                methodVisitor.visitInsn(Opcodes.ARETURN);
                return new Size(4, 0);
            }
        }
    }

    /**
     * An invocation factory is responsible for creating a special method invocation for any method that is to be
     * invoked. These special method invocations are then implemented by the
     * {@link net.bytebuddy.implementation.auxiliary.TypeProxy}.
     * Illegal {@link Implementation.SpecialMethodInvocation} are implemented by
     * throwing an {@link java.lang.AbstractMethodError}.
     */
    public interface InvocationFactory {

        /**
         * Creates a special method invocation to implement for a given method.
         *
         * @param implementationTarget The implementation target the type proxy is created for.
         * @param proxiedType          The type for the type proxy to subclass or implement.
         * @param instrumentedMethod   The instrumented method that is to be invoked.
         * @return A special method invocation of the given method or an illegal invocation if the proxy should
         * throw an {@link java.lang.AbstractMethodError} when the instrumented method is invoked.
         */
        Implementation.SpecialMethodInvocation invoke(Implementation.Target implementationTarget,
                                                      TypeDescription proxiedType,
                                                      MethodDescription instrumentedMethod);

        /**
         * Default implementations of the
         * {@link net.bytebuddy.implementation.auxiliary.TypeProxy.InvocationFactory}.
         */
        enum Default implements InvocationFactory {

            /**
             * Invokes the super method of the instrumented method.
             */
            SUPER_METHOD {
                /** {@inheritDoc} */
                public Implementation.SpecialMethodInvocation invoke(Implementation.Target implementationTarget,
                                                                     TypeDescription proxiedType,
                                                                     MethodDescription instrumentedMethod) {
                    return implementationTarget.invokeDominant(instrumentedMethod.asSignatureToken());
                }
            },

            /**
             * Invokes the default method of the instrumented method if it exists and is not ambiguous.
             */
            DEFAULT_METHOD {
                /** {@inheritDoc} */
                public Implementation.SpecialMethodInvocation invoke(Implementation.Target implementationTarget,
                                                                     TypeDescription proxiedType,
                                                                     MethodDescription instrumentedMethod) {
                    return implementationTarget.invokeDefault(instrumentedMethod.asSignatureToken(), proxiedType);
                }
            };
        }
    }

    /**
     * Loads a type proxy onto the operand stack which is created by calling one of its constructors. When this
     * stack manipulation is applied, an instance of the instrumented type must lie on top of the operand stack.
     * All constructor parameters will be assigned their default values when this stack operation is applied.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class ForSuperMethodByConstructor implements StackManipulation {

        /**
         * The type for the type proxy to subclass or implement.
         */
        private final TypeDescription proxiedType;

        /**
         * The implementation target this type proxy is created for.
         */
        private final Implementation.Target implementationTarget;

        /**
         * The parameter types of the constructor that should be called.
         */
        private final List<TypeDescription> constructorParameters;

        /**
         * {@code true} if any finalizers should be ignored for the delegation.
         */
        private final boolean ignoreFinalizer;

        /**
         * Determines if the proxy should be serializable.
         */
        private final boolean serializableProxy;

        /**
         * Creates a new stack operation for creating a type proxy by calling one of its constructors.
         *
         * @param proxiedType           The type for the type proxy to subclass or implement.
         * @param implementationTarget  The implementation target this type proxy is created for.
         * @param constructorParameters The parameter types of the constructor that should be called.
         * @param ignoreFinalizer       {@code true} if any finalizers should be ignored for the delegation.
         * @param serializableProxy     Determines if the proxy should be serializable.
         */
        public ForSuperMethodByConstructor(TypeDescription proxiedType,
                                           Implementation.Target implementationTarget,
                                           List<TypeDescription> constructorParameters,
                                           boolean ignoreFinalizer,
                                           boolean serializableProxy) {
            this.proxiedType = proxiedType;
            this.implementationTarget = implementationTarget;
            this.constructorParameters = constructorParameters;
            this.ignoreFinalizer = ignoreFinalizer;
            this.serializableProxy = serializableProxy;
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
            TypeDescription proxyType = implementationContext
                    .register(new TypeProxy(proxiedType,
                            implementationTarget,
                            InvocationFactory.Default.SUPER_METHOD,
                            ignoreFinalizer,
                            serializableProxy));
            StackManipulation[] constructorValue = new StackManipulation[constructorParameters.size()];
            int index = 0;
            for (TypeDescription parameterType : constructorParameters) {
                constructorValue[index++] = DefaultValue.of(parameterType);
            }
            return new Compound(
                    TypeCreation.of(proxyType),
                    Duplication.SINGLE,
                    new Compound(constructorValue),
                    MethodInvocation.invoke(proxyType.getDeclaredMethods().filter(isConstructor().and(takesArguments(constructorParameters))).getOnly()),
                    Duplication.SINGLE,
                    MethodVariableAccess.loadThis(),
                    FieldAccess.forField(proxyType.getDeclaredFields().filter((named(INSTANCE_FIELD))).getOnly()).write()
            ).apply(methodVisitor, implementationContext);
        }
    }

    /**
     * Loads a type proxy onto the operand stack which is created by constructing a serialization constructor using
     * the Oracle JDK's {@link sun.reflect.ReflectionFactory#newConstructorForSerialization(Class, java.lang.reflect.Constructor)}
     * method which might not be available in any Java runtime. When this stack manipulation is applied, an instance of
     * the instrumented type must lie on top of the operand stack.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class ForSuperMethodByReflectionFactory implements StackManipulation {

        /**
         * The type for which a proxy type is created.
         */
        private final TypeDescription proxiedType;

        /**
         * The implementation target of the proxied type.
         */
        private final Implementation.Target implementationTarget;

        /**
         * {@code true} {@code true} if any finalizer methods should be ignored for proxying.
         */
        private final boolean ignoreFinalizer;

        /**
         * Determines if the proxy should be serializable.
         */
        private final boolean serializableProxy;

        /**
         * Creates a new stack operation for reflectively creating a type proxy for the given arguments.
         *
         * @param proxiedType          The type for the type proxy to subclass or implement.
         * @param implementationTarget The implementation target this type proxy is created for.
         * @param ignoreFinalizer      {@code true} if any finalizer methods should be ignored for proxying.
         * @param serializableProxy    Determines if the proxy should be serializable.
         */
        public ForSuperMethodByReflectionFactory(TypeDescription proxiedType,
                                                 Implementation.Target implementationTarget,
                                                 boolean ignoreFinalizer,
                                                 boolean serializableProxy) {
            this.proxiedType = proxiedType;
            this.implementationTarget = implementationTarget;
            this.ignoreFinalizer = ignoreFinalizer;
            this.serializableProxy = serializableProxy;
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
            TypeDescription proxyType = implementationContext.register(new TypeProxy(proxiedType,
                    implementationTarget,
                    InvocationFactory.Default.SUPER_METHOD,
                    ignoreFinalizer,
                    serializableProxy));
            return new Compound(
                    MethodInvocation.invoke(proxyType.getDeclaredMethods().filter(named(REFLECTION_METHOD).and(takesArguments(0))).getOnly()),
                    Duplication.SINGLE,
                    MethodVariableAccess.loadThis(),
                    FieldAccess.forField(proxyType.getDeclaredFields().filter((named(INSTANCE_FIELD))).getOnly()).write()
            ).apply(methodVisitor, implementationContext);
        }
    }

    /**
     * Creates a type proxy which delegates its super method calls to any invokable default method of
     * a given interface and loads an instance of this proxy onto the operand stack.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class ForDefaultMethod implements StackManipulation {

        /**
         * The proxied interface type.
         */
        private final TypeDescription proxiedType;

        /**
         * The implementation target for the original instrumentation.
         */
        private final Implementation.Target implementationTarget;

        /**
         * {@code true} if the proxy should be {@link java.io.Serializable}.
         */
        private final boolean serializableProxy;

        /**
         * Creates a new proxy creation for a default interface type proxy.
         *
         * @param proxiedType          The proxied interface type.
         * @param implementationTarget The implementation target for the original implementation.
         * @param serializableProxy    {@code true} if the proxy should be {@link java.io.Serializable}.
         */
        public ForDefaultMethod(TypeDescription proxiedType,
                                Implementation.Target implementationTarget,
                                boolean serializableProxy) {
            this.proxiedType = proxiedType;
            this.implementationTarget = implementationTarget;
            this.serializableProxy = serializableProxy;
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
            TypeDescription proxyType = implementationContext.register(new TypeProxy(proxiedType,
                    implementationTarget,
                    InvocationFactory.Default.DEFAULT_METHOD,
                    true,
                    serializableProxy));
            return new Compound(
                    TypeCreation.of(proxyType),
                    Duplication.SINGLE,
                    MethodInvocation.invoke(proxyType.getDeclaredMethods().filter(isConstructor()).getOnly()),
                    Duplication.SINGLE,
                    MethodVariableAccess.loadThis(),
                    FieldAccess.forField(proxyType.getDeclaredFields().filter((named(INSTANCE_FIELD))).getOnly()).write()
            ).apply(methodVisitor, implementationContext);
        }
    }

    /**
     * An implementation for a method call of a {@link net.bytebuddy.implementation.auxiliary.TypeProxy}.
     */
    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
    protected class MethodCall implements Implementation {

        /**
         * The method accessor factory to query for the super method invocation.
         */
        private final MethodAccessorFactory methodAccessorFactory;

        /**
         * Creates a new method call implementation.
         *
         * @param methodAccessorFactory The method accessor factory to query for the super method invocation.
         */
        protected MethodCall(MethodAccessorFactory methodAccessorFactory) {
            this.methodAccessorFactory = methodAccessorFactory;
        }

        /**
         * {@inheritDoc}
         */
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withField(new FieldDescription.Token(INSTANCE_FIELD,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE,
                    implementationTarget.getInstrumentedType().asGenericType()));
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType());
        }

        /**
         * Implementation of a byte code appender for a {@link net.bytebuddy.implementation.auxiliary.TypeProxy.MethodCall}.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class Appender implements ByteCodeAppender {

            /**
             * The stack manipulation for loading the proxied instance onto the stack.
             */
            private final StackManipulation fieldLoadingInstruction;

            /**
             * Creates a new appender.
             *
             * @param instrumentedType The instrumented type that is proxied by the enclosing instrumentation.
             */
            protected Appender(TypeDescription instrumentedType) {
                fieldLoadingInstruction = FieldAccess.forField(instrumentedType.getDeclaredFields().filter((named(INSTANCE_FIELD))).getOnly()).read();
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                SpecialMethodInvocation specialMethodInvocation = invocationFactory.invoke(implementationTarget, proxiedType, instrumentedMethod);
                StackManipulation.Size size = (specialMethodInvocation.isValid()
                        ? new AccessorMethodInvocation(instrumentedMethod, specialMethodInvocation)
                        : AbstractMethodErrorThrow.INSTANCE).apply(methodVisitor, implementationContext);
                return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
            }

            /**
             * Stack manipulation for invoking an accessor method.
             */
            @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
            protected class AccessorMethodInvocation implements StackManipulation {

                /**
                 * The instrumented method that is implemented.
                 */
                private final MethodDescription instrumentedMethod;

                /**
                 * The special method invocation that is invoked by this accessor method invocation.
                 */
                private final SpecialMethodInvocation specialMethodInvocation;

                /**
                 * Creates a new accessor method invocation.
                 *
                 * @param instrumentedMethod      The instrumented method that is implemented.
                 * @param specialMethodInvocation The special method invocation that is invoked by this accessor
                 *                                method invocation.
                 */
                protected AccessorMethodInvocation(MethodDescription instrumentedMethod,
                                                   SpecialMethodInvocation specialMethodInvocation) {
                    this.instrumentedMethod = instrumentedMethod;
                    this.specialMethodInvocation = specialMethodInvocation;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isValid() {
                    return specialMethodInvocation.isValid();
                }

                /**
                 * {@inheritDoc}
                 */
                public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                    MethodDescription.InDefinedShape proxyMethod = methodAccessorFactory.registerAccessorFor(specialMethodInvocation, MethodAccessorFactory.AccessType.DEFAULT);
                    return new StackManipulation.Compound(
                            MethodVariableAccess.loadThis(),
                            fieldLoadingInstruction,
                            MethodVariableAccess.allArgumentsOf(instrumentedMethod).asBridgeOf(proxyMethod),
                            MethodInvocation.invoke(proxyMethod),
                            MethodReturn.of(instrumentedMethod.getReturnType())
                    ).apply(methodVisitor, implementationContext);
                }
            }
        }
    }
}
