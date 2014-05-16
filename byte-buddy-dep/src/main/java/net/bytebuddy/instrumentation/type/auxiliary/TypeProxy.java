package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.DefaultValue;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.modifier.Ownership;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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
    private final TypeDescription proxiedType;
    private final TypeDescription instrumentedType;
    private final boolean ignoreFinalizer;

    /**
     * Creates a new type proxy.
     *
     * @param proxiedType      The type this proxy should implement which can either be a non-final class or an interface.
     * @param instrumentedType The type on which all accessor methods are invoked, i.e. the type for which this type proxy
     *                         is a proxy for.
     * @param ignoreFinalizer  {@code true} if any finalizer methods should be ignored for proxying.
     */
    public TypeProxy(TypeDescription proxiedType, TypeDescription instrumentedType, boolean ignoreFinalizer) {
        this.proxiedType = proxiedType;
        this.instrumentedType = instrumentedType;
        this.ignoreFinalizer = ignoreFinalizer;
    }

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        MethodMatcher finalizerMatcher = ignoreFinalizer ? not(isFinalizer()) : any();
        return new ByteBuddy(classFileVersion)
                .subclass(proxiedType, ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER.toArray(new ModifierContributor.ForType[DEFAULT_TYPE_MODIFIER.size()]))
                .method(finalizerMatcher)
                .intercept(new MethodCall(methodAccessorFactory))
                .defineMethod(REFLECTION_METHOD, TargetType.DESCRIPTION, Collections.<TypeDescription>emptyList(), Ownership.STATIC)
                .intercept(new SilentConstruction())
                .make();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        TypeProxy typeProxy = (TypeProxy) other;
        return ignoreFinalizer == typeProxy.ignoreFinalizer
                && instrumentedType.equals(typeProxy.instrumentedType)
                && proxiedType.equals(typeProxy.proxiedType);
    }

    @Override
    public int hashCode() {
        int result = proxiedType.hashCode();
        result = 31 * result + instrumentedType.hashCode();
        result = 31 * result + (ignoreFinalizer ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TypeProxy{" +
                "proxiedType=" + proxiedType +
                ", instrumentedType=" + instrumentedType +
                ", ignoreFinalizer=" + ignoreFinalizer +
                '}';
    }

    /**
     * Loads a type proxy onto the operand stack which is created by calling one of its constructors. When this
     * stack manipulation is applied, an instance of the instrumented type must lie on top of the operand stack.
     * All constructor parameters will be assigned their default values when this stack operation is applied.
     */
    public static class ByConstructor implements StackManipulation {

        private final TypeDescription proxiedType;
        private final TypeDescription instrumentedType;
        private final List<TypeDescription> constructorParameters;
        private final boolean ignoreFinalizer;

        /**
         * Creates a new stack operation for creating a type proxy by calling one of its constructors.
         *
         * @param proxiedType           The type for the type proxy to subclass or implement.
         * @param instrumentedType      The instrumented type which is the target of the method calls of the {@code proxiedType}.
         * @param constructorParameters The parameter types of the constructor that should be called.
         * @param ignoreFinalizer       {@code true} if any finalizers should be ignored for the delegation.
         */
        public ByConstructor(TypeDescription proxiedType,
                             TypeDescription instrumentedType,
                             List<TypeDescription> constructorParameters,
                             boolean ignoreFinalizer) {
            this.proxiedType = proxiedType;
            this.instrumentedType = instrumentedType;
            this.constructorParameters = constructorParameters;
            this.ignoreFinalizer = ignoreFinalizer;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            TypeDescription proxyType = instrumentationContext.register(new TypeProxy(proxiedType, instrumentedType, ignoreFinalizer));
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
                    MethodVariableAccess.forType(instrumentedType).loadFromIndex(0),
                    FieldAccess.forField(proxyType.getDeclaredFields().named(INSTANCE_FIELD)).putter()
            ).apply(methodVisitor, instrumentationContext);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ByConstructor that = (ByConstructor) other;
            return ignoreFinalizer == that.ignoreFinalizer
                    && constructorParameters.equals(that.constructorParameters)
                    && instrumentedType.equals(that.instrumentedType)
                    && proxiedType.equals(that.proxiedType);
        }

        @Override
        public int hashCode() {
            int result = proxiedType.hashCode();
            result = 31 * result + instrumentedType.hashCode();
            result = 31 * result + constructorParameters.hashCode();
            result = 31 * result + (ignoreFinalizer ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TypeProxy.ByConstructor{" +
                    "proxiedType=" + proxiedType +
                    ", instrumentedType=" + instrumentedType +
                    ", constructorParameters=" + constructorParameters +
                    ", ignoreFinalizer=" + ignoreFinalizer +
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

        private final TypeDescription proxiedType;
        private final TypeDescription instrumentedType;
        private final boolean ignoreFinalizer;

        /**
         * Creates a new stack operation for reflectively creating a type proxy for the given arguments.
         *
         * @param proxiedType      The type for the type proxy to subclass or implement.
         * @param instrumentedType The instrumented type which is the target of the method calls of the {@code proxiedType}.
         * @param ignoreFinalizer  {@code true} {@code true} if any finalizer methods should be ignored for proxying.
         */
        public ByReflectionFactory(TypeDescription proxiedType, TypeDescription instrumentedType, boolean ignoreFinalizer) {
            this.proxiedType = proxiedType;
            this.instrumentedType = instrumentedType;
            this.ignoreFinalizer = ignoreFinalizer;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            TypeDescription proxyType = instrumentationContext.register(new TypeProxy(proxiedType, instrumentedType, ignoreFinalizer));
            return new Compound(
                    MethodInvocation.invoke(proxyType.getDeclaredMethods()
                            .filter(named(REFLECTION_METHOD).and(takesArguments(0))).getOnly()),
                    Duplication.SINGLE,
                    MethodVariableAccess.forType(instrumentedType).loadFromIndex(0),
                    FieldAccess.forField(proxyType.getDeclaredFields().named(INSTANCE_FIELD)).putter()
            ).apply(methodVisitor, instrumentationContext);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ByReflectionFactory that = (ByReflectionFactory) other;
            return ignoreFinalizer == that.ignoreFinalizer
                    && instrumentedType.equals(that.instrumentedType)
                    && proxiedType.equals(that.proxiedType);
        }

        @Override
        public int hashCode() {
            int result = proxiedType.hashCode();
            result = 31 * result + instrumentedType.hashCode();
            result = 31 * result + (ignoreFinalizer ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TypeProxy.ByReflectionFactory{" +
                    "proxiedType=" + proxiedType +
                    ", instrumentedType=" + instrumentedType +
                    ", ignoreFinalizer=" + ignoreFinalizer +
                    '}';
        }
    }

    private class MethodCall implements Instrumentation {

        private final MethodAccessorFactory methodAccessorFactory;

        private MethodCall(MethodAccessorFactory methodAccessorFactory) {
            this.methodAccessorFactory = methodAccessorFactory;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withField(INSTANCE_FIELD, TypeProxy.this.instrumentedType, Opcodes.ACC_SYNTHETIC);
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new Appender(instrumentedType);
        }

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

        private class Appender implements ByteCodeAppender {

            private final StackManipulation fieldLoadingInstruction;

            public Appender(TypeDescription instrumentedType) {
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
                MethodDescription proxyMethod = methodAccessorFactory.requireAccessorMethodFor(instrumentedMethod,
                        MethodAccessorFactory.LookupMode.Default.BY_SIGNATURE);
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        MethodVariableAccess.forType(instrumentedType).loadFromIndex(0),
                        fieldLoadingInstruction,
                        MethodVariableAccess.loadArguments(instrumentedMethod),
                        MethodInvocation.invoke(proxyMethod),
                        MethodReturn.returning(instrumentedMethod.getReturnType())
                ).apply(methodVisitor, instrumentationContext);
                return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
            }

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
                return "Appender{" +
                        "methodCall=" + MethodCall.this +
                        "fieldLoadingInstruction=" + fieldLoadingInstruction +
                        '}';
            }
        }
    }

    private class SilentConstruction implements Instrumentation {

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new Appender(instrumentedType);
        }

        private TypeProxy getTypeProxy() {
            return TypeProxy.this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && TypeProxy.this.equals(((SilentConstruction) other).getTypeProxy());
        }

        @Override
        public int hashCode() {
            return TypeProxy.this.hashCode();
        }

        @Override
        public String toString() {
            return "TypeProxy.SilentConstruction{typeProxy=" + TypeProxy.this + "}";
        }

        private class Appender implements ByteCodeAppender {

            public static final String REFLECTION_FACTORY_INTERNAL_NAME = "sun/reflect/ReflectionFactory";
            public static final String GET_REFLECTION_FACTORY_METHOD_NAME = "getReflectionFactory";
            public static final String GET_REFLECTION_FACTORY_METHOD_DESCRIPTOR = "()Lsun/reflect/ReflectionFactory;";
            public static final String NEW_CONSTRUCTOR_FOR_SERIALIZATION_METHOD_NAME = "newConstructorForSerialization";
            public static final String NEW_CONSTRUCTOR_FOR_SERIALIZATION_METHOD_DESCRIPTOR =
                    "(Ljava/lang/Class;Ljava/lang/reflect/Constructor;)Ljava/lang/reflect/Constructor;";

            public static final String JAVA_LANG_OBJECT_DESCRIPTOR = "Ljava/lang/Object;";
            public static final String JAVA_LANG_OBJECT_INTERNAL_NAME = "java/lang/Object";
            public static final String JAVA_LANG_CONSTRUCTOR_INTERNAL_NAME = "java/lang/reflect/Constructor";
            public static final String NEW_INSTANCE_METHOD_NAME = "newInstance";
            public static final String NEW_INSTANCE_METHOD_DESCRIPTOR = "([Ljava/lang/Object;)Ljava/lang/Object;";

            public static final String JAVA_LANG_CLASS_INTERNAL_NAME = "java/lang/Class";
            public static final String GET_DECLARED_CONSTRUCTOR_METHOD_NAME = "getDeclaredConstructor";
            public static final String GET_DECLARED_CONSTRUCTOR_METHOD_DESCRIPTOR =
                    "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;";

            private final TypeDescription instrumentedType;

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

            private SilentConstruction getSilentConstruction() {
                return SilentConstruction.this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((Appender) other).instrumentedType)
                        && SilentConstruction.this.equals(((Appender) other).getSilentConstruction());
            }

            @Override
            public int hashCode() {
                return 31 * SilentConstruction.this.hashCode() + instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "TypeProxy.SilentConstruction.Appender{" +
                        "silentConstruction=" + SilentConstruction.this +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }
        }
    }
}
