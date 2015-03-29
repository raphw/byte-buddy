package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
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

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.isValidIdentifier;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * An adapter for adapting an {@link java.lang.reflect.InvocationHandler}. The adapter allows the invocation handler
 * to also intercept method calls to non-interface methods.
 */
public abstract class InvocationHandlerAdapter implements Instrumentation {

    /**
     * The prefix for field that are created for storing the instrumented value.
     */
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
     * Determines if the {@link java.lang.reflect.Method} instances that are handed to the intercepted methods are
     * cached in {@code static} fields.
     */
    protected final boolean cacheMethods;

    /**
     * Creates a new invocation handler for a given field.
     *
     * @param fieldName    The name of the field.
     * @param cacheMethods Determines if the {@link java.lang.reflect.Method} instances that are handed to the
     *                     intercepted methods are cached in {@code static} fields.
     * @param assigner     The assigner to apply when defining this instrumentation.
     */
    protected InvocationHandlerAdapter(String fieldName, boolean cacheMethods, Assigner assigner) {
        this.fieldName = fieldName;
        this.cacheMethods = cacheMethods;
        this.assigner = assigner;
    }

    /**
     * Creates a default assigner to use.
     *
     * @return The default assigner.
     */
    private static Assigner defaultAssigner() {
        return new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE));
    }

    /**
     * Creates an instrumentation for any instance of an {@link java.lang.reflect.InvocationHandler} that delegates
     * all method interceptions to the given instance which will be stored in a {@code static} field.
     *
     * @param invocationHandler The invocation handler to which all method calls are delegated.
     * @return An instrumentation that delegates all method interceptions to the given invocation handler.
     */
    public static InvocationHandlerAdapter of(InvocationHandler invocationHandler) {
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
    public static InvocationHandlerAdapter of(InvocationHandler invocationHandler, String fieldName) {
        return new ForStaticDelegation(isValidIdentifier(fieldName), false, defaultAssigner(), nonNull(invocationHandler));
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
    public static InvocationHandlerAdapter toInstanceField(String fieldName) {
        return new ForInstanceDelegation(isValidIdentifier(fieldName), false, defaultAssigner());
    }

    /**
     * Returns a list of stack manipulations that loads all arguments of an instrumented method.
     *
     * @param instrumentedMethod The method that is instrumented.
     * @return A list of stack manipulation that loads all arguments of an instrumented method.
     */
    private List<StackManipulation> argumentValuesOf(MethodDescription instrumentedMethod) {
        TypeList parameterTypes = instrumentedMethod.getParameters().asTypeList();
        List<StackManipulation> instruction = new ArrayList<StackManipulation>(parameterTypes.size());
        TypeDescription objectType = TypeDescription.OBJECT;
        int currentIndex = 1;
        for (TypeDescription parameterType : parameterTypes) {
            instruction.add(new StackManipulation.Compound(
                    MethodVariableAccess.forType(parameterType).loadOffset(currentIndex),
                    assigner.assign(parameterType, objectType, false)));
            currentIndex += parameterType.getStackSize().getSize();
        }
        return instruction;
    }

    /**
     * By default, any {@link java.lang.reflect.Method} instance that is handed over to an
     * {@link java.lang.reflect.InvocationHandler} is created on each invocation of the method.
     * {@link java.lang.reflect.Method} look-ups are normally cached by its defining {@link java.lang.Class} what
     * makes a repeated look-up of a method little expensive. However, because {@link java.lang.reflect.Method}
     * instances are mutable by their {@link java.lang.reflect.AccessibleObject} contact, any looked-up instance
     * needs to be copied by its defining {@link java.lang.Class} before exposing it. This can cause performance
     * deficits when a method is for example called repeatedly in a loop. By enabling the method cache, this
     * performance penalty can be avoided by caching a single {@link java.lang.reflect.Method} instance for
     * any intercepted method as a {@code static} field in the instrumented type.
     *
     * @return A similar invocation handler adapter which caches any {@link java.lang.reflect.Method} instance
     * in form of a {@code static} field.
     */
    public abstract AssignerConfigurable withMethodCache();

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
        TypeDescription invocationHandlerType = new TypeDescription.ForLoadedType(InvocationHandler.class);
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                preparingManipulation,
                FieldAccess.forField(instrumentedType.getDeclaredFields()
                        .filter((named(fieldName))).getOnly()).getter(),
                MethodVariableAccess.forType(TypeDescription.OBJECT).loadOffset(0),
                cacheMethods
                        ? MethodConstant.forMethod(instrumentedMethod).cached()
                        : MethodConstant.forMethod(instrumentedMethod),
                ArrayFactory.targeting(TypeDescription.OBJECT).withValues(argumentValuesOf(instrumentedMethod)),
                MethodInvocation.invoke(invocationHandlerType.getDeclaredMethods().getOnly()),
                assigner.assign(TypeDescription.OBJECT, instrumentedMethod.getReturnType(), true),
                MethodReturn.returning(instrumentedMethod.getReturnType())
        ).apply(methodVisitor, instrumentationContext);
        return new ByteCodeAppender.Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        InvocationHandlerAdapter that = (InvocationHandlerAdapter) other;
        return cacheMethods == that.cacheMethods
                && assigner.equals(that.assigner)
                && fieldName.equals(that.fieldName);
    }

    @Override
    public int hashCode() {
        int result = fieldName.hashCode();
        result = 31 * result + assigner.hashCode();
        result = 31 * result + (cacheMethods ? 1 : 0);
        return result;
    }

    /**
     * Allows for the configuration of an {@link net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner}
     * of an {@link net.bytebuddy.instrumentation.InvocationHandlerAdapter}.
     */
    protected interface AssignerConfigurable extends Instrumentation {

        /**
         * Configures an assigner to use with this invocation handler adapter.
         *
         * @param assigner The assigner to apply when defining this instrumentation.
         * @return This instrumentation with the given {@code assigner} configured.
         */
        Instrumentation withAssigner(Assigner assigner);
    }

    /**
     * An implementation of an {@link net.bytebuddy.instrumentation.InvocationHandlerAdapter} that delegates method
     * invocations to an adapter that is stored in a static field.
     */
    protected static class ForStaticDelegation extends InvocationHandlerAdapter implements AssignerConfigurable {

        /**
         * The invocation handler to which method interceptions are to be delegated.
         */
        protected final InvocationHandler invocationHandler;

        /**
         * Creates a new invocation handler adapter for delegating invocations to an invocation handler that is stored
         * in a static field.
         *
         * @param fieldName         The name of the field.
         * @param cacheMethods      Determines if the {@link java.lang.reflect.Method} instances that are handed to the
         *                          intercepted methods are cached in {@code static} fields.
         * @param assigner          The assigner to apply when defining this instrumentation.
         * @param invocationHandler The invocation handler to which all method calls are delegated.
         */
        protected ForStaticDelegation(String fieldName,
                                      boolean cacheMethods,
                                      Assigner assigner,
                                      InvocationHandler invocationHandler) {
            super(fieldName, cacheMethods, assigner);
            this.invocationHandler = invocationHandler;
        }

        @Override
        public AssignerConfigurable withMethodCache() {
            return new ForStaticDelegation(fieldName, true, assigner, invocationHandler);
        }

        @Override
        public Instrumentation withAssigner(Assigner assigner) {
            return new ForStaticDelegation(fieldName, cacheMethods, nonNull(assigner), invocationHandler);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType
                    .withField(fieldName, new TypeDescription.ForLoadedType(InvocationHandler.class), Opcodes.ACC_STATIC)
                    .withInitializer(LoadedTypeInitializer.ForStaticField.nonAccessible(fieldName, invocationHandler));
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return new Appender(instrumentationTarget.getTypeDescription());
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
                    ", cacheMethods=" + cacheMethods +
                    ", invocationHandler=" + invocationHandler +
                    '}';
        }

        /**
         * An appender for implementing the {@link net.bytebuddy.instrumentation.InvocationHandlerAdapter.ForStaticDelegation}.
         */
        protected class Appender implements ByteCodeAppender {

            /**
             * The instrumented type for which the methods are being intercepted.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new appender.
             *
             * @param instrumentedType The type that is instrumented.
             */
            protected Appender(TypeDescription instrumentedType) {
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
                        StackManipulation.LegalTrivial.INSTANCE);
            }

            /**
             * Returns the outer class.
             *
             * @return The outer class of this instance.
             */
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
                return "InvocationHandlerAdapter.ForStaticDelegation.Appender{" +
                        "invocationHandlerAdapter=" + ForStaticDelegation.this +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }
        }
    }

    /**
     * An implementation of an {@link net.bytebuddy.instrumentation.InvocationHandlerAdapter} that delegates method
     * invocations to an adapter that is stored in an instance field.
     */
    protected static class ForInstanceDelegation extends InvocationHandlerAdapter implements AssignerConfigurable {

        /**
         * Creates a new invocation handler adapter for delegating invocations to an invocation handler that is stored
         * in an instance field.
         *
         * @param fieldName    The name of the field.
         * @param cacheMethods Determines if the {@link java.lang.reflect.Method} instances that are handed to the
         *                     intercepted methods are cached in {@code static} fields.
         * @param assigner     The assigner to apply when defining this instrumentation.
         */
        protected ForInstanceDelegation(String fieldName, boolean cacheMethods, Assigner assigner) {
            super(fieldName, cacheMethods, assigner);
        }

        @Override
        public AssignerConfigurable withMethodCache() {
            return new ForInstanceDelegation(fieldName, true, assigner);
        }

        @Override
        public Instrumentation withAssigner(Assigner assigner) {
            return new ForInstanceDelegation(fieldName, cacheMethods, nonNull(assigner));
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withField(fieldName,
                    new TypeDescription.ForLoadedType(InvocationHandler.class),
                    Opcodes.ACC_PUBLIC);
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return new Appender(instrumentationTarget.getTypeDescription());
        }

        @Override
        public String toString() {
            return "InvocationHandlerAdapter.ForInstanceDelegation{" +
                    "fieldName=" + fieldName +
                    "cacheMethods=" + cacheMethods +
                    '}';
        }

        /**
         * An appender for implementing the {@link net.bytebuddy.instrumentation.InvocationHandlerAdapter.ForInstanceDelegation}.
         */
        protected class Appender implements ByteCodeAppender {

            /**
             * The type that is subject of the instrumentation.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new appender.
             *
             * @param instrumentedType The type that is instrumented.
             */
            protected Appender(TypeDescription instrumentedType) {
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
                        MethodVariableAccess.forType(instrumentedType).loadOffset(0));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((Appender) other).instrumentedType)
                        && ForInstanceDelegation.this.equals(((Appender) other).getInvocationHandlerAdapter());
            }

            /**
             * Returns the outer class.
             *
             * @return The outer class.
             */
            private InvocationHandlerAdapter getInvocationHandlerAdapter() {
                return ForInstanceDelegation.this;
            }

            @Override
            public int hashCode() {
                return 31 * ForInstanceDelegation.this.hashCode() + instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "InvocationHandlerAdapter.ForInstanceDelegation.Appender{" +
                        "invocationHandlerAdapter=" + ForInstanceDelegation.this +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }
        }
    }
}
