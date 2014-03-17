package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ByteBuddy;
import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant.DefaultValue;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.modifier.Ownership;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

public class TypeProxy implements AuxiliaryType {

    public static final String REFLECTION_METHOD = "make";
    private static final String INSTANCE_FIELD = "target";

    public static class ByConstructor implements StackManipulation {

        private final TypeDescription proxiedType;
        private final TypeDescription instrumentedType;
        private final List<TypeDescription> constructorParameters;
        private final boolean ignoreFinalizer;

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

    public static class ByReflectionFactory implements StackManipulation {

        private final TypeDescription proxiedType;
        private final TypeDescription instrumentedType;
        private final boolean ignoreFinalizer;

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
                MethodDescription proxyMethod = methodAccessorFactory.requireAccessorMethodFor(instrumentedMethod);
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        MethodVariableAccess.forType(instrumentedType).loadFromIndex(0),
                        fieldLoadingInstruction,
                        MethodVariableAccess.loadArguments(instrumentedMethod),
                        MethodInvocation.invoke(proxyMethod),
                        MethodReturn.returning(instrumentedMethod.getReturnType())
                ).apply(methodVisitor, instrumentationContext);
                return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }

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

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodAccessorFactory.equals(((MethodCall) other).methodAccessorFactory);
        }

        @Override
        public int hashCode() {
            return methodAccessorFactory.hashCode();
        }

        @Override
        public String toString() {
            return "MethodCall{methodAccessorFactory=" + methodAccessorFactory + '}';
        }
    }

    private class SilentConstruction implements Instrumentation {

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
                        GET_REFLECTION_FACTORY_METHOD_DESCRIPTOR);
                methodVisitor.visitLdcInsn(Type.getType(instrumentedType.getDescriptor()));
                methodVisitor.visitLdcInsn(Type.getType(JAVA_LANG_OBJECT_DESCRIPTOR));
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, JAVA_LANG_CLASS_INTERNAL_NAME);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        JAVA_LANG_CLASS_INTERNAL_NAME,
                        GET_DECLARED_CONSTRUCTOR_METHOD_NAME,
                        GET_DECLARED_CONSTRUCTOR_METHOD_DESCRIPTOR);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        REFLECTION_FACTORY_INTERNAL_NAME,
                        NEW_CONSTRUCTOR_FOR_SERIALIZATION_METHOD_NAME,
                        NEW_CONSTRUCTOR_FOR_SERIALIZATION_METHOD_DESCRIPTOR);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, JAVA_LANG_OBJECT_INTERNAL_NAME);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JAVA_LANG_CONSTRUCTOR_INTERNAL_NAME,
                        NEW_INSTANCE_METHOD_NAME,
                        NEW_INSTANCE_METHOD_DESCRIPTOR);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, instrumentedType.getInternalName());
                methodVisitor.visitInsn(Opcodes.ARETURN);
                return new Size(4, 0);
            }
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new Appender(instrumentedType);
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 31;
        }
    }

    private final TypeDescription proxiedType;
    private final TypeDescription instrumentedType;
    private final boolean ignoreFinalizer;

    public TypeProxy(TypeDescription proxiedType, TypeDescription instrumentedType, boolean ignoreFinalizer) {
        this.proxiedType = proxiedType;
        this.instrumentedType = instrumentedType;
        this.ignoreFinalizer = ignoreFinalizer;
    }

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFormatVersion classFormatVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        MethodMatcher methodMatcher = ignoreFinalizer ? not(isFinalizer()) : any();
        return new ByteBuddy(classFormatVersion)
                .subclass(proxiedType, ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER.toArray(new ModifierContributor.ForType[DEFAULT_TYPE_MODIFIER.size()]))
                .method(methodMatcher).intercept(new MethodCall(methodAccessorFactory))
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
}
