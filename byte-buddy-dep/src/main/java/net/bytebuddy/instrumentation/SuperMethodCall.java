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
 * an exception will be thrown. Note that methods that were explicitly defined for an instrumentation are never
 * considered to have a super method even if there is a method with a compatible signature. Constructors are
 * considered to have a "super method" if the direct super type defines a constructor with identical signature.
 * If a method is found to not have a super method, e.g. when instrumenting a static method, an exception is thrown.
 * <p>&nbsp;</p>
 * Besides implementing constructors, this instrumentation is useful when a method of a super type is not supposed
 * to be altered but should be equipped with additional annotations.
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

    /**
     * A super method invocation where the return value is dropped instead of returning from the method.
     */
    protected static enum WithoutReturn implements Instrumentation {

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
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            StackManipulation superMethodCall = instrumentationTarget.invokeSuper(instrumentedMethod, Target.MethodLookup.Default.EXACT);
            if (!superMethodCall.isValid()) {
                throw new IllegalArgumentException("Cannot call super method of " + instrumentedMethod);
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
        protected static enum TerminationHandler {

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
        }
    }
}
