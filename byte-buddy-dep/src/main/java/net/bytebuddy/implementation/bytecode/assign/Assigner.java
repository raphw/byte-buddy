package net.bytebuddy.implementation.bytecode.assign;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.implementation.bytecode.assign.primitive.VoidAwareAssigner;
import net.bytebuddy.implementation.bytecode.assign.reference.ReferenceTypeAwareAssigner;

/**
 * An assigner is responsible for converting some type {@code A} to another type {@code B} if possible.
 * <p>&nbsp;</p>
 * An assigner is for example responsible for type casting, auto boxing or unboxing or for the widening of primitive
 * types.
 */
@SuppressFBWarnings(value = "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", justification = "Safe initialization is implied")
public interface Assigner {

    /**
     * A default assigner that can handle {@code void}, primitive types and references.
     */
    Assigner DEFAULT = new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE));

    /**
     * @param source The original type that is to be transformed into the {@code targetType}.
     * @param target The target type into which the {@code sourceType} is to be converted.
     * @param typing A hint whether the assignment should consider the runtime type of the source type,
     *               i.e. if type down or cross castings are allowed. If this hint is set, this is
     *               also an indication that {@code void} to non-{@code void} assignments are permitted.
     * @return A stack manipulation that transforms the {@code sourceType} into the {@code targetType} if this
     * is possible. An illegal stack manipulation otherwise.
     */
    StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing);

    /**
     * Indicates for a type assignment, if a type casting should be applied in case that two types are not statically assignable.
     * Also, a dynamic typing indicates that void values are assignable to other types by assigning the target type's default value.
     */
    enum Typing {

        /**
         * Requires static typing.
         */
        STATIC(false),

        /**
         * Allows dynamic typing.
         */
        DYNAMIC(true);

        /**
         * {@code true} if dynamic typing is a legitimate choice.
         */
        private final boolean dynamic;

        /**
         * Creates a new typing hint.
         *
         * @param dynamic {@code true} if dynamic typing is a legitimate choice.
         */
        Typing(boolean dynamic) {
            this.dynamic = dynamic;
        }

        /**
         * Resolves a typing constant for the presented boolean where {@code true} indicates that dynamic typing is a legitimate choice.
         *
         * @param dynamic An indicator for if dynamic typing is a legitimate choice.
         * @return A corresponding typing constant.
         */
        public static Typing of(boolean dynamic) {
            return dynamic
                    ? DYNAMIC
                    : STATIC;
        }

        /**
         * Checks if this instance's typing behavior permits dynamic typing.
         *
         * @return {@code true} if dynamic typing is a legitimate choice.
         */
        public boolean isDynamic() {
            return dynamic;
        }
    }

    /**
     * An assigner that only allows to assign types if they are equal to another.
     */
    enum EqualTypesOnly implements Assigner {

        /**
         * An type assigner that only considers equal generic types to be assignable.
         */
        GENERIC {
            /** {@inheritDoc} */
            public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
                return source.equals(target)
                        ? StackManipulation.Trivial.INSTANCE
                        : StackManipulation.Illegal.INSTANCE;
            }
        },

        /**
         * A type assigner that considers two generic types to be equal if their erasure is equal.
         */
        ERASURE {
            /** {@inheritDoc} */
            public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
                return source.asErasure().equals(target.asErasure())
                        ? StackManipulation.Trivial.INSTANCE
                        : StackManipulation.Illegal.INSTANCE;
            }
        };
    }

    /**
     * An assigner that does not allow any assignments.
     */
    enum Refusing implements Assigner {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
            return StackManipulation.Illegal.INSTANCE;
        }
    }
}
