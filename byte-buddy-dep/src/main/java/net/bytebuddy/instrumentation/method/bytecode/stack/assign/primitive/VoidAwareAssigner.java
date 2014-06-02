package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.DefaultValue;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This assigner is able to handle the {@code void} type. This means:
 * <ol>
 * <li>If a {@code void} type is assigned to the {@code void} it will consider this a trivial operation.</li>
 * <li>If a {@code void} type is assigned to a non-{@code void} type, it will pop the top value from the stack.</li>
 * <li>If a non-{@code void} type is assigned to a {@code void} type, it will load the target type's default value
 * only if this was configured at the assigner's construction.</li>
 * <li>If two non-{@code void} types are subject of the assignment, it will delegate the assignment to its chained
 * assigner.</li>
 * </ol>
 */
public class VoidAwareAssigner implements Assigner {

    /**
     * An assigner that is capable of handling assignments that do not involve {@code void} types.
     */
    private final Assigner nonVoidAwareAssigner;

    /**
     * If {@code true}, this assigner handles an assignment of a {@code void} type to a non-{@code void} type
     * as the creation of a default value. Note that this does not reassemble the behavior of the Java compiler.
     */
    private final boolean returnDefaultValue;

    /**
     * Creates a new assigner that is capable of handling void types.
     *
     * @param nonVoidAwareAssigner A chained assigner which will be queried by this assigner to handle assignments that
     *                             do not involve a {@code void} type.
     * @param returnDefaultValue   Determines if this assigner will load a target type's default value onto the stack if
     *                             a {@code void} type is assigned to a non-{@code void} type.
     */
    public VoidAwareAssigner(Assigner nonVoidAwareAssigner, boolean returnDefaultValue) {
        this.nonVoidAwareAssigner = nonVoidAwareAssigner;
        this.returnDefaultValue = returnDefaultValue;
    }

    @Override
    public StackManipulation assign(TypeDescription sourceType, TypeDescription targetType, boolean considerRuntimeType) {
        if (sourceType.represents(void.class) && targetType.represents(void.class)) {
            return StackManipulation.LegalTrivial.INSTANCE;
        } else if (sourceType.represents(void.class) /* && subType != void.class */) {
            return returnDefaultValue ? DefaultValue.of(targetType) : StackManipulation.Illegal.INSTANCE;
        } else if (/* superType != void.class && */ targetType.represents(void.class)) {
            return ValueRemovingStackManipulation.of(sourceType);
        } else /* superType != void.class && subType != void.class */ {
            return nonVoidAwareAssigner.assign(sourceType, targetType, considerRuntimeType);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && returnDefaultValue == ((VoidAwareAssigner) other).returnDefaultValue
                && nonVoidAwareAssigner.equals(((VoidAwareAssigner) other).nonVoidAwareAssigner);
    }

    @Override
    public int hashCode() {
        return 31 * nonVoidAwareAssigner.hashCode() + (returnDefaultValue ? 1 : 0);
    }

    @Override
    public String toString() {
        return "VoidAwareAssigner{chained=" + nonVoidAwareAssigner + ", returnDefaultValue=" + returnDefaultValue + '}';
    }

    /**
     * A stack manipulation that removes a value from the operand stack.
     */
    private static enum ValueRemovingStackManipulation implements StackManipulation {

        /**
         * Removes the top-most value from the operand stack.
         */
        POP_ONE_FRAME(Opcodes.POP, StackSize.SINGLE),

        /**
         * Removes the two top-most values from the operand stack.
         */
        POP_TWO_FRAMES(Opcodes.POP2, StackSize.DOUBLE);

        /**
         * The opcode for instructing the removal.
         */
        private final int removalOpcode;

        /**
         * The size reduction of the operand stack that is implied by this operation.
         */
        private final Size sizeChange;

        /**
         * Creates a new value removing stack manipulation.
         *
         * @param removalOpcode The opcode for instructing the removal.
         * @param sizeChange    The size reduction of the operand stack that is implied by this operation.
         */
        private ValueRemovingStackManipulation(int removalOpcode, StackSize sizeChange) {
            this.removalOpcode = removalOpcode;
            this.sizeChange = sizeChange.toDecreasingSize();
        }

        /**
         * Identifies a suitable stack manipulation for removing a given type from the operand stack.
         *
         * @param typeDescription The type to remove from the stack.
         * @return A stack manipulation to remove from the stack.
         */
        public static StackManipulation of(TypeDescription typeDescription) {
            if (typeDescription.represents(long.class) || typeDescription.represents(double.class)) {
                return POP_TWO_FRAMES;
            } else if (typeDescription.represents(void.class)) {
                throw new IllegalArgumentException("Cannot pop void type from stack");
            } else {
                return POP_ONE_FRAME;
            }
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitInsn(removalOpcode);
            return sizeChange;
        }
    }
}
