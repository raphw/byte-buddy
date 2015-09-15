package net.bytebuddy.implementation.bytecode.assign.reference;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;

/**
 * A simple assigner that is capable of handling the casting of reference types. Primitives can only be assigned to
 * each other if they represent the same type.
 */
public enum ReferenceTypeAwareAssigner implements Assigner {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public StackManipulation assign(TypeDescription sourceType, TypeDescription targetType, Typing typing) {
        if (sourceType.isPrimitive() || targetType.isPrimitive()) {
            if (sourceType.equals(targetType)) {
                return StackManipulation.Trivial.INSTANCE;
            } else {
                return StackManipulation.Illegal.INSTANCE;
            }
        } else if (targetType.isAssignableFrom(sourceType)) {
            return StackManipulation.Trivial.INSTANCE;
        } else if (typing.isDynamic()) {
            return TypeCasting.to(targetType);
        } else {
            return StackManipulation.Illegal.INSTANCE;
        }
    }

    @Override
    public String toString() {
        return "ReferenceTypeAwareAssigner." + name();
    }
}
