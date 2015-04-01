package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Removal;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import org.objectweb.asm.MethodVisitor;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * This instrumentation will create a new method which simply calls its super method. If no such method is defined,
 * an exception will be thrown. Constructors are considered to have a super method if the direct super class defines
 * a constructor with an identical signature. Default methods are invoked as such if they are non-ambiguous. Static
 * methods can have a (pseudo) super method if a type that defines such a method is rebased. Rebased types can also
 * shadow constructors or methods of an actual super class. Besides implementing constructors, this instrumentation
 * is useful when a method of a super type is not supposed to be altered but should be equipped with additional
 * annotations. Furthermore, this instrumentation allows to hard code a super method call to be performed after
 * performing another {@link net.bytebuddy.instrumentation.Instrumentation}.
 */
public enum SuperMethodCall implements Instrumentation {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public ByteCodeAppender appender(Target instrumentationTarget) {
        return new Appender(instrumentationTarget, Appender.TerminationHandler.RETURNING);
    }

    /**
     * Appends another instrumentation to a super method call.
     *
     * @param instrumentation The instrumentation to append.
     * @return An instrumentation that first invokes the instrumented method's super method and then applies
     * the given instrumentation.
     */
    public Instrumentation andThen(Instrumentation instrumentation) {
        return new Compound(WithoutReturn.INSTANCE, nonNull(instrumentation));
    }

    @Override
    public String toString() {
        return "SuperMethodCall." + name();
    }

    /**
     * A super method invocation where the return value is dropped instead of returning from the method.
     */
    protected enum WithoutReturn implements Instrumentation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return new Appender(instrumentationTarget, Appender.TerminationHandler.DROPPING);
        }

        @Override
        public String toString() {
            return "SuperMethodCall.WithoutReturn." + name();
        }
    }

    /**
     * An appender for implementing a {@link net.bytebuddy.instrumentation.SuperMethodCall}.
     */
    protected static class Appender implements ByteCodeAppender {

        /**
         * The target of the current instrumentation.
         */
        private final Target instrumentationTarget;

        /**
         * The termination handler to apply after invoking the super method.
         */
        private final TerminationHandler terminationHandler;

        /**
         * Creates a new appender.
         *
         * @param instrumentationTarget The instrumentation target of the current type creation.
         * @param terminationHandler    The termination handler to apply after invoking the super method.
         */
        protected Appender(Target instrumentationTarget, TerminationHandler terminationHandler) {
            this.instrumentationTarget = instrumentationTarget;
            this.terminationHandler = terminationHandler;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            StackManipulation superMethodCall = instrumentedMethod.isDefaultMethod()
                    && instrumentationTarget.getTypeDescription().getInterfaces().contains(instrumentedMethod.getDeclaringType())
                    ? instrumentationTarget.invokeDefault(instrumentedMethod.getDeclaringType(), instrumentedMethod.getUniqueSignature())
                    : instrumentationTarget.invokeSuper(instrumentedMethod, Target.MethodLookup.Default.EXACT);
            if (!superMethodCall.isValid()) {
                throw new IllegalStateException("Cannot call super (or default) method of " + instrumentedMethod);
            }
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                    superMethodCall,
                    terminationHandler.of(instrumentedMethod)
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && instrumentationTarget.equals(((Appender) other).instrumentationTarget)
                    && terminationHandler.equals(((Appender) other).terminationHandler);
        }

        @Override
        public int hashCode() {
            return instrumentationTarget.hashCode() + 31 * terminationHandler.hashCode();
        }

        @Override
        public String toString() {
            return "SuperMethodCall.Appender{" +
                    "instrumentationTarget=" + instrumentationTarget +
                    ", terminationHandler=" + terminationHandler +
                    '}';
        }

        /**
         * A handler that determines how to handle the method return value.
         */
        protected enum TerminationHandler {

            /**
             * A termination handler that returns the value of the super method invocation.
             */
            RETURNING {
                @Override
                protected StackManipulation of(MethodDescription methodDescription) {
                    return MethodReturn.returning(methodDescription.getReturnType());
                }
            },

            /**
             * A termination handler that simply pops the value of the super method invocation off the stack.
             */
            DROPPING {
                @Override
                protected StackManipulation of(MethodDescription methodDescription) {
                    return Removal.pop(methodDescription.getReturnType());
                }
            };

            /**
             * Creates a stack manipulation that represents this handler's behavior.
             *
             * @param methodDescription The method for which this handler is supposed to create a stack
             *                          manipulation for.
             * @return The stack manipulation that implements this handler.
             */
            protected abstract StackManipulation of(MethodDescription methodDescription);

            @Override
            public String toString() {
                return "SuperMethodCall.Appender.TerminationHandler." + name();
            }
        }
    }
}
