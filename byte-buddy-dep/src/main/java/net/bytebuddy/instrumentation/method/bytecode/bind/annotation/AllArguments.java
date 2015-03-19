package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterDescription;
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
 * By default, this annotation applies a
 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments.Assignment#STRICT}
 * assignment of the source method's parameters to the array. This implies that parameters that are not assignable to
 * the annotated array's component type make the method with this parameter unbindable. To avoid this, you can
 * use a {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments.Assignment#SLACK} assignment
 * which simply skips non-assignable values instead.
 *
 * @see net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments.Assignment
 * @see net.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 * @see net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AllArguments {

    /**
     * Defines the type of {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments.Assignment}
     * type that is applied for filling the annotated array with values.
     *
     * @return The assignment handling to be applied for the annotated parameter.
     */
    Assignment value() default Assignment.STRICT;

    /**
     * A directive for how an {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments}
     * annotation on an array is to be interpreted.
     */
    public static enum Assignment {

        /**
         * A strict assignment attempts to include <b>all</b> parameter values of the source method. If only one of these
         * parameters is not assignable to the component type of the annotated array, the method is considered as
         * non-bindable.
         */
        STRICT(true),

        /**
         * Other than a {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments.Assignment#STRICT}
         * assignment, a slack assignment simply ignores non-bindable parameters and does not include them in the target
         * array. In the most extreme case where no source method parameter is assignable to the component type
         * of the annotated array, the array that is assigned to the target parameter is empty.
         */
        SLACK(false);

        /**
         * Determines if this assignment is strict.
         */
        private final boolean strict;

        /**
         * Creates a new assignment type.
         *
         * @param strict {@code true} if this assignment is strict.
         */
        private Assignment(boolean strict) {
            this.strict = strict;
        }

        /**
         * Returns {@code true} if this assignment is strict.
         *
         * @return {@code true} if this assignment is strict.
         */
        protected boolean isStrict() {
            return strict;
        }
    }

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
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<AllArguments> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            if (!target.getTypeDescription().isArray()) {
                throw new IllegalStateException("Expected an array type for all argument annotation on " + source);
            }
            ArrayFactory arrayFactory = ArrayFactory.targeting(target.getTypeDescription().getComponentType());
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(source.getParameters().size());
            int offset = source.isStatic() ? 0 : 1;
            boolean dynamicallyTyped = RuntimeType.Verifier.check(target);
            for (TypeDescription sourceParameter : source.getParameters().asTypeList()) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        MethodVariableAccess.forType(sourceParameter).loadOffset(offset),
                        assigner.assign(sourceParameter, arrayFactory.getComponentType(), dynamicallyTyped));
                if (stackManipulation.isValid()) {
                    stackManipulations.add(stackManipulation);
                } else if (annotation.loadSilent().value().isStrict()) {
                    return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
                }
                offset += sourceParameter.getStackSize().getSize();
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(arrayFactory.withValues(stackManipulations));
        }
    }
}
