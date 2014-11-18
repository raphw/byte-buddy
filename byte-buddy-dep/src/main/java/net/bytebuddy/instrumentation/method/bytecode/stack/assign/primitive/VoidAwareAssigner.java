package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.method.bytecode.stack.Removal;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.DefaultValue;
import net.bytebuddy.instrumentation.type.TypeDescription;

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
    private final Assigner chained;

    /**
     * If {@code true}, this assigner handles an assignment of a {@code void} type to a non-{@code void} type
     * as the creation of a default value. Note that this does not reassemble the behavior of the Java compiler.
     */
    private final boolean returnDefaultValue;

    /**
     * Creates a new assigner that is capable of handling void types.
     *
     * @param chained            A chained assigner which will be queried by this assigner to handle assignments that
     *                           do not involve a {@code void} type.
     * @param returnDefaultValue Determines if this assigner will load a target type's default value onto the stack if
     *                           a {@code void} type is assigned to a non-{@code void} type.
     */
    public VoidAwareAssigner(Assigner chained, boolean returnDefaultValue) {
        this.chained = chained;
        this.returnDefaultValue = returnDefaultValue;
    }

    @Override
    public StackManipulation assign(TypeDescription sourceType, TypeDescription targetType, boolean considerRuntimeType) {
        if (sourceType.represents(void.class) && targetType.represents(void.class)) {
            return StackManipulation.LegalTrivial.INSTANCE;
        } else if (sourceType.represents(void.class) /* && subType != void.class */) {
            return returnDefaultValue ? DefaultValue.of(targetType) : StackManipulation.Illegal.INSTANCE;
        } else if (/* superType != void.class && */ targetType.represents(void.class)) {
            return Removal.pop(sourceType);
        } else /* superType != void.class && subType != void.class */ {
            return chained.assign(sourceType, targetType, considerRuntimeType);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && returnDefaultValue == ((VoidAwareAssigner) other).returnDefaultValue
                && chained.equals(((VoidAwareAssigner) other).chained);
    }

    @Override
    public int hashCode() {
        return 31 * chained.hashCode() + (returnDefaultValue ? 1 : 0);
    }

    @Override
    public String toString() {
        return "VoidAwareAssigner{chained=" + chained + ", returnDefaultValue=" + returnDefaultValue + '}';
    }
}
