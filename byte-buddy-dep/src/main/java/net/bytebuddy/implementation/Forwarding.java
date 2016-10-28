package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Type;

import static net.bytebuddy.matcher.ElementMatchers.genericFieldType;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * This implementation forwards method invocations to another instance. For this, the intercepted method must be
 * defined on a super type of the given delegation target. Static methods cannot be forwarded as they are not
 * invoked on an instance.
 *
 * @see MethodDelegation
 */
public class Forwarding implements Implementation.Composable {

    /**
     * The prefix of any implicit field name for storing a delegate..
     */
    private static final String FIELD_PREFIX = "forwarding";

    /**
     * A handler for preparing the instrumented type and the field invocation operation.
     */
    protected final PreparationHandler preparationHandler;

    /**
     * The termination handler to apply.
     */
    protected final TerminationHandler terminationHandler;

    /**
     * Creates a new forwarding implementation.
     *
     * @param preparationHandler A handler for preparing the instrumented type and the field invocation operation.
     * @param terminationHandler The termination handler to apply.
     */
    protected Forwarding(PreparationHandler preparationHandler, TerminationHandler terminationHandler) {
        this.preparationHandler = preparationHandler;
        this.terminationHandler = terminationHandler;
    }

    /**
     * Forwards all intercepted method invocations to the given instance which is stored in a {@code static} field
     * of the instrumented class. The field name is generated from the instance's hash code.
     *
     * @param delegate The delegate to which all intercepted methods should be forwarded.
     * @return A corresponding implementation.
     */
    public static Implementation.Composable to(Object delegate) {
        return to(delegate, delegate.getClass());
    }

    /**
     * Forwards all intercepted method invocations to the given instance which is stored in a {@code static} field
     * of the instrumented class.
     *
     * @param delegate  The delegate to which all intercepted methods should be forwarded.
     * @param fieldName The name of the field in which the delegate should be stored.
     * @return A corresponding implementation.
     */
    public static Implementation.Composable to(Object delegate, String fieldName) {
        return to(delegate, fieldName, delegate.getClass());
    }

    /**
     * Forwards all intercepted method invocations to the given instance which is stored in a {@code static} field
     * of the instrumented class.
     *
     * @param delegate The delegate to which all intercepted methods should be forwarded.
     * @param type     The type of the field. Must be a subtype of the delegate's type.
     * @return A corresponding implementation.
     */
    public static Implementation.Composable to(Object delegate, Type type) {
        return to(delegate, String.format("%s$%d", FIELD_PREFIX, Math.abs(delegate.hashCode() % Integer.MAX_VALUE)), type);
    }

    /**
     * Forwards all intercepted method invocations to the given instance which is stored in a {@code static} field
     * of the instrumented class.
     *
     * @param delegate  The delegate to which all intercepted methods should be forwarded.
     * @param fieldName The name of the field in which the delegate should be stored.
     * @param type      The type of the field. Must be a subtype of the delegate's type.
     * @return A corresponding implementation.
     */
    public static Implementation.Composable to(Object delegate, String fieldName, Type type) {
        TypeDescription.Generic typeDescription = TypeDefinition.Sort.describe(type);
        if (!typeDescription.asErasure().isInstance(delegate)) {
            throw new IllegalArgumentException(delegate + " is not of type " + type);
        }
        return new Forwarding(new PreparationHandler.ForInstance(fieldName, typeDescription, delegate), TerminationHandler.RETURNING);
    }

