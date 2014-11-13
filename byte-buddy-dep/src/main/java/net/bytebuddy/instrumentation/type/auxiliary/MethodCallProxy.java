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
import net.bytebuddy.modifier.Visibility;
import org.objectweb.asm.MethodVisitor;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isConstructor;

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

    /**
     * The prefix of the fields holding the original method invocation's arguments.
     */
    private static final String FIELD_NAME_PREFIX = "argument";

    /**
     * The special method invocation to invoke from the auxiliary type.
     */
    private final Instrumentation.SpecialMethodInvocation specialMethodInvocation;

    /**
     * Determines if the generated proxy should be serializableProxy.
     */
    private final boolean serializableProxy;

    /**
     * The assigner to use for invoking a bridge method target where the parameter and return types need to be
     * assigned.
     */
    private final Assigner assigner;

    /**
     * Creates a new method call proxy for a given method and uses a default assigner for assigning the method's return
     * value to either the {@link java.util.concurrent.Callable#call()} or {@link Runnable#run()} method returns.
     *
     * @param specialMethodInvocation The special method invocation which should be invoked by this method call proxy.
     * @param serializableProxy       Determines if the generated proxy should be serializableProxy.
     */
    public MethodCallProxy(Instrumentation.SpecialMethodInvocation specialMethodInvocation,
                           boolean serializableProxy) {
        this(specialMethodInvocation,
                serializableProxy,
                new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), true));
    }

    /**
     * Creates a new method call proxy for a given method.
     *
     * @param specialMethodInvocation The special method invocation which should be invoked by this method call proxy.
     * @param serializableProxy       Determines if the generated proxy should be serializableProxy.
     * @param assigner                An assigner for assigning the target method's return value to either the
     *                                {@link java.util.concurrent.Callable#call()} or {@link Runnable#run()}} methods'
     *                                return values.
     */
    public MethodCallProxy(Instrumentation.SpecialMethodInvocation specialMethodInvocation,
                           boolean serializableProxy,
                           Assigner assigner) {
        this.specialMethodInvocation = specialMethodInvocation;
        this.serializableProxy = serializableProxy;
        this.assigner = assigner;
    }

    /**
     * Creates a linked hash map of field names to their types where each field represents a parameter of the method.
     *
     * @param methodDescription The method to extract into fields.
     * @return A map of fields in the order they need to be loaded onto the operand stack for invoking the original
     * method, including a reference to the instance of the instrumented type that is invoked if applicable.
     */
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

    /**
     * Creates a field name for a method parameter of a given index.
     *
     * @param index The index for which the field name is to be created.
     * @return The name for the given parameter.
     */
    private static String fieldName(int index) {
        return String.format("%s%d", FIELD_NAME_PREFIX, index);
    }

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        MethodDescription accessorMethod = methodAccessorFactory.registerAccessorFor(specialMethodInvocation);
        LinkedHashMap<String, TypeDescription> parameterFields = extractFields(accessorMethod);
        DynamicType.Builder<?> builder = new ByteBuddy(classFileVersion)
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .methodLookupEngine(ProxyMethodLookupEngine.INSTANCE)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .implement(Runnable.class, Callable.class).intercept(new MethodCall(accessorMethod, assigner))
                .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                .defineConstructor(new ArrayList<TypeDescription>(parameterFields.values()))
                .intercept(ConstructorCall.INSTANCE);
        for (Map.Entry<String, TypeDescription> field : parameterFields.entrySet()) {
            builder = builder.defineField(field.getKey(), field.getValue(), Visibility.PRIVATE);
        }
        return builder.make();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodCallProxy that = (MethodCallProxy) other;
        return serializableProxy == that.serializableProxy
                && assigner.equals(that.assigner)
                && specialMethodInvocation.equals(that.specialMethodInvocation);
    }

    @Override
    public int hashCode() {
        int result = specialMethodInvocation.hashCode();
        result = 31 * result + (serializableProxy ? 1 : 0);
        result = 31 * result + assigner.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MethodCallProxy{" +
                "specialMethodInvocation=" + specialMethodInvocation +
                ", serializableProxy=" + serializableProxy +
                ", assigner=" + assigner +
                '}';
    }

    /**
     * A method lookup engine with hard-coded information about the methods to be implemented by a
     * {@link net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy}. This avoids a reflective lookup
     * of these methods what improves the runtime performance of this lookup.
     */
    protected static enum ProxyMethodLookupEngine implements MethodLookupEngine, MethodLookupEngine.Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * The list of methods to be returned by this method lookup engine.
         */
        private final MethodList methodList;

        /**
         * Creates this singleton proxy method lookup engine.
         */
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
        public MethodLookupEngine make(boolean extractDefaultMethods) {
            return this;
        }
    }

    /**
     * An instrumentation for implementing a method of a {@link net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy}.
     */
    protected static class MethodCall implements Instrumentation {

        /**
         * The method that is accessed by the implemented method.
         */
        private final MethodDescription accessorMethod;

        /**
         * The assigner to be used for invoking the accessor method.
         */
        private final Assigner assigner;

        /**
         * Creates a new method call instrumentation.
         *
         * @param accessorMethod The method that is accessed by the implemented method.
         * @param assigner       The assigner to be used for invoking the accessor method.
         */
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
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && accessorMethod.equals(((MethodCall) other).accessorMethod)
                    && assigner.equals(((MethodCall) other).assigner);
        }

        @Override
        public int hashCode() {
            return accessorMethod.hashCode() + 31 * assigner.hashCode();
        }

        @Override
        public String toString() {
            return "MethodCallProxy.MethodCall{" +
                    "accessorMethod=" + accessorMethod +
                    ", assigner=" + assigner +
                    '}';
        }

        /**
         * The appender for implementing the {@link net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy.MethodCall}.
         */
        protected class Appender implements ByteCodeAppender {

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

            @Override
            public boolean appendsCode() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor,
                              Context instrumentationContext,
                              MethodDescription instrumentedMethod) {
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
                        && instrumentedType.equals(((Appender) other).instrumentedType)
                        && MethodCall.this.equals(((Appender) other).getMethodCall());
            }

            @Override
            public int hashCode() {
                return 31 * MethodCall.this.hashCode() + instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCallProxy.MethodCall.Appender{" +
                        "methodCall=" + MethodCall.this +
                        ", instrumentedType=" + instrumentedType +
                        '}';
            }
        }
    }

    /**
     * An instrumentation for implementing a constructor of a {@link net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy}.
     */
    protected static enum ConstructorCall implements Instrumentation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A reference of the {@link Object} type default constructor.
         */
        private final MethodDescription objectTypeDefaultConstructor;

        /**
         * Creates the constructor call singleton.
         */
        private ConstructorCall() {
            this.objectTypeDefaultConstructor = new TypeDescription.ForLoadedType(Object.class)
                    .getDeclaredMethods()
                    .filter(isConstructor())
                    .getOnly();
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return new Appender(instrumentationTarget.getTypeDescription());
        }

        /**
         * The appender for implementing the {@link net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy.ConstructorCall}.
         */
        protected static class Appender implements ByteCodeAppender {

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

            @Override
            public boolean appendsCode() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
                StackManipulation thisReference = MethodVariableAccess.REFERENCE.loadFromIndex(0);
                FieldList fieldList = instrumentedType.getDeclaredFields();
                StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                int index = 0;
                for (FieldDescription fieldDescription : fieldList) {
                    fieldLoading[index] = new StackManipulation.Compound(
                            thisReference,
                            MethodVariableAccess.forType(fieldDescription.getFieldType())
                                    .loadFromIndex(instrumentedMethod.getParameterOffset(index)),
                            FieldAccess.forField(fieldDescription).putter()
                    );
                    index++;
                }
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        thisReference,
                        MethodInvocation.invoke(ConstructorCall.INSTANCE.objectTypeDefaultConstructor),
                        new StackManipulation.Compound(fieldLoading),
                        MethodReturn.VOID
                ).apply(methodVisitor, instrumentationContext);
                return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
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
                return "MethodCallProxy.ConstructorCall.Appender{instrumentedType=" + instrumentedType + '}';
            }
        }
    }

    /**
     * A stack manipulation that creates a {@link net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy}
     * for a given method an pushes such an object onto the call stack. For this purpose, all arguments of the proxied method
     * are loaded onto the stack what is only possible if this instance is used from a method with an identical signature such
     * as the target method itself.
     */
    public static class AssignableSignatureCall implements StackManipulation {

        /**
         * The special method invocation to be proxied by this stack manipulation.
         */
        private final Instrumentation.SpecialMethodInvocation specialMethodInvocation;

        /**
         * Determines if the generated proxy should be serializableProxy.
         */
        private final boolean serializable;

        /**
         * Creates an operand stack assignment that creates a
         * {@link net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy} for the
         * {@code targetMethod} and pushes this proxy object onto the stack.
         *
         * @param specialMethodInvocation The special method invocation which should be invoked by the created method
         *                                call proxy.
         * @param serializable            Determines if the generated proxy should be serializableProxy.
         */
        public AssignableSignatureCall(Instrumentation.SpecialMethodInvocation specialMethodInvocation,
                                       boolean serializable) {
            this.specialMethodInvocation = specialMethodInvocation;
            this.serializable = serializable;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            TypeDescription auxiliaryType = instrumentationContext
                    .register(new MethodCallProxy(specialMethodInvocation, serializable));
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
                    && serializable == ((AssignableSignatureCall) other).serializable
                    && specialMethodInvocation.equals(((AssignableSignatureCall) other).specialMethodInvocation);
        }

        @Override
        public int hashCode() {
            return 31 * specialMethodInvocation.hashCode() + (serializable ? 1 : 0);
        }

        @Override
        public String toString() {
            return "MethodCallProxy.AssignableSignatureCall{" +
                    "specialMethodInvocation=" + specialMethodInvocation +
                    ", serializableProxy=" + serializable +
                    '}';
        }
    }
}
