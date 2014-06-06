package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This instrumentation forwards method invocations to another instance. For this, the intercepted method must be
 * defined on a super type of the given delegation target. Static methods cannot be forwarded as they are not
 * invoked on an instance.
 */
public class Forwarding implements Instrumentation {

    /**
     * The prefix of any implicit field name for storing a delegate..
     */
    private static final String FIELD_PREFIX = "forwarding";

    /**
     * The name of the field.
     */
    protected final String fieldName;

    /**
     * The type of the field.
     */
    protected final TypeDescription fieldType;

    /**
     * A handler for preparing the instrumented type and the field invocation operation.
     */
    protected final PreparationHandler preparationHandler;

    /**
     * Creates a new forwarding instrumentation.
     *
     * @param fieldName          The name of the field.
     * @param fieldType          The type of the field.
     * @param preparationHandler A handler for preparing the instrumented type and the field invocation operation.
     */
    protected Forwarding(String fieldName, TypeDescription fieldType, PreparationHandler preparationHandler) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.preparationHandler = preparationHandler;
    }

    /**
     * Forwards all intercepted method invocations to the given instance which is stored in a {@code static} field
     * of the instrumented class. The field name is generated from the instance's hash code.
     *
     * @param delegate The delegate to which all intercepted methods should be forwarded.
     * @return A corresponding instrumentation.
     */
    public static Instrumentation to(Object delegate) {
        return to(delegate, String.format("%s$%d", FIELD_PREFIX, delegate.hashCode()));
    }

    /**
     * Forwards all intercepted method invocations to the given instance which is stored in a {@code static} field
     * of the instrumented class.
     *
     * @param delegate  The delegate to which all intercepted methods should be forwarded.
     * @param fieldName The name of the field in which the delegate should be stored.
     * @return A corresponding instrumentation.
     */
    public static Instrumentation to(Object delegate, String fieldName) {
        return new Forwarding(fieldName,
                new TypeDescription.ForLoadedType(delegate.getClass()),
                new PreparationHandler.ForStaticInstance(delegate));
    }

    /**
     * Forwards all intercepted method invocations to a {@code static} field of the instrumented class. The value
     * of this field must be set explicitly.
     *
     * @param fieldName The name of the field in which the delegate should be stored.
     * @param fieldType The type of the field and thus the type of which the delegate is assumed to be of.
     * @return A corresponding instrumentation.
     */
    public static Instrumentation toStaticField(String fieldName, Class<?> fieldType) {
        return new Forwarding(fieldName,
                new TypeDescription.ForLoadedType(fieldType),
                PreparationHandler.ForStaticField.INSTANCE);
    }

    /**
     * Forwards all intercepted method invocations to an instance field of the instrumented class. The value
     * of this field must be set explicitly.
     *
     * @param fieldName The name of the field in which the delegate should be stored.
     * @param fieldType The type of the field and thus the type of which the delegate is assumed to be of.
     * @return A corresponding instrumentation.
     */
    public static Instrumentation toInstanceField(String fieldName, Class<?> fieldType) {
        return new Forwarding(fieldName,
                new TypeDescription.ForLoadedType(fieldType),
                PreparationHandler.ForInstanceField.INSTANCE);
    }

    @Override
    public ByteCodeAppender appender(Target instrumentationTarget) {
        return new Appender(loadDelegate(instrumentationTarget.getTypeDescription()));
    }

    /**
     * Loads the field onto the operand stack.
     *
     * @param instrumentedType The instrumented type that declares the field.
     * @return A stack manipulation for loading the field value onto the operand stack.
     */
    private StackManipulation loadDelegate(TypeDescription instrumentedType) {
        return new StackManipulation.Compound(preparationHandler.loadFieldOwner(),
                FieldAccess.forField(instrumentedType.getDeclaredFields().named(fieldName)).getter());
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return preparationHandler.prepare(instrumentedType, fieldName, fieldType);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Forwarding that = (Forwarding) other;
        return fieldName.equals(that.fieldName)
                && fieldType.equals(that.fieldType)
                && preparationHandler.equals(that.preparationHandler);
    }

    @Override
    public int hashCode() {
        int result = fieldName.hashCode();
        result = 31 * result + fieldType.hashCode();
        result = 31 * result + preparationHandler.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Forwarding{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldType=" + fieldType +
                ", preparationHandler=" + preparationHandler +
                '}';
    }

    /**
     * A handler for preparing a {@link net.bytebuddy.instrumentation.Forwarding} instrumentation.
     */
    protected interface PreparationHandler {

        /**
         * Prepares the instrumented type.
         *
         * @param instrumentedType The instrumented type to prepare.
         * @param fieldName        The name of the field in which the delegate should be stored.
         * @param fieldType        The type of the field.
         * @return The prepared instrumented type.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType, String fieldName, TypeDescription fieldType);

        /**
         * Creates a stack manipulation for loading the field owner onto the operand stack.
         *
         * @return A stack manipulation for loading the field owner onto the operand stack.
         */
        StackManipulation loadFieldOwner();

        /**
         * A preparation handler for an unset instance that is stored in an instance field.
         */
        static enum ForInstanceField implements PreparationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType, String fieldName, TypeDescription fieldType) {
                return instrumentedType.withField(fieldName, fieldType, Opcodes.ACC_PRIVATE);
            }

            @Override
            public StackManipulation loadFieldOwner() {
                return MethodVariableAccess.REFERENCE.loadFromIndex(0);
            }
        }

        /**
         * A preparation handler for an unset instance that is stored in a {@code static} field.
         */
        static enum ForStaticField implements PreparationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType, String fieldName, TypeDescription fieldType) {
                return instrumentedType.withField(fieldName, fieldType, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
            }

            @Override
            public StackManipulation loadFieldOwner() {
                return StackManipulation.LegalTrivial.INSTANCE;
            }
        }

        /**
         * A preparation handler for an explicit instance that is stored in a {@code static} field.
         */
        static class ForStaticInstance implements PreparationHandler {

            /**
             * The target of the delegation.
             */
            private final Object target;

            /**
             * Creates a new preparation handler for an explicit instance.
             *
             * @param target The target of the delegation.
             */
            public ForStaticInstance(Object target) {
                this.target = target;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType, String fieldName, TypeDescription fieldType) {
                return instrumentedType
                        .withField(fieldName, fieldType, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)
                        .withInitializer(new TypeInitializer.ForStaticField<Object>(fieldName, target, true));
            }

            @Override
            public StackManipulation loadFieldOwner() {
                return StackManipulation.LegalTrivial.INSTANCE;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && target.equals(((ForStaticInstance) other).target);
            }

            @Override
            public int hashCode() {
                return target.hashCode();
            }

            @Override
            public String toString() {
                return "Forwarding.PreparationHandler.ForStaticInstance{target=" + target + '}';
            }
        }
    }

    /**
     * An appender for implementing a {@link net.bytebuddy.instrumentation.Forwarding} operation.
     */
    private class Appender implements ByteCodeAppender {

        /**
         * The stack manipulation for loading the delegate onto the stack, i.e. the field loading operation.
         */
        private final StackManipulation delegateLoadingInstruction;

        /**
         * Creates a new appender.
         *
         * @param delegateLoadingInstruction The stack manipulation for loading the delegate onto the stack, i.e.
         *                                   the field loading operation.
         */
        private Appender(StackManipulation delegateLoadingInstruction) {
            this.delegateLoadingInstruction = delegateLoadingInstruction;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            if (!instrumentedMethod.getDeclaringType().isAssignableFrom(fieldType)) {
                throw new IllegalArgumentException("Cannot forward " + instrumentedMethod + " to " + fieldType);
            } else if (instrumentedMethod.isStatic()) {
                throw new IllegalArgumentException("Cannot forward the static method " + instrumentedMethod);
            }
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    delegateLoadingInstruction,
                    MethodVariableAccess.loadArguments(instrumentedMethod),
                    MethodInvocation.invoke(instrumentedMethod).virtual(fieldType),
                    MethodReturn.returning(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && delegateLoadingInstruction.equals(((Appender) other).delegateLoadingInstruction)
                    && Forwarding.this.equals(((Appender) other).getForwarding());
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private Forwarding getForwarding() {
            return Forwarding.this;
        }

        @Override
        public int hashCode() {
            return Forwarding.this.hashCode() + 31 * delegateLoadingInstruction.hashCode();
        }

        @Override
        public String toString() {
            return "Forwarding.Appender{delegateLoadingInstruction=" + delegateLoadingInstruction + '}';
        }
    }
}