    /**
     * Delegates a method invocation to a field. The field's type must be compatible to the declaring type of the method.
     *
     * @param name The name of the field.
     * @return An implementation for a method forwarding that invokes the instrumented method on the given field.
     */
    public static Implementation.Composable toField(String name) {
        return toField(name, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
    }

    /**
     * Delegates a method invocation to a field. The field's type must be compatible to the declaring type of the method.
     *
     * @param name                The name of the field.
     * @param fieldLocatorFactory The field locator factory to use.
     * @return An implementation for a method forwarding that invokes the instrumented method on the given field.
     */
    public static Implementation.Composable toField(String name, FieldLocator.Factory fieldLocatorFactory) {
        return new Forwarding(new PreparationHandler.ForField(name, fieldLocatorFactory), TerminationHandler.RETURNING);
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(preparationHandler.resolve(implementationTarget.getInstrumentedType()), terminationHandler);
    }

    @Override
    public Implementation andThen(Implementation implementation) {
        return new Compound(new Forwarding(preparationHandler, TerminationHandler.DROPPING), implementation);
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return preparationHandler.prepare(instrumentedType);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Forwarding that = (Forwarding) object;
        return preparationHandler.equals(that.preparationHandler) && terminationHandler == that.terminationHandler;
    }

    @Override
    public int hashCode() {
        int result = preparationHandler.hashCode();
        result = 31 * result + terminationHandler.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Forwarding{" +
                "preparationHandler=" + preparationHandler +
                ", terminationHandler=" + terminationHandler +
                '}';
    }

    /**
     * A preparation handler is responsible for reading the field containing the forwarding instance.
     */
    protected interface PreparationHandler extends InstrumentedType.Prepareable {

        /**
         * Resolves the field to which to delegate.
         *
         * @param instrumentedType The instrumented type.
         * @return The field to which to delegate.
         */
        FieldDescription resolve(TypeDescription instrumentedType);

        /**
         * A preparation handler that delegates to a specific instance.
         */
        class ForInstance implements PreparationHandler {

            /**
             * The name of the field to delegate to.
             */
            private final String fieldName;

            /**
             * The type of the field.
             */
            private final TypeDescription.Generic typeDescription;

            /**
             * The delegate instance.
             */
            private final Object delegate;

            /**
             * Creates a new preparation handler for delegating to a field.
             *
             * @param fieldName       The name of the field to delegate to.
             * @param typeDescription The type of the field.
             * @param delegate        The delegate instance.
             */
            protected ForInstance(String fieldName, TypeDescription.Generic typeDescription, Object delegate) {
                this.fieldName = fieldName;
                this.typeDescription = typeDescription;
                this.delegate = delegate;
            }

            @Override
            public FieldDescription resolve(TypeDescription instrumentedType) {
                return instrumentedType.getDeclaredFields().filter(named(fieldName).and(genericFieldType(typeDescription))).getOnly();
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType
                        .withField(new FieldDescription.Token(fieldName, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, typeDescription))
                        .withInitializer(new LoadedTypeInitializer.ForStaticField(fieldName, delegate));
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForInstance that = (ForInstance) object;
                return fieldName.equals(that.fieldName)
                        && typeDescription.equals(that.typeDescription)
                        && delegate.equals(that.delegate);
            }

            @Override
            public int hashCode() {
                int result = fieldName.hashCode();
                result = 31 * result + typeDescription.hashCode();
                result = 31 * result + delegate.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "Forwarding.PreparationHandler.ForInstance{" +
                        "fieldName='" + fieldName + '\'' +
                        ", typeDescription=" + typeDescription +
                        ", delegate=" + delegate +
                        '}';
            }
        }

        /**
         * A preparation handler that delegates to a specific field.
         */
        class ForField implements PreparationHandler {

            /**
             * The name of the field to delegate to.
             */
            private final String fieldName;

            /**
             * The field locator factory to use.
             */
            private final FieldLocator.Factory fieldLocatorFactory;

            /**
             * Creates a new preparation handler for forwarding to a specific field.
             *
             * @param fieldName           The name of the field to delegate to.
             * @param fieldLocatorFactory The field locator factory to use.
             */
            protected ForField(String fieldName, FieldLocator.Factory fieldLocatorFactory) {
                this.fieldName = fieldName;
                this.fieldLocatorFactory = fieldLocatorFactory;
            }

            @Override
            public FieldDescription resolve(TypeDescription instrumentedType) {
                FieldLocator.Resolution resolution = fieldLocatorFactory.make(instrumentedType).locate(fieldName);
                if (!resolution.isResolved()) {
                    throw new IllegalStateException();
                }
                return resolution.getField();
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForField forField = (ForField) object;
                return fieldName.equals(forField.fieldName) && fieldLocatorFactory.equals(forField.fieldLocatorFactory);
            }

            @Override
            public int hashCode() {
                int result = fieldName.hashCode();
                result = 31 * result + fieldLocatorFactory.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "Forwarding.PreparationHandler.ForField{" +
                        "fieldName='" + fieldName + '\'' +
                        ", fieldLocatorFactory=" + fieldLocatorFactory +
                        '}';
            }
        }
    }

    /**
     * A termination handler is responsible for a method's return.
     */
    protected enum TerminationHandler {

        /**
         * A termination handler that drops the forwarded method's return value.
         */
        DROPPING {
            @Override
            protected StackManipulation resolve(TypeDefinition returnType) {
                return Removal.pop(returnType);
            }
        },

        /**
         * A termination handler that returns the forwarded method's return value.
         */
        RETURNING {
            @Override
            protected StackManipulation resolve(TypeDefinition returnType) {
                return MethodReturn.of(returnType);
            }
        };

        /**
         * Resolves a stack manipulation for handling the forwarded method's return value.
         *
         * @param returnType The return type.
         * @return An appropriate stack manipulation.
         */
        protected abstract StackManipulation resolve(TypeDefinition returnType);

        @Override
        public String toString() {
            return "Forwarding.TerminationHandler." + name();
        }
    }

    /**
     * An appender for implementing a {@link net.bytebuddy.implementation.Forwarding} operation.
     */
    protected static class Appender implements ByteCodeAppender {

        /**
         * The field to forward to.
         */
        private final FieldDescription fieldDescription;

        /**
         * The termination handler to apply.
         */
        private final TerminationHandler terminationHandler;

        /**
         * Creates a new appender for a forwarding implementation.
         *
         * @param fieldDescription   The field to forward to.
         * @param terminationHandler The termination handler to apply.
         */
        protected Appender(FieldDescription fieldDescription, TerminationHandler terminationHandler) {
            this.fieldDescription = fieldDescription;
            this.terminationHandler = terminationHandler;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (instrumentedMethod.isStatic()) {
                throw new IllegalStateException("Cannot forward the static method " + instrumentedMethod);
            } else if (!instrumentedMethod.isInvokableOn(fieldDescription.getType().asErasure())) {
                throw new IllegalStateException("Cannot forward " + instrumentedMethod + " to " + fieldDescription.getType());
            }
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    fieldDescription.isStatic()
                            ? StackManipulation.Trivial.INSTANCE
                            : MethodVariableAccess.REFERENCE.loadOffset(0),
                    FieldAccess.forField(fieldDescription).getter(),
                    MethodVariableAccess.allArgumentsOf(instrumentedMethod),
                    MethodInvocation.invoke(instrumentedMethod).virtual(fieldDescription.getType().asErasure()),
                    terminationHandler.resolve(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, implementationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            Appender appender = (Appender) object;
            return fieldDescription.equals(appender.fieldDescription)
                    && terminationHandler == appender.terminationHandler;
        }

        @Override
        public int hashCode() {
            int result = fieldDescription.hashCode();
            result = 31 * result + terminationHandler.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Forwarding.Appender{" +
                    "fieldDescription=" + fieldDescription +
                    ", terminationHandler=" + terminationHandler +
                    '}';
        }
    }
}
