package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Type;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * This implementation forwards method invocations to another instance. For this, the intercepted method must be
 * defined on a super type of the given delegation target. Static methods cannot be forwarded as they are not
 * invoked on an instance.
 *
 * @see MethodDelegation
 */
public class Forwarding implements Implementation {

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
    protected final TypeDescription.Generic fieldType;

    /**
     * A handler for preparing the instrumented type and the field invocation operation.
     */
    protected final PreparationHandler preparationHandler;

    /**
     * Creates a new forwarding implementation.
     *
     * @param fieldName          The name of the field.
     * @param fieldType          The type of the field.
     * @param preparationHandler A handler for preparing the instrumented type and the field invocation operation.
     */
    protected Forwarding(String fieldName, TypeDescription.Generic fieldType, PreparationHandler preparationHandler) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.preparationHandler = preparationHandler;
    }

    /**
     * Forwards all intercepted method invocations to the given instance which is stored in a {@code static} field
     * of the instrumented class. The field name is generated from the instance's hash code.
     *
     * @param delegate The delegate to which all intercepted methods should be forwarded.
     * @return A corresponding implementation.
     */
    public static Implementation to(Object delegate) {
        return to(delegate, String.format("%s$%d", FIELD_PREFIX, Math.abs(delegate.hashCode() % Integer.MAX_VALUE)));
    }

    /**
     * Forwards all intercepted method invocations to the given instance which is stored in a {@code static} field
     * of the instrumented class.
     *
     * @param delegate  The delegate to which all intercepted methods should be forwarded.
     * @param fieldName The name of the field in which the delegate should be stored.
     * @return A corresponding implementation.
     */
    public static Implementation to(Object delegate, String fieldName) {
        return new Forwarding(fieldName,
                new TypeDescription.Generic.OfNonGenericType.ForLoadedType(delegate.getClass()),
                new PreparationHandler.ForStaticInstance(delegate));
    }

    /**
     * Forwards all intercepted method invocations to a {@code static} field of the instrumented class. The value
     * of this field must be set explicitly.
     *
     * @param fieldName The name of the field in which the delegate should be stored.
     * @param fieldType The type of the field and thus the type of which the delegate is assumed to be of.
     * @return A corresponding implementation.
     */
    public static Implementation toStaticField(String fieldName, Type fieldType) {
        return toStaticField(fieldName, TypeDefinition.Sort.describe(fieldType));
    }

    /**
     * Forwards all intercepted method invocations to a {@code static} field of the instrumented class. The value
     * of this field must be set explicitly.
     *
     * @param fieldName The name of the field in which the delegate should be stored.
     * @param fieldType The type of the field and thus the type of which the delegate is assumed to be of.
     * @return A corresponding implementation.
     */
    public static Implementation toStaticField(String fieldName, TypeDefinition fieldType) {
        return new Forwarding(fieldName, fieldType.asGenericType(), PreparationHandler.ForStaticField.INSTANCE);
    }

    /**
     * Forwards all intercepted method invocations to an instance field of the instrumented class. The value
     * of this field must be set explicitly.
     *
     * @param fieldName The name of the field in which the delegate should be stored.
     * @param fieldType The type of the field and thus the type of which the delegate is assumed to be of.
     * @return A corresponding implementation.
     */
    public static Implementation toInstanceField(String fieldName, Type fieldType) {
        return toInstanceField(fieldName, TypeDefinition.Sort.describe(fieldType));
    }

    /**
     * Forwards all intercepted method invocations to an instance field of the instrumented class. The value
     * of this field must be set explicitly.
     *
     * @param fieldName The name of the field in which the delegate should be stored.
     * @param fieldType The type of the field and thus the type of which the delegate is assumed to be of.
     * @return A corresponding implementation.
     */
    public static Implementation toInstanceField(String fieldName, TypeDefinition fieldType) {
        return new Forwarding(fieldName, fieldType.asGenericType(), PreparationHandler.ForInstanceField.INSTANCE);
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(loadDelegate(implementationTarget.getInstrumentedType()));
    }

    /**
     * Loads the field onto the operand stack.
     *
     * @param instrumentedType The instrumented type that declares the field.
     * @return A stack manipulation for loading the field value onto the operand stack.
     */
    private StackManipulation loadDelegate(TypeDescription instrumentedType) {
        return new StackManipulation.Compound(preparationHandler.loadFieldOwner(),
                FieldAccess.forField(instrumentedType.getDeclaredFields().filter((named(fieldName))).getOnly()).getter());
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
     * A handler for preparing a {@link net.bytebuddy.implementation.Forwarding} implementation.
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
        InstrumentedType prepare(InstrumentedType instrumentedType, String fieldName, TypeDescription.Generic fieldType);

        /**
         * Creates a stack manipulation for loading the field owner onto the operand stack.
         *
         * @return A stack manipulation for loading the field owner onto the operand stack.
         */
        StackManipulation loadFieldOwner();

        /**
         * A preparation handler for an unset instance that is stored in an instance field.
         */
        enum ForInstanceField implements PreparationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType, String fieldName, TypeDescription.Generic fieldType) {
                if (instrumentedType.isInterface()) {
                    throw new IllegalStateException("Cannot define instance field '" + fieldName + "' for " + instrumentedType);
                }
                return instrumentedType.withField(new FieldDescription.Token(fieldName, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, fieldType));
            }

            @Override
            public StackManipulation loadFieldOwner() {
                return MethodVariableAccess.REFERENCE.loadOffset(0);
            }

            @Override
            public String toString() {
                return "Forwarding.PreparationHandler.ForInstanceField." + name();
            }
        }

        /**
         * A preparation handler for an unset instance that is stored in a {@code static} field.
         */
        enum ForStaticField implements PreparationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType, String fieldName, TypeDescription.Generic fieldType) {
                return instrumentedType.withField(new FieldDescription.Token(fieldName, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, fieldType));
            }

            @Override
            public StackManipulation loadFieldOwner() {
                return StackManipulation.Trivial.INSTANCE;
            }

            @Override
            public String toString() {
                return "Forwarding.PreparationHandler.ForStaticField." + name();
            }
        }

        /**
         * A preparation handler for an explicit instance that is stored in a {@code static} field.
         */
        class ForStaticInstance implements PreparationHandler {

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
            public InstrumentedType prepare(InstrumentedType instrumentedType, String fieldName, TypeDescription.Generic fieldType) {
                return instrumentedType
                        .withField(new FieldDescription.Token(fieldName, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, fieldType))
                        .withInitializer(new LoadedTypeInitializer.ForStaticField(fieldName, target));
            }

            @Override
            public StackManipulation loadFieldOwner() {
                return StackManipulation.Trivial.INSTANCE;
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
     * An appender for implementing a {@link net.bytebuddy.implementation.Forwarding} operation.
     */
    protected class Appender implements ByteCodeAppender {

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
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (!instrumentedMethod.isInvokableOn(fieldType.asErasure())) {
                throw new IllegalArgumentException("Cannot forward " + instrumentedMethod + " to " + fieldType);
            } else if (instrumentedMethod.isStatic()) {
                throw new IllegalArgumentException("Cannot forward the static method " + instrumentedMethod);
            }
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    delegateLoadingInstruction,
                    MethodVariableAccess.allArgumentsOf(instrumentedMethod),
                    MethodInvocation.invoke(instrumentedMethod).virtual(fieldType.asErasure()),
                    MethodReturn.of(instrumentedMethod.getReturnType().asErasure())
            ).apply(methodVisitor, implementationContext);
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
