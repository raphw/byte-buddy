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
    public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
        if (source.isPrimitive() || target.isPrimitive()) {
            return source.equals(target)
                    ? StackManipulation.Trivial.INSTANCE
                    : StackManipulation.Illegal.INSTANCE;

        } else if (source.asErasure().isAssignableTo(target.asErasure())) {
            return StackManipulation.Trivial.INSTANCE;
        } else if (typing.isDynamic()) {
            return TypeCasting.to(target);
        } else {
            return StackManipulation.Illegal.INSTANCE;
        }
    }
}
