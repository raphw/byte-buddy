package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.NamingStrategy;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.FieldRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationContextDelegate;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.SubclassTypeInstrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class MethodCallProxy implements AuxiliaryType {

    private static final String FIELD_NAME_PREFIX = "arg";

    public static class CalledFromSameSignatureMethod implements StackManipulation {

        private final MethodDescription proxiedMethod;
        private final Assigner assigner;

        public CalledFromSameSignatureMethod(MethodDescription proxiedMethod) {
            this(proxiedMethod, new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), true));
        }

        public CalledFromSameSignatureMethod(MethodDescription proxiedMethod, Assigner assigner) {
            this.proxiedMethod = proxiedMethod;
            this.assigner = assigner;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            String typeName = instrumentationContext.register(new MethodCallProxy(proxiedMethod, assigner));
            methodVisitor.visitTypeInsn(Opcodes.NEW, typeName);
            methodVisitor.visitInsn(Opcodes.DUP);
            Size size = new Size(2, 2);
            size = size.aggregate(MethodVariableAccess.loadAll(proxiedMethod).apply(methodVisitor, instrumentationContext));
            StringBuilder stringBuilder = new StringBuilder("(");
            if (!proxiedMethod.isStatic()) {
                stringBuilder.append(proxiedMethod.getDeclaringType().getDescriptor());
            }
            for (TypeDescription parameterType : proxiedMethod.getParameterTypes()) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            stringBuilder.append(")V");
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, typeName, MethodDescription.CONSTRUCTOR_INTERNAL_NAME, stringBuilder.toString());
            return new Size(1, size.getMaximalSize());
        }
    }

    private static class MethodDelegate implements MethodMatcher, MethodRegistry.Compiled, ByteCodeAppender {

        private static final String CALL_METHOD_NAME = "call", RUN_METHOD_NAME = "run";

        private final List<FieldAccess.Defined> fieldAccess;
        private final MethodDescription proxiedMethod;
        private final Assigner assigner;

        private MethodDelegate(List<FieldAccess.Defined> fieldAccess, MethodDescription proxiedMethod, Assigner assigner) {
            this.fieldAccess = fieldAccess;
            this.proxiedMethod = proxiedMethod;
            this.assigner = assigner;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.getParameterTypes().size() == 0
                    && (methodDescription.getName().equals(CALL_METHOD_NAME)
                    && methodDescription.getDeclaringType().getName().equals(Callable.class.getName()))
                    || (methodDescription.getName().equals(RUN_METHOD_NAME)
                    && methodDescription.getDeclaringType().getName().equals(Runnable.class.getName()));
        }

        @Override
        public MethodRegistry.Compiled.Entry target(MethodDescription methodDescription) {
            return new MethodRegistry.Compiled.Entry.Default(this, MethodAttributeAppender.NoOp.INSTANCE);
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            StackManipulation.Size size = new StackManipulation.Size(0, 0);
            for (FieldAccess.Defined fieldAccess : this.fieldAccess) {
                size = size.aggregate(fieldAccess.getter().apply(methodVisitor, instrumentationContext));
            }
            size = size.aggregate(MethodInvocation.invoke(proxiedMethod).apply(methodVisitor, instrumentationContext));
            size = size.aggregate(assigner.assign(proxiedMethod.getReturnType(), instrumentedMethod.getReturnType(), false)
                    .apply(methodVisitor, instrumentationContext));
            return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }

    private static class ConstructorDelegate implements MethodMatcher, MethodRegistry.Compiled, ByteCodeAppender {

        private final List<FieldAccess.Defined> fieldAccess;

        private ConstructorDelegate(List<FieldAccess.Defined> fieldAccess) {
            this.fieldAccess = fieldAccess;
        }

        @Override
        public boolean matches(MethodDescription methodDescription) {
            return methodDescription.isConstructor();
        }

        @Override
        public Entry target(MethodDescription methodDescription) {
            return new Entry.Default(this, MethodAttributeAppender.NoOp.INSTANCE);
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            StackManipulation.Size size = new StackManipulation.Size(0, 0);
            int offset = 1, index = 0;
            for (TypeDescription parameterType : instrumentedMethod.getParameterTypes()) {
                MethodVariableAccess.forType(instrumentedMethod.getDeclaringType()).loadFromIndex(0);
                MethodVariableAccess.forType(parameterType).loadFromIndex(offset);
                int parameterSize = parameterType.getStackSize().getSize() + 1;
                size = size.aggregate(new StackManipulation.Size(parameterSize, parameterSize));
                size = size.aggregate(fieldAccess.get(index++).putter().apply(methodVisitor, instrumentationContext));
                offset += parameterType.getStackSize().getSize();
            }
            size = size.aggregate(MethodReturn.VOID.apply(methodVisitor, instrumentationContext));
            return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }

    private final MethodDescription proxiedMethod;
    private final Assigner assigner;

    public MethodCallProxy(MethodDescription proxiedMethod, Assigner assigner) {
        this.proxiedMethod = proxiedMethod;
        this.assigner = assigner;
    }

    @Override
    public DynamicType<?> make(String auxiliaryTypeName, MethodProxyFactory methodProxyFactory) {
        MethodDescription proxiedMethod = methodProxyFactory.requireProxyMethodFor(this.proxiedMethod);
        int fieldIndex = 0;
        InstrumentedType proxy = new SubclassTypeInstrumentation(ClassFormatVersion.forCurrentJavaVersion(),
                Object.class,
                Arrays.<Class<?>>asList(Runnable.class, Callable.class),
                Opcodes.ACC_PUBLIC,
                new NamingStrategy.Fixed(auxiliaryTypeName));
        List<FieldAccess.Defined> fieldAccess = new LinkedList<FieldAccess.Defined>();
        List<TypeDescription> fieldTypes = new ArrayList<TypeDescription>();
        if (!proxiedMethod.isStatic()) {
            proxy = registerFieldFor(proxy, proxiedMethod.getDeclaringType(), fieldIndex++, fieldAccess, fieldTypes);
        }
        for (TypeDescription parameterType : proxiedMethod.getParameterTypes()) {
            proxy = registerFieldFor(proxy, parameterType, fieldIndex++, fieldAccess, fieldTypes);
        }
        proxy = proxy.withMethod(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                new TypeDescription.ForLoadedType(void.class),
                fieldTypes,
                ModifierContributor.EMPTY_MASK);
        SubclassInstrumentationContextDelegate contextDelegate = new SubclassInstrumentationContextDelegate(proxy);
        Instrumentation.Context instrumentationContext = new Instrumentation.Context.Default(contextDelegate, contextDelegate);
        MethodDelegate methodDelegate = new MethodDelegate(fieldAccess, proxiedMethod, assigner);
        ConstructorDelegate constructorDelegate = new ConstructorDelegate(fieldAccess);
        return new TypeWriter.Builder<Object>(proxy, instrumentationContext, ClassFormatVersion.forCurrentJavaVersion())
                .build(new ClassVisitorWrapper.Chain())
                .fields()
                .write(proxy.getDeclaredFields(), FieldRegistry.Compiled.NoOp.INSTANCE)
                .methods()
                .write(proxy.getDeclaredMethods().filter(methodDelegate), methodDelegate)
                .write(proxy.getDeclaredMethods().filter(constructorDelegate), constructorDelegate)
                .write(contextDelegate.getProxiedMethods(), contextDelegate)
                .make();
    }

    private static InstrumentedType registerFieldFor(InstrumentedType proxy,
                                                     TypeDescription fieldType,
                                                     int fieldIndex,
                                                     List<FieldAccess.Defined> fieldAccess,
                                                     List<TypeDescription> fieldTypes) {
        String fieldName = FIELD_NAME_PREFIX + fieldIndex;
        proxy = proxy.withField(fieldName, fieldType, Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);
        fieldAccess.add(FieldAccess.forField(proxy.getDeclaredFields().named(fieldName)));
        fieldTypes.add(fieldType);
        return proxy;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && assigner.equals(((MethodCallProxy) other).assigner)
                && proxiedMethod.equals(((MethodCallProxy) other).proxiedMethod);
    }

    @Override
    public int hashCode() {
        return  31 * proxiedMethod.hashCode() + assigner.hashCode();
    }

    @Override
    public String toString() {
        return "MethodCallProxy{" +
                "proxiedMethod=" + proxiedMethod +
                ", assigner=" + assigner +
                '}';
    }
}
