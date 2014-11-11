package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.Throw;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.DefaultValue;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.modifier.Ownership;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

/**
 * A type proxy creates accessor methods for all overridable methods of a given type by subclassing the given type and
 * delegating all method calls to accessor methods of the instrumented type it was created for.
 */
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
     * The instrumentation target of the proxied type.
     */
    private final Instrumentation.Target instrumentationTarget;

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
     * @param proxiedType           The type this proxy should implement which can either be a non-final class or an interface.
     * @param instrumentationTarget The instrumentation target this type proxy is created for.
     * @param ignoreFinalizer       {@code true} if any finalizer methods should be ignored for proxying.
     * @param serializableProxy     Determines if the proxy should be serializable.
     */
    public TypeProxy(TypeDescription proxiedType,
                     Instrumentation.Target instrumentationTarget,
                     InvocationFactory invocationFactory,
                     boolean ignoreFinalizer,
                     boolean serializableProxy) {
        this.proxiedType = proxiedType;
        this.instrumentationTarget = instrumentationTarget;
        this.invocationFactory = invocationFactory;
        this.ignoreFinalizer = ignoreFinalizer;
        this.serializableProxy = serializableProxy;
    }

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        return new ByteBuddy(classFileVersion)
                .subclass(proxiedType, invocationFactory.getConstructorStrategy())
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                .method(ignoreFinalizer ? not(isFinalizer()) : any())
                .intercept(new MethodCall(methodAccessorFactory))
                .defineMethod(REFLECTION_METHOD, TargetType.DESCRIPTION, Collections.<TypeDescription>emptyList(), Ownership.STATIC)
                .intercept(SilentConstruction.INSTANCE)
                .make();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        TypeProxy typeProxy = (TypeProxy) other;
        return ignoreFinalizer == typeProxy.ignoreFinalizer
                && serializableProxy == typeProxy.serializableProxy
                && instrumentationTarget.equals(typeProxy.instrumentationTarget)
                && invocationFactory.equals(typeProxy.invocationFactory)
                && proxiedType.equals(typeProxy.proxiedType);
    }

    @Override
    public int hashCode() {
        int result = proxiedType.hashCode();
        result = 31 * result + instrumentationTarget.hashCode();
        result = 31 * result + invocationFactory.hashCode();
        result = 31 * result + (ignoreFinalizer ? 1 : 0);
        result = 31 * result + (serializableProxy ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TypeProxy{" +
                "proxiedType=" + proxiedType +
                ", instrumentationTarget=" + instrumentationTarget +
                ", invocationFactory=" + invocationFactory +
                ", ignoreFinalizer=" + ignoreFinalizer +
                ", serializableProxy=" + serializableProxy +
                '}';
    }

    /**
     * A stack manipulation that throws an abstract method error in case that a given super method cannot be invoked.
     */
    private static enum AbstractMethodErrorThrow implements StackManipulation {

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
        private AbstractMethodErrorThrow() {
            TypeDescription abstractMethodError = new TypeDescription.ForLoadedType(AbstractMethodError.class);
            MethodDescription constructor = abstractMethodError.getDeclaredMethods()
                    .filter(isConstructor().and(takesArguments(0))).getOnly();
            implementation = new Compound(TypeCreation.forType(abstractMethodError),
                    Duplication.SINGLE,
                    MethodInvocation.invoke(constructor),
                    Throw.INSTANCE);
        }

        @Override
        public boolean isValid() {
            return implementation.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            return implementation.apply(methodVisitor, instrumentationContext);
        }
    }

    /**
     * An implementation of a <i>silent construction</i> of a given type by using the non-standardized
     * {@link sun.reflect.ReflectionFactory}. This way, a constructor invocation can be avoided. However, this comes
     * at the cost of potentially breaking compatibility as the reflection factory is not standardized.
     */
    private enum SilentConstruction implements Instrumentation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return new Appender(instrumentationTarget.getTypeDescription());
        }

        /**
         * The appender for implementing a {@link net.bytebuddy.instrumentation.type.auxiliary.TypeProxy.SilentConstruction}.
         */
        private static class Appender implements ByteCodeAppender {

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

            @Override
            public boolean appendsCode() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
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

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((Appender) other).instrumentedType);
            }

            @Override
            public int hashCode() {
                return instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "TypeProxy.SilentConstruction.Appender{instrumentedType=" + instrumentedType + '}';
            }
        }
    }

    public static interface InvocationFactory {

        Instrumentation.SpecialMethodInvocation invoke(Instrumentation.Target instrumentationTarget,
                                                       TypeDescription proxiedType,
                                                       MethodDescription instrumentedMethod);

        ConstructorStrategy getConstructorStrategy();

        static enum ForSuperMethodCall implements InvocationFactory {

            INSTANCE;

            @Override
            public Instrumentation.SpecialMethodInvocation invoke(Instrumentation.Target instrumentationTarget,
                                                                  TypeDescription proxiedType,
                                                                  MethodDescription instrumentedMethod) {
                return instrumentationTarget.invokeSuper(instrumentedMethod, Instrumentation.Target.MethodLookup.Default.MOST_SPECIFIC);
            }

            @Override
            public ConstructorStrategy getConstructorStrategy() {
                return ConstructorStrategy.Default.IMITATE_SUPER_TYPE;
            }
        }

        static enum ForDefaultMethodCall implements InvocationFactory {

            INSTANCE;

            @Override
            public Instrumentation.SpecialMethodInvocation invoke(Instrumentation.Target instrumentationTarget,
                                                                  TypeDescription proxiedType,
                                                                  MethodDescription instrumentedMethod) {
                return instrumentationTarget.invokeDefault(proxiedType, instrumentedMethod.getUniqueSignature());
            }

            @Override
            public ConstructorStrategy getConstructorStrategy() {
                return ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR;
            }
        }
    }

    /**
     * Loads a type proxy onto the operand stack which is created by calling one of its constructors. When this
     * stack manipulation is applied, an instance of the instrumented type must lie on top of the operand stack.
     * All constructor parameters will be assigned their default values when this stack operation is applied.
     */
    public static class ByConstructor implements StackManipulation {

        /**
         * The type for the type proxy to subclass or implement.
         */
        private final TypeDescription proxiedType;

        /**
         * The instrumentation target this type proxy is created for.
         */
        private final Instrumentation.Target instrumentationTarget;

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
         * @param instrumentationTarget The instrumentation target this type proxy is created for.
         * @param constructorParameters The parameter types of the constructor that should be called.
         * @param ignoreFinalizer       {@code true} if any finalizers should be ignored for the delegation.
         * @param serializableProxy     Determines if the proxy should be serializable.
         */
        public ByConstructor(TypeDescription proxiedType,
                             Instrumentation.Target instrumentationTarget,
                             List<TypeDescription> constructorParameters,
                             boolean ignoreFinalizer,
                             boolean serializableProxy) {
            this.proxiedType = proxiedType;
            this.instrumentationTarget = instrumentationTarget;
            this.constructorParameters = constructorParameters;
            this.ignoreFinalizer = ignoreFinalizer;
            this.serializableProxy = serializableProxy;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            TypeDescription proxyType = instrumentationContext
                    .register(new TypeProxy(proxiedType,
                            instrumentationTarget,
                            InvocationFactory.ForSuperMethodCall.INSTANCE,
                            ignoreFinalizer,
                            serializableProxy));
            StackManipulation[] constructorValue = new StackManipulation[constructorParameters.size()];
            int index = 0;
            for (TypeDescription parameterType : constructorParameters) {
                constructorValue[index++] = DefaultValue.of(parameterType);
            }
            return new Compound(
                    TypeCreation.forType(proxyType),
                    Duplication.SINGLE,
                    new Compound(constructorValue),
                    MethodInvocation.invoke(proxyType.getDeclaredMethods()
                            .filter(isConstructor().and(takesArguments(constructorParameters))).getOnly()),
                    Duplication.SINGLE,
                    MethodVariableAccess.forType(instrumentationTarget.getTypeDescription()).loadFromIndex(0),
                    FieldAccess.forField(proxyType.getDeclaredFields().named(INSTANCE_FIELD)).putter()
            ).apply(methodVisitor, instrumentationContext);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ByConstructor that = (ByConstructor) other;
            return ignoreFinalizer == that.ignoreFinalizer
                    && serializableProxy == that.serializableProxy
                    && constructorParameters.equals(that.constructorParameters)
                    && instrumentationTarget.equals(that.instrumentationTarget)
                    && proxiedType.equals(that.proxiedType);
        }

        @Override
        public int hashCode() {
            int result = proxiedType.hashCode();
            result = 31 * result + instrumentationTarget.hashCode();
            result = 31 * result + constructorParameters.hashCode();
            result = 31 * result + (ignoreFinalizer ? 1 : 0);
            result = 31 * result + (serializableProxy ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TypeProxy.ByConstructor{" +
                    "proxiedType=" + proxiedType +
                    ", instrumentationTarget=" + instrumentationTarget +
                    ", constructorParameters=" + constructorParameters +
                    ", ignoreFinalizer=" + ignoreFinalizer +
                    ", serializableProxy=" + serializableProxy +
                    '}';
        }
    }

    /**
     * Loads a type proxy onto the operand stack which is created by constructing a serialization constructor using
     * the Oracle JDK's {@link sun.reflect.ReflectionFactory#newConstructorForSerialization(Class, java.lang.reflect.Constructor)}
     * method which might not be available in any Java runtime. When this stack manipulation is applied, an instance of
     * the instrumented type must lie on top of the operand stack.
     */
    public static class ByReflectionFactory implements StackManipulation {

        /**
         * The type for which a proxy type is created.
         */
        private final TypeDescription proxiedType;

        /**
         * The instrumentation target of the proxied type.
         */
        private final Instrumentation.Target instrumentationTarget;

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
         * @param proxiedType           The type for the type proxy to subclass or implement.
         * @param instrumentationTarget The instrumentation target this type proxy is created for.
         * @param ignoreFinalizer       {@code true} if any finalizer methods should be ignored for proxying.
         * @param serializableProxy     Determines if the proxy should be serializable.
         */
        public ByReflectionFactory(TypeDescription proxiedType,
                                   Instrumentation.Target instrumentationTarget,
                                   boolean ignoreFinalizer,
                                   boolean serializableProxy) {
            this.proxiedType = proxiedType;
            this.instrumentationTarget = instrumentationTarget;
            this.ignoreFinalizer = ignoreFinalizer;
            this.serializableProxy = serializableProxy;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            TypeDescription proxyType = instrumentationContext
                    .register(new TypeProxy(proxiedType,
                            instrumentationTarget,
                            InvocationFactory.ForSuperMethodCall.INSTANCE,
                            ignoreFinalizer,
                            serializableProxy));
            return new Compound(
                    MethodInvocation.invoke(proxyType.getDeclaredMethods()
                            .filter(named(REFLECTION_METHOD).and(takesArguments(0))).getOnly()),
                    Duplication.SINGLE,
                    MethodVariableAccess.forType(instrumentationTarget.getTypeDescription()).loadFromIndex(0),
                    FieldAccess.forField(proxyType.getDeclaredFields().named(INSTANCE_FIELD)).putter()
            ).apply(methodVisitor, instrumentationContext);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ByReflectionFactory that = (ByReflectionFactory) other;
            return ignoreFinalizer == that.ignoreFinalizer
                    && instrumentationTarget.equals(that.instrumentationTarget)
                    && proxiedType.equals(that.proxiedType)
                    && serializableProxy == that.serializableProxy;
        }

        @Override
        public int hashCode() {
            int result = proxiedType.hashCode();
            result = 31 * result + instrumentationTarget.hashCode();
            result = 31 * result + (ignoreFinalizer ? 1 : 0);
            result = 31 * result + (serializableProxy ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TypeProxy.ByReflectionFactory{" +
                    "proxiedType=" + proxiedType +
                    ", instrumentationTarget=" + instrumentationTarget +
                    ", ignoreFinalizer=" + ignoreFinalizer +
                    ", serializableProxy=" + serializableProxy +
                    '}';
        }
    }

    public static class ForDefaultMethod implements StackManipulation {

        private final TypeDescription proxiedType;

        private final Instrumentation.Target instrumentationTarget;

        private final boolean serializableProxy;

        public ForDefaultMethod(TypeDescription proxiedType,
                                Instrumentation.Target instrumentationTarget,
                                boolean serializableProxy) {
            this.proxiedType = proxiedType;
            this.instrumentationTarget = instrumentationTarget;
            this.serializableProxy = serializableProxy;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            TypeDescription proxyType = instrumentationContext
                    .register(new TypeProxy(proxiedType,
                            instrumentationTarget,
                            InvocationFactory.ForDefaultMethodCall.INSTANCE,
                            true,
                            serializableProxy));
            return new Compound(
                    TypeCreation.forType(proxyType),
                    Duplication.SINGLE,
                    MethodInvocation.invoke(proxyType.getDeclaredMethods().filter(isConstructor()).getOnly()),
                    Duplication.SINGLE,
                    MethodVariableAccess.forType(instrumentationTarget.getTypeDescription()).loadFromIndex(0),
                    FieldAccess.forField(proxyType.getDeclaredFields().named(INSTANCE_FIELD)).putter()
            ).apply(methodVisitor, instrumentationContext);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ForDefaultMethod that = (ForDefaultMethod) other;
            return serializableProxy == that.serializableProxy
                    && instrumentationTarget.equals(that.instrumentationTarget)
                    && proxiedType.equals(that.proxiedType);
        }

        @Override
        public int hashCode() {
            int result = proxiedType.hashCode();
            result = 31 * result + instrumentationTarget.hashCode();
            result = 31 * result + (serializableProxy ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TypeProxy.ForDefaultMethod{" +
                    "proxiedType=" + proxiedType +
                    ", instrumentationTarget=" + instrumentationTarget +
                    ", serializableProxy=" + serializableProxy +
                    '}';
        }
    }

    /**
     * An instrumentation for implementing a method call of a {@link net.bytebuddy.instrumentation.type.auxiliary.TypeProxy}.
     */
    protected class MethodCall implements Instrumentation {

        /**
         * The method accessor factory to query for the super method invocation.
         */
        private final MethodAccessorFactory methodAccessorFactory;

        /**
         * Creates a new method call instrumentation.
         *
         * @param methodAccessorFactory The method accessor factory to query for the super method invocation.
         */
        protected MethodCall(MethodAccessorFactory methodAccessorFactory) {
            this.methodAccessorFactory = methodAccessorFactory;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withField(INSTANCE_FIELD,
                    TypeProxy.this.instrumentationTarget.getTypeDescription(),
                    Opcodes.ACC_SYNTHETIC);
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return new Appender(instrumentationTarget.getTypeDescription());
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private TypeProxy getTypeProxy() {
            return TypeProxy.this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodAccessorFactory.equals(((MethodCall) other).methodAccessorFactory)
                    && TypeProxy.this.equals(((MethodCall) other).getTypeProxy());
        }

        @Override
        public int hashCode() {
            return 31 * TypeProxy.this.hashCode() + methodAccessorFactory.hashCode();
        }

        @Override
        public String toString() {
            return "TypeProxy.MethodCall{" +
                    "typeProxy=" + TypeProxy.this +
                    "methodAccessorFactory=" + methodAccessorFactory +
                    '}';
        }

        /**
         * Implementation of a byte code appender for a {@link net.bytebuddy.instrumentation.type.auxiliary.TypeProxy.MethodCall}.
         */
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
                fieldLoadingInstruction = FieldAccess.forField(instrumentedType.getDeclaredFields().named(INSTANCE_FIELD)).getter();
            }

            @Override
            public boolean appendsCode() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor,
                              Context instrumentationContext,
                              MethodDescription instrumentedMethod) {
                StackManipulation.Size stackSize = implement(instrumentedMethod,
                        invocationFactory.invoke(instrumentationTarget, proxiedType, instrumentedMethod))
                        .apply(methodVisitor, instrumentationContext);
                return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
            }

            /**
             * Returns the actual implementation of a method based on the legality of the special method invocation.
             *
             * @param instrumentedMethod      The instrumented method.
             * @param specialMethodInvocation The special method invocation to proxy by the implemented method.
             * @return A stack manipulation that represents the invocation of the special method invocation.
             */
            private StackManipulation implement(MethodDescription instrumentedMethod,
                                                Instrumentation.SpecialMethodInvocation specialMethodInvocation) {
                return specialMethodInvocation.isValid()
                        ? new AccessorMethodInvocation(instrumentedMethod, specialMethodInvocation)
                        : AbstractMethodErrorThrow.INSTANCE;
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private MethodCall getMethodCall() {
                return MethodCall.this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && fieldLoadingInstruction.equals(((Appender) other).fieldLoadingInstruction)
                        && MethodCall.this.equals(((Appender) other).getMethodCall());
            }

            @Override
            public int hashCode() {
                return 31 * MethodCall.this.hashCode() + fieldLoadingInstruction.hashCode();
            }

            @Override
            public String toString() {
                return "TypeProxy.MethodCall.Appender{" +
                        "methodCall=" + MethodCall.this +
                        "fieldLoadingInstruction=" + fieldLoadingInstruction +
                        '}';
            }

            /**
             * Stack manipulation for invoking an accessor method.
             */
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

                @Override
                public boolean isValid() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                    MethodDescription proxyMethod = methodAccessorFactory.registerAccessorFor(specialMethodInvocation);
                    return new StackManipulation.Compound(
                            MethodVariableAccess.REFERENCE.loadFromIndex(0),
                            fieldLoadingInstruction,
                            MethodVariableAccess.forBridgeMethodInvocation(instrumentedMethod, proxyMethod),
                            MethodInvocation.invoke(proxyMethod),
                            MethodReturn.returning(instrumentedMethod.getReturnType())
                    ).apply(methodVisitor, instrumentationContext);
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private Appender getAppender() {
                    return Appender.this;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    AccessorMethodInvocation that = (AccessorMethodInvocation) other;
                    return Appender.this.equals(that.getAppender())
                            && instrumentedMethod.equals(that.instrumentedMethod)
                            && specialMethodInvocation.equals(that.specialMethodInvocation);
                }

                @Override
                public int hashCode() {
                    int result = Appender.this.hashCode();
                    result = 31 * result + instrumentedMethod.hashCode();
                    result = 31 * result + specialMethodInvocation.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "TypeProxy.MethodCall.Appender.AccessorMethodInvocation{" +
                            "appender=" + Appender.this +
                            ", instrumentedMethod=" + instrumentedMethod +
                            ", specialMethodInvocation=" + specialMethodInvocation +
                            '}';
                }
            }
        }
    }
}
