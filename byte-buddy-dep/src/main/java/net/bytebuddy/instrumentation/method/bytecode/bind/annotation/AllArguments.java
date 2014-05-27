package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayFactory;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameters that are annotated with this annotation will be assigned a collection (or an array) containing
 * all arguments of the source method. Currently, this annotation supports the following collection types:
 * <ul>
 * <li>Array</li>
 * </ul>
 * <p>&nbsp;</p>
 * If the parameters of the source method are not assignable to the collection's component type, the method with
 * the annotated parameter will not be considered as a possible binding target for the source method.
 *
 * @see net.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 * @see net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AllArguments {

    /**
     * A binder for handling the
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<AllArguments> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<AllArguments> getHandledType() {
            return AllArguments.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AllArguments annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
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
                    return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
                }
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(arrayFactory.withValues(stackManipulations));
        }
    }
}
