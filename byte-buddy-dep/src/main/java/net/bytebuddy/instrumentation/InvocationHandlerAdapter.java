package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.LegalTrivialStackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayFactory;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.isValidIdentifier;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * An adapter for adapting an {@link java.lang.reflect.InvocationHandler}. The adapter allows the invocation handler
 * to also intercept method calls to non-interface methods.
 */
public abstract class InvocationHandlerAdapter implements Instrumentation {

    private static final String PREFIX = "invocationHandler";
    /**
     * The name of the field for storing an invocation handler.
     */
    protected final String fieldName;
    /**
     * The assigner that is used for assigning the return invocation handler's return value to the
     * intercepted method's return value.
     */
    protected final Assigner assigner;

    /**
     * Creates a new invocation handler for a given field.
     *
     * @param fieldName The name of the field.
     */
    protected InvocationHandlerAdapter(String fieldName) {
        this.fieldName = fieldName;
        this.assigner = new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), true);
    }

    /**
     * Creates an instrumentation for any instance of an {@link java.lang.reflect.InvocationHandler} that delegates
     * all method interceptions to the given instance which will be stored in a {@code static} field.
     *
     * @param invocationHandler The invocation handler to which all method calls are delegated.
     * @return An instrumentation that delegates all method interceptions to the given invocation handler.
     */
    public static Instrumentation of(InvocationHandler invocationHandler) {
        return of(invocationHandler, String.format("%s$%d", PREFIX, Math.abs(invocationHandler.hashCode())));
    }

    /**
     * Creates an instrumentation for any instance of an {@link java.lang.reflect.InvocationHandler} that delegates
     * all method interceptions to the given instance which will be stored in a {@code static} field.
     *
     * @param invocationHandler The invocation handler to which all method calls are delegated.
     * @param fieldName         The name of the field.
     * @return An instrumentation that delegates all method interceptions to the given invocation handler.
     */
    public static Instrumentation of(InvocationHandler invocationHandler, String fieldName) {
        return new ForStaticDelegation(nonNull(invocationHandler), isValidIdentifier(fieldName));
    }

    /**
     * Creates an instrumentation for any {@link java.lang.reflect.InvocationHandler} that delegates
     * all method interceptions to a {@code public} instance field with the given name. This field has to be
     * set before any invocations are intercepted. Otherwise, a {@link java.lang.NullPointerException} will be
     * thrown.
     *
     * @param fieldName The name of the field.
     * @return An instrumentation that delegates all method interceptions to an instance field of the given name.
     */
    public static Instrumentation of(String fieldName) {
        return new ForInstanceDelegation(isValidIdentifier(fieldName));
    }

    private static List<StackManipulation> argumentValuesOf(MethodDescription instrumentedMethod) {
        TypeList parameterTypes = instrumentedMethod.getParameterTypes();
        List<StackManipulation> instruction = new ArrayList<StackManipulation>(parameterTypes.size());
        int currentIndex = 1;
        for (TypeDescription parameterType : parameterTypes) {
            instruction.add(MethodVariableAccess.forType(parameterType).loadFromIndex(currentIndex));
            currentIndex += parameterType.getStackSize().getSize();
        }
        return instruction;
    }

    /**
     * Applies an instrumentation that delegates to a invocation handler.
     *
     * @param methodVisitor          The method visitor for writing the byte code to.
     * @param instrumentationContext The instrumentation context for the current instrumentation.
     * @param instrumentedMethod     The method that is instrumented.
     * @param instrumentedType       The type that is instrumented.
     * @param preparingManipulation  A stack manipulation that applies any preparation to the operand stack.
     * @return The size of the applied assignment.
     */
    protected ByteCodeAppender.Size apply(MethodVisitor methodVisitor,
                                          Context instrumentationContext,
                                          MethodDescription instrumentedMethod,
                                          TypeDescription instrumentedType,
                                          StackManipulation preparingManipulation) {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        TypeDescription invocationHandlerType = new TypeDescription.ForLoadedType(InvocationHandler.class);
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                preparingManipulation,
                FieldAccess.forField(instrumentedType.getDeclaredFields().named(fieldName)).getter(),
                MethodVariableAccess.forType(objectType).loadFromIndex(0),
                MethodConstant.forMethod(instrumentedMethod),
                ArrayFactory.targeting(objectType).withValues(argumentValuesOf(instrumentedMethod)),
                MethodInvocation.invoke(invocationHandlerType.getDeclaredMethods().getOnly()),
                assigner.assign(objectType, instrumentedMethod.getReturnType(), true),
                MethodReturn.returning(instrumentedMethod.getReturnType())
        ).apply(methodVisitor, instrumentationContext);
        return new ByteCodeAppender.Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && fieldName.equals(((InvocationHandlerAdapter) other).fieldName);
    }

    @Override
    public int hashCode() {
        return 31 * fieldName.hashCode();
    }

    private static class ForStaticDelegation extends InvocationHandlerAdapter {

        private final InvocationHandler invocationHandler;

        private ForStaticDelegation(InvocationHandler invocationHandler, String fieldName) {
            super(fieldName);
            this.invocationHandler = invocationHandler;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType
                    .withField(fieldName, new TypeDescription.ForLoadedType(InvocationHandler.class), Opcodes.ACC_STATIC)
                    .withInitializer(TypeInitializer.ForStaticField.nonAccessible(fieldName, invocationHandler));
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new Appender(instrumentedType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && super.equals(other)
                    && invocationHandler.equals(((ForStaticDelegation) other).invocationHandler);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + invocationHandler.hashCode();
        }

        @Override
        public String toString() {
            return "InvocationHandlerAdapter.ForStaticDelegation{" +
                    "fieldName=" + fieldName +
                    "invocationHandler=" + invocationHandler +
                    '}';
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
            public Size apply(MethodVisitor methodVisitor,
                              Context instrumentationContext,
                              MethodDescription instrumentedMethod) {
                return ForStaticDelegation.this.apply(methodVisitor,
                        instrumentationContext,
                        instrumentedMethod,
                        instrumentedType,
                        LegalTrivialStackManipulation.INSTANCE);
            }

            private InvocationHandlerAdapter getInvocationHandlerAdapter() {
                return ForStaticDelegation.this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((Appender) other).instrumentedType)
                        && ForStaticDelegation.this.equals(((Appender) other).getInvocationHandlerAdapter());
            }

            @Override
            public int hashCode() {
                return 31 * ForStaticDelegation.this.hashCode() + instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "InvocationHandlerAdapter.Appender{" +
                        "invocationHandlerAdapter=" + ForStaticDelegation.this +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }
        }
    }

    private static class ForInstanceDelegation extends InvocationHandlerAdapter {

        private ForInstanceDelegation(String fieldName) {
            super(fieldName);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withField(fieldName,
                    new TypeDescription.ForLoadedType(InvocationHandler.class),
                    Opcodes.ACC_PUBLIC);
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new Appender(instrumentedType);
        }

        @Override
        public String toString() {
            return "InvocationHandlerAdapter.ForInstanceDelegation{" +
                    "fieldName=" + fieldName +
                    '}';
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
                return ForInstanceDelegation.this.apply(methodVisitor,
                        instrumentationContext,
                        instrumentedMethod,
                        instrumentedType,
                        MethodVariableAccess.forType(instrumentedType).loadFromIndex(0));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((Appender) other).instrumentedType)
                        && ForInstanceDelegation.this.equals(((Appender) other).getInvocationHandlerAdapter());
            }

            private InvocationHandlerAdapter getInvocationHandlerAdapter() {
                return ForInstanceDelegation.this;
            }

            @Override
            public int hashCode() {
                return 31 * ForInstanceDelegation.this.hashCode() + instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "InvocationHandlerAdapter.Appender{" +
                        "invocationHandlerAdapter=" + ForInstanceDelegation.this +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }
        }
    }
}
