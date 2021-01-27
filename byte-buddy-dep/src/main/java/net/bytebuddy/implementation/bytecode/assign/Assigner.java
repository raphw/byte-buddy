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
     * A default assigner that can handle {@code void}, primitive types and references. 可以处理{@code void}、基元类型和引用的默认赋值器
     */
    Assigner DEFAULT = new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE));

    /**
     * @param source The original type that is to be transformed into the {@code targetType}.
     * @param target The target type into which the {@code sourceType} is to be converted.
     * @param typing     A hint whether the assignment should consider the runtime type of the source type,
     *                   i.e. if type down or cross castings are allowed. If this hint is set, this is
     *                   also an indication that {@code void} to non-{@code void} assignments are permitted.
     * @return A stack manipulation that transforms the {@code sourceType} into the {@code targetType} if this
     * is possible. An illegal stack manipulation otherwise.
     */
    StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing);

    /** 指示类型的转化是静态的还是动态的。静态的意思是，调用前已经确定了。动态类型时，生成是才确定。默认值是动态生成
     * Indicates for a type assignment, if a type casting should be applied in case that two types are not statically assignable.
     * Also, a dynamic typing indicates that void values are assignable to other types by assigning the target type's default value. 指示对于类型分配，在两个类型不可静态分配的情况下是否应应用类型转换。另外，动态类型表示通过指定目标类型的默认值，可以将void值指定给其他类型
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
            @Override
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
            @Override
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

        @Override
        public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
            return StackManipulation.Illegal.INSTANCE;
        }
    }
}
