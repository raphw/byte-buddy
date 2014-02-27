package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayFactory;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameters that are annotated with this annotation will be assigned a collection (or an array) containing
 * all arguments of the source method. Currently, this annotation supports the following collection types:
 * <ul>
 *     <li>Array</li>
 * </ul>
 *
 * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface AllArguments {

    /**
     * A binder for handling the
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ArgumentBinder<AllArguments> {
        INSTANCE;

        @Override
        public Class<AllArguments> getHandledType() {
            return AllArguments.class;
        }

        @Override
        public ParameterBinding<?> bind(AllArguments annotation,
                                         int targetParameterIndex,
                                         MethodDescription source,
                                         MethodDescription target,
                                         TypeDescription instrumentedType,
                                         Assigner assigner) {
            TypeDescription targetType = target.getParameterTypes().get(targetParameterIndex);
            if (!targetType.isArray()) {
                throw new IllegalStateException("Expected an array type for " + targetType);
            }
            ArrayFactory arrayFactory = ArrayFactory.targeting(targetType.getComponentType());
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(source.getParameterTypes().size());
            int offset = source.isStatic() ? 0 : 1;
            boolean considerRuntimeType = RuntimeType.Verifier.check(target, targetParameterIndex);
            for (TypeDescription sourceParameter : source.getParameterTypes()) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        MethodVariableAccess.forType(sourceParameter).loadFromIndex(offset),
                        assigner.assign(sourceParameter, arrayFactory.getComponentType(), considerRuntimeType));
                if (stackManipulation.isValid()) {
                    offset += sourceParameter.getStackSize().getSize();
                    stackManipulations.add(stackManipulation);
                } else {
                    return ParameterBinding.makeIllegal();
                }
            }
            return ParameterBinding.makeAnonymous(arrayFactory.withValues(stackManipulations));
        }
    }
}
