package net.bytebuddy.implementation.auxiliary;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;

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
    private final Implementation.SpecialMethodInvocation specialMethodInvocation;

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
    public MethodCallProxy(Implementation.SpecialMethodInvocation specialMethodInvocation, boolean serializableProxy) {
        this(specialMethodInvocation, serializableProxy, Assigner.DEFAULT);
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
    public MethodCallProxy(Implementation.SpecialMethodInvocation specialMethodInvocation, boolean serializableProxy, Assigner assigner) {
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
        LinkedHashMap<String, TypeDescription> typeDescriptions = new LinkedHashMap<String, TypeDescription>();
        int currentIndex = 0;
        if (!methodDescription.isStatic()) {
            typeDescriptions.put(fieldName(currentIndex++), methodDescription.getDeclaringType().asErasure());
        }
        for (ParameterDescription parameterDescription : methodDescription.getParameters()) {
            typeDescriptions.put(fieldName(currentIndex++), parameterDescription.getType().asErasure());
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
                .with(PrecomputedMethodGraph.INSTANCE)
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .implement(Runnable.class, Callable.class).intercept(new MethodCall(accessorMethod, assigner))
                .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                .defineConstructor().withParameters(parameterFields.values())
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
     * A precomputed method graph that only displays the methods that are relevant for creating a method call proxy.
     */
    protected enum PrecomputedMethodGraph implements MethodGraph.Compiler {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * The precomputed method graph.
         */
        private final MethodGraph.Linked methodGraph;

        /**
         * Creates the precomputed method graph.
         */
        @SuppressFBWarnings(value = "SE_BAD_FIELD_STORE", justification = "Precomputed method graph is not intended for serialization")
        PrecomputedMethodGraph() {
            LinkedHashMap<MethodDescription.SignatureToken, MethodGraph.Node> nodes = new LinkedHashMap<MethodDescription.SignatureToken, MethodGraph.Node>();
            MethodDescription callMethod = new MethodDescription.Latent(new TypeDescription.ForLoadedType(Callable.class),
                    "call",
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    Collections.<TypeVariableToken>emptyList(),
                    TypeDescription.Generic.OBJECT,
                    Collections.<ParameterDescription.Token>emptyList(),
                    Collections.singletonList(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Exception.class)),
                    Collections.<AnnotationDescription>emptyList(),
                    AnnotationValue.UNDEFINED,
                    TypeDescription.Generic.UNDEFINED);
            nodes.put(callMethod.asSignatureToken(), new MethodGraph.Node.Simple(callMethod));
            MethodDescription runMethod = new MethodDescription.Latent(new TypeDescription.ForLoadedType(Runnable.class),
                    "run",
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    Collections.<TypeVariableToken>emptyList(),
                    TypeDescription.Generic.VOID,
                    Collections.<ParameterDescription.Token>emptyList(),
                    Collections.<TypeDescription.Generic>emptyList(),
                    Collections.<AnnotationDescription>emptyList(),
                    AnnotationValue.UNDEFINED,
                    TypeDescription.Generic.UNDEFINED);
            nodes.put(runMethod.asSignatureToken(), new MethodGraph.Node.Simple(runMethod));
            MethodGraph methodGraph = new MethodGraph.Simple(nodes);
            this.methodGraph = new MethodGraph.Linked.Delegation(methodGraph, methodGraph, Collections.<TypeDescription, MethodGraph>emptyMap());
        }

        @Override
        public MethodGraph.Linked compile(TypeDescription typeDescription) {
            return compile(typeDescription, typeDescription);
        }

        @Override
        public MethodGraph.Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint) {
            return methodGraph;
        }

        @Override
        public String toString() {
            return "MethodCallProxy.PrecomputedMethodGraph." + name();
        }
    }

    /**
     * An implementation for a constructor of a {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy}.
     */
    protected enum ConstructorCall implements Implementation {

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
        ConstructorCall() {
            objectTypeDefaultConstructor = TypeDescription.OBJECT.getDeclaredMethods().filter(isConstructor()).getOnly();
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType());
        }

        @Override
        public String toString() {
            return "MethodCallProxy.ConstructorCall." + name();
        }

        /**
         * The appender for implementing the {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy.ConstructorCall}.
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
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                StackManipulation thisReference = MethodVariableAccess.REFERENCE.loadOffset(0);
                FieldList<?> fieldList = instrumentedType.getDeclaredFields();
                StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                int index = 0;
                for (FieldDescription fieldDescription : fieldList) {
                    fieldLoading[index] = new StackManipulation.Compound(
                            thisReference,
                            MethodVariableAccess.of(fieldDescription.getType().asErasure())
                                    .loadOffset(instrumentedMethod.getParameters().get(index).getOffset()),
                            FieldAccess.forField(fieldDescription).putter()
                    );
                    index++;
                }
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        thisReference,
                        MethodInvocation.invoke(ConstructorCall.INSTANCE.objectTypeDefaultConstructor),
                        new StackManipulation.Compound(fieldLoading),
                        MethodReturn.VOID
                ).apply(methodVisitor, implementationContext);
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
     * An implementation for a method of a {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy}.
     */
    protected static class MethodCall implements Implementation {

        /**
         * The method that is accessed by the implemented method.
         */
        private final MethodDescription accessorMethod;

        /**
         * The assigner to be used for invoking the accessor method.
         */
        private final Assigner assigner;

        /**
         * Creates a new method call implementation.
         *
         * @param accessorMethod The method that is accessed by the implemented method.
         * @param assigner       The assigner to be used for invoking the accessor method.
         */
        protected MethodCall(MethodDescription accessorMethod, Assigner assigner) {
            this.accessorMethod = accessorMethod;
            this.assigner = assigner;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType());
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
         * The appender for implementing the {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy.MethodCall}.
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
            public Size apply(MethodVisitor methodVisitor,
                              Context implementationContext,
                              MethodDescription instrumentedMethod) {
                StackManipulation thisReference = MethodVariableAccess.of(instrumentedType).loadOffset(0);
                FieldList<?> fieldList = instrumentedType.getDeclaredFields();
                StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                int index = 0;
                for (FieldDescription fieldDescription : fieldList) {
                    fieldLoading[index++] = new StackManipulation.Compound(thisReference, FieldAccess.forField(fieldDescription).getter());
                }
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        new StackManipulation.Compound(fieldLoading),
                        MethodInvocation.invoke(accessorMethod),
                        assigner.assign(accessorMethod.getReturnType(), instrumentedMethod.getReturnType(), Assigner.Typing.DYNAMIC),
                        MethodReturn.of(instrumentedMethod.getReturnType().asErasure())
                ).apply(methodVisitor, implementationContext);
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
     * A stack manipulation that creates a {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy}
     * for a given method an pushes such an object onto the call stack. For this purpose, all arguments of the proxied method
     * are loaded onto the stack what is only possible if this instance is used from a method with an identical signature such
     * as the target method itself.
     */
    public static class AssignableSignatureCall implements StackManipulation {

        /**
         * The special method invocation to be proxied by this stack manipulation.
         */
        private final Implementation.SpecialMethodInvocation specialMethodInvocation;

        /**
         * Determines if the generated proxy should be serializableProxy.
         */
        private final boolean serializable;

        /**
         * Creates an operand stack assignment that creates a
         * {@link net.bytebuddy.implementation.auxiliary.MethodCallProxy} for the
         * {@code targetMethod} and pushes this proxy object onto the stack.
         *
         * @param specialMethodInvocation The special method invocation which should be invoked by the created method
         *                                call proxy.
         * @param serializable            Determines if the generated proxy should be serializableProxy.
         */
        public AssignableSignatureCall(Implementation.SpecialMethodInvocation specialMethodInvocation,
                                       boolean serializable) {
            this.specialMethodInvocation = specialMethodInvocation;
            this.serializable = serializable;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            TypeDescription auxiliaryType = implementationContext
                    .register(new MethodCallProxy(specialMethodInvocation, serializable));
            return new Compound(
                    TypeCreation.of(auxiliaryType),
                    Duplication.SINGLE,
                    MethodVariableAccess.allArgumentsOf(specialMethodInvocation.getMethodDescription()).prependThisReference(),
                    MethodInvocation.invoke(auxiliaryType.getDeclaredMethods().filter(isConstructor()).getOnly())
            ).apply(methodVisitor, implementationContext);
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
