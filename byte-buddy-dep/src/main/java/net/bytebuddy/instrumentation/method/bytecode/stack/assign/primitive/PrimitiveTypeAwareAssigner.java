package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;

/**
 * This assigner is able to handle non-{@code void}, primitive types. This means:
 * <ol>
 * <li>If a primitive type is assigned to a non-primitive type, it will attempt to widen the source type into the
 * target type.</li>
 * <li>If a primitive type is assigned to a non-primitive type, it will attempt to box the source type and then
 * query the chained assigner for assigning the boxed type to the target type.</li>
 * <li>If a non-primitive type is assigned to a primitive type, it will unbox the source type and then attempt a
 * widening of the unboxed type into the target type. If the source type does not represent a wrapper type, it will
 * attempt to infer the boxing type from the target type and check if the source type is assignable to this wrapper
 * type.</li>
 * <li>If two non-primitive types are subject of the assignment, it will delegate the assignment to its chained
 * assigner.</li>
 * </ol>
 */
public class PrimitiveTypeAwareAssigner implements Assigner {

    /**
     * Another assigner that is aware of assigning reference types. This assigner is queried for assigning
     * non-primitive types or for assigning a boxed type to another non-primitive type.
     */
    private final Assigner referenceTypeAwareAssigner;

    /**
     * Creates a new assigner with the given delegate.
     *
     * @param referenceTypeAwareAssigner A chained assigner that is queried for assignments not involving primitive
     *                                   types.
     */
    public PrimitiveTypeAwareAssigner(Assigner referenceTypeAwareAssigner) {
        this.referenceTypeAwareAssigner = referenceTypeAwareAssigner;
    }

    @Override
    public StackManipulation assign(TypeDescription sourceType, TypeDescription targetType, boolean dynamicallyTyped) {
        if (sourceType.isPrimitive() && targetType.isPrimitive()) {
            return PrimitiveWideningDelegate.forPrimitive(sourceType).widenTo(targetType);
        } else if (sourceType.isPrimitive() /* && !subType.isPrimitive() */) {
            return PrimitiveBoxingDelegate.forPrimitive(sourceType).assignBoxedTo(targetType, referenceTypeAwareAssigner, dynamicallyTyped);
        } else if (/* !superType.isPrimitive() && */ targetType.isPrimitive()) {
            return PrimitiveUnboxingDelegate.forReferenceType(sourceType).assignUnboxedTo(targetType, referenceTypeAwareAssigner, dynamicallyTyped);
        } else /* !superType.isPrimitive() && !subType.isPrimitive()) */ {
            return referenceTypeAwareAssigner.assign(sourceType, targetType, dynamicallyTyped);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && referenceTypeAwareAssigner.equals(((PrimitiveTypeAwareAssigner) other).referenceTypeAwareAssigner);
    }

    @Override
    public int hashCode() {
        return referenceTypeAwareAssigner.hashCode();
    }

    @Override
    public String toString() {
        return "PrimitiveTypeAwareAssigner{referenceTypeAwareAssigner=" + referenceTypeAwareAssigner + '}';
    }
}
