package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import org.objectweb.asm.MethodVisitor;

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
        return new Appender(instrumentationTarget);
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
         * Creates a new appender.
         *
         * @param instrumentationTarget The instrumentation target of the current type creation.
         */
        private Appender(Target instrumentationTarget) {
            this.instrumentationTarget = instrumentationTarget;
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
                    MethodReturn.returning(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && instrumentationTarget.equals(((Appender) other).instrumentationTarget);
        }

        @Override
        public int hashCode() {
            return instrumentationTarget.hashCode();
        }

        @Override
        public String toString() {
            return "SuperMethodCall.Appender{instrumentationTarget=" + instrumentationTarget + '}';
        }
    }
}
