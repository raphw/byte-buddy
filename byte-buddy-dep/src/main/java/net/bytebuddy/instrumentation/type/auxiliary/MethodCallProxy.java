package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.modifier.MemberVisibility;
import org.objectweb.asm.MethodVisitor;

import java.util.*;
import java.util.concurrent.Callable;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

/**
 * A method call proxy represents a class that is compiled against a particular method which can then be called whenever
 * either its {@link java.util.concurrent.Callable#call()} or {@link Runnable#run()} method is called where the method
 * call proxy implements both interfaces.
 * <p>&nbsp;</p>
 * In order to do so, the method call proxy instances are constructed by providing all the necessary information for
 * calling a particular method:
 * <ol>
 * <li>If the target method is not {@code static}, the first argument should be an instance on which the method is called.</li>
 * <li>All arguments for the called method in the order in which they are required.</li>
 * </ol>
 */
public class MethodCallProxy implements AuxiliaryType {

    private static final String FIELD_NAME_PREFIX = "argument";
    private final Instrumentation.SpecialMethodInvocation specialMethodInvocation;
    private final Assigner assigner;

    /**
     * Creates a new method call proxy for a given method and uses a default assigner for assigning the method's return
     * value to either the {@link java.util.concurrent.Callable#call()} or {@link Runnable#run()} method returns.
     *
     * @param specialMethodInvocation The special method invocation which should be invoked by this method call proxy.
     */
    public MethodCallProxy(Instrumentation.SpecialMethodInvocation specialMethodInvocation) {
        this(specialMethodInvocation, new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), true));
    }

    /**
     * Creates a new method call proxy for a given method.
     *
     * @param specialMethodInvocation The special method invocation which should be invoked by this method call proxy.
     * @param assigner                An assigner for assigning the target method's return value to either the
     *                                {@link java.util.concurrent.Callable#call()} or {@link Runnable#run()}} methods'
     *                                return values.
     */
    public MethodCallProxy(Instrumentation.SpecialMethodInvocation specialMethodInvocation, Assigner assigner) {
        this.specialMethodInvocation = specialMethodInvocation;
        this.assigner = assigner;
    }

    private static LinkedHashMap<String, TypeDescription> extractFields(MethodDescription methodDescription) {
        TypeList parameterTypes = methodDescription.getParameterTypes();
        LinkedHashMap<String, TypeDescription> typeDescriptions =
                new LinkedHashMap<String, TypeDescription>((methodDescription.isStatic() ? 0 : 1) + parameterTypes.size());
        int currentIndex = 0;
        if (!methodDescription.isStatic()) {
            typeDescriptions.put(fieldName(currentIndex++), methodDescription.getDeclaringType());
        }
        for (TypeDescription parameterType : parameterTypes) {
            typeDescriptions.put(fieldName(currentIndex++), parameterType);
        }
        return typeDescriptions;
    }

    private static String fieldName(int index) {
        return String.format("%s%d", FIELD_NAME_PREFIX, index);
    }

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        MethodDescription accessorMethod = methodAccessorFactory.registerAccessorFor(specialMethodInvocation);
        LinkedHashMap<String, TypeDescription> parameterFields = extractFields(accessorMethod);
        Instrumentation methodCall = new MethodCall(accessorMethod, assigner);
        DynamicType.Builder<?> builder = new ByteBuddy(classFileVersion)
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .methodLookupEngine(ProxyMethodLookupEngine.INSTANCE)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .implement(Runnable.class)
                .implement(Callable.class)
                .method(not(isDeclaredBy(Object.class))).intercept(methodCall)
                .defineConstructor(new ArrayList<TypeDescription>(parameterFields.values()))
                .intercept(new ConstructorCall());
        for (Map.Entry<String, TypeDescription> field : parameterFields.entrySet()) {
            builder = builder.defineField(field.getKey(), field.getValue(), MemberVisibility.PRIVATE);
        }
        return builder.make();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && assigner.equals(((MethodCallProxy) other).assigner)
                && specialMethodInvocation.equals(((MethodCallProxy) other).specialMethodInvocation);
    }

    @Override
    public int hashCode() {
        return 31 * specialMethodInvocation.hashCode() + assigner.hashCode();
    }

    @Override
    public String toString() {
        return "MethodCallProxy{" +
                "specialMethodInvocation=" + specialMethodInvocation +
                ", assigner=" + assigner +
                '}';
    }

    private static enum ProxyMethodLookupEngine implements MethodLookupEngine, MethodLookupEngine.Factory {

        INSTANCE;

        private final MethodList methodList;

        private ProxyMethodLookupEngine() {
            List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(2);
            methodDescriptions.addAll(new MethodList.ForLoadedType(Runnable.class));
            methodDescriptions.addAll(new MethodList.ForLoadedType(Callable.class));
            methodList = new MethodList.Explicit(methodDescriptions);
        }

        @Override
        public Finding process(TypeDescription typeDescription) {
            List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(3);
            methodDescriptions.addAll(methodList);
            methodDescriptions.addAll(typeDescription.getDeclaredMethods());
            return new Finding.Default(typeDescription,
                    new MethodList.Explicit(methodDescriptions),
                    Collections.<TypeDescription, Set<MethodDescription>>emptyMap());
        }

        @Override
        public MethodLookupEngine make(ClassFileVersion classFileVersion) {
            return this;
        }
    }

    /**
     * A stack manipulation that creates a {@link net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy}
     * for a given method an pushes such an object onto the call stack. For this purpose, all arguments of the proxied method
     * are loaded onto the stack what is only possible if this instance is used from a method with an identical signature such
     * as the target method itself.
     */
    public static class AssignableSignatureCall implements StackManipulation {

        private final Instrumentation.SpecialMethodInvocation specialMethodInvocation;

        /**
         * Creates an operand stack assignment that creates a
         * {@link net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy} for the
         * {@code targetMethod} and pushes this proxy object onto the stack.
         *
         * @param specialMethodInvocation The special method invocation which should be invoked by the created method
         *                                call proxy.
         */
        public AssignableSignatureCall(Instrumentation.SpecialMethodInvocation specialMethodInvocation) {
            this.specialMethodInvocation = specialMethodInvocation;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            TypeDescription auxiliaryType = instrumentationContext.register(new MethodCallProxy(specialMethodInvocation));
            return new Compound(
                    TypeCreation.forType(auxiliaryType),
                    Duplication.SINGLE,
                    MethodVariableAccess.loadThisReferenceAndArguments(specialMethodInvocation.getMethodDescription()),
                    MethodInvocation.invoke(auxiliaryType.getDeclaredMethods().filter(isConstructor()).getOnly())
            ).apply(methodVisitor, instrumentationContext);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && specialMethodInvocation.equals(((AssignableSignatureCall) other).specialMethodInvocation);
        }

        @Override
        public int hashCode() {
            return specialMethodInvocation.hashCode();
        }

        @Override
        public String toString() {
            return "MethodCallProxy.AssignableSignatureCall{specialMethodInvocation=" + specialMethodInvocation + '}';
        }
    }

    private static class MethodCall implements Instrumentation {

        private final MethodDescription accessorMethod;
        private final Assigner assigner;

        private MethodCall(MethodDescription accessorMethod, Assigner assigner) {
            this.accessorMethod = accessorMethod;
            this.assigner = assigner;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return new Appender(instrumentationTarget.getTypeDescription());
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && accessorMethod.equals(((MethodCall) o).accessorMethod);
        }

        @Override
        public int hashCode() {
            return accessorMethod.hashCode();
        }

        @Override
        public String toString() {
            return "MethodCall{accessorMethod=" + accessorMethod + '}';
        }

        private class Appender implements ByteCodeAppender {

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
                StackManipulation thisReference = MethodVariableAccess.forType(instrumentedType).loadFromIndex(0);
                FieldList fieldList = instrumentedType.getDeclaredFields();
                StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                int index = 0;
                for (FieldDescription fieldDescription : fieldList) {
                    fieldLoading[index++] = new StackManipulation.Compound(thisReference, FieldAccess.forField(fieldDescription).getter());
                }
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        new StackManipulation.Compound(fieldLoading),
                        MethodInvocation.invoke(accessorMethod),
                        assigner.assign(accessorMethod.getReturnType(), instrumentedMethod.getReturnType(), false),
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
                        && instrumentedType.equals(((Appender) other).instrumentedType)
                        && MethodCall.this.equals(((Appender) other).getMethodCall());
            }

            @Override
            public int hashCode() {
                return 31 * MethodCall.this.hashCode() + instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "Appender{" +
                        "methodCall=" + MethodCall.this +
                        ", instrumentedType=" + instrumentedType +
                        '}';
            }
        }
    }

    private static class ConstructorCall implements Instrumentation {

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return new Appender(instrumentationTarget.getTypeDescription());
        }

        @Override
        public boolean equals(Object other) {
            return other != null && other.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 31;
        }

        private class Appender implements ByteCodeAppender {

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
                StackManipulation thisReference = MethodVariableAccess.forType(instrumentedMethod.getDeclaringType()).loadFromIndex(0);
                FieldList fieldList = instrumentedType.getDeclaredFields();
                StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                int index = 0;
                for (FieldDescription fieldDescription : fieldList) {
                    fieldLoading[index] = new StackManipulation.Compound(
                            thisReference,
                            MethodVariableAccess.forType(fieldDescription.getFieldType()).loadFromIndex(instrumentedMethod.getParameterOffset(index)),
                            FieldAccess.forField(fieldDescription).putter()
                    );
                    index++;
                }
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        thisReference,
                        MethodInvocation.invoke(instrumentedType.getSupertype().getDeclaredMethods().filter(isConstructor()).getOnly()),
                        new StackManipulation.Compound(fieldLoading),
                        MethodReturn.VOID
                ).apply(methodVisitor, instrumentationContext);
                return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
            }

            private ConstructorCall getConstructorCall() {
                return ConstructorCall.this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((Appender) other).instrumentedType)
                        && ConstructorCall.this.equals(((Appender) other).getConstructorCall());
            }

            @Override
            public int hashCode() {
                return instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "Appender{" +
                        "constructorCall=" + ConstructorCall.this +
                        ", instrumentedType=" + instrumentedType +
                        '}';
            }
        }
    }
}
