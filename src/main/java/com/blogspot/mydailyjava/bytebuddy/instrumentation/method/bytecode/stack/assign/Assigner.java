package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

/**
 * An assigner is responsible for converting some type {@code A} to another type {@code B} if possible.
 * <p/>
 * An assigner is for example responsible for type casting, auto boxing or unboxing or for the widening of primitive
 * types.
 */
public interface Assigner {

    /**
     * @param sourceType          The original type that is to be transformed into the {@code targetType}.
     * @param targetType          The target type into which the {@code sourceType} is to be converted.
     * @param considerRuntimeType A hint whether the assignment should consider the runtime type of the source type,
     *                            i.e. if type down or cross castings are allowed.
     * @return A stack manipulation that transforms the {@code sourceType} into the {@code targetType} if this
     * is possible. An illegal stack manipulation otherwise.
     */
    StackManipulation assign(TypeDescription sourceType, TypeDescription targetType, boolean considerRuntimeType);
}
