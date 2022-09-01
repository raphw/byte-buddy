package net.bytebuddy.implementation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;

/**
 * Implements a method that returns the current instance. This is only possible for non-static methods.
 */
public enum SelfReturn implements Implementation {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * {@inheritDoc}
     */
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    /**
     * {@inheritDoc}
     */
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(implementationTarget.getInstrumentedType());
    }

    /**
     * An appender to return the instance of the type being instrumented.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class Appender implements ByteCodeAppender {

        /**
         * The instrumented type.
         */
        private final TypeDescription instrumentedType;

        /**
         * Creates a new appender.
         *
         * @param instrumentedType The instrumented type.
         */
        protected Appender(TypeDescription instrumentedType) {
            this.instrumentedType = instrumentedType;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (instrumentedMethod.isStatic() || instrumentedMethod.isConstructor()) {
                throw new IllegalStateException("Cannot return self from static method or constructor" + instrumentedMethod);
            } else if (!instrumentedType.isAssignableTo(instrumentedMethod.getReturnType().asErasure())) {
                throw new IllegalStateException("Cannot assign " + instrumentedType + " to " + instrumentedMethod);
            }
            return new Size(new StackManipulation.Compound(MethodVariableAccess.loadThis(), MethodReturn.REFERENCE)
                    .apply(methodVisitor, implementationContext)
                    .getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }
}
