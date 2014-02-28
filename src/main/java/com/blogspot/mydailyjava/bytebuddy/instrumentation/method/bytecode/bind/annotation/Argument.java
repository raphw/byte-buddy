package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.*;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Parameters that are annotated with this annotation will be assigned the value of the parameter of the source method
 * with the given parameter. For example, if source method {@code foo(String, Integer)} is bound to target method
 * {@code bar(@Argument(1) Integer)}, the second parameter of {@code foo} will be bound to the first argument of
 * {@code bar}.
 * <p/>
 * If a source method has less parameters than specified by {@link Argument#value()}, the method carrying this parameter
 * annotation is excluded from the list of possible binding candidates to this particular source method. The same happens,
 * if the source method parameter at the specified index is not assignable to the annotated parameter.
 *
 * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Argument {

    /**
     * Determines if a parameter binding should be considered for resolving ambiguous method bindings.
     *
     * @see Argument#bindingMechanic()
     * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver
     */
    static enum BindingMechanic {

        UNIQUE {
            @Override
            protected MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription sourceType,
                                                                             TypeDescription targetType,
                                                                             int sourceParameterIndex,
                                                                             Assigner assigner,
                                                                             boolean considerRuntimeType,
                                                                             int parameterOffset) {
                return MethodDelegationBinder.ParameterBinding.Unique.of(
                        new StackManipulation.Compound(
                                MethodVariableAccess.forType(sourceType).loadFromIndex(parameterOffset),
                                assigner.assign(sourceType, targetType, considerRuntimeType)),
                        new MostSpecificTypeResolver.ParameterIndexToken(sourceParameterIndex));
            }
        },
        ANONYMOUS {
            @Override
            protected MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription sourceType,
                                                                             TypeDescription targetType,
                                                                             int sourceParameterIndex,
                                                                             Assigner assigner,
                                                                             boolean considerRuntimeType,
                                                                             int parameterOffset) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(
                        new StackManipulation.Compound(
                                MethodVariableAccess.forType(sourceType).loadFromIndex(parameterOffset),
                                assigner.assign(sourceType, targetType, considerRuntimeType)));
            }
        };

        /**
         * Creates the binding that is requrest
         *
         * @param sourceType           The source type to be bound.
         * @param targetType           The target type the {@code sourceType} is to be bound to.
         * @param sourceParameterIndex The index of the source parameter.
         * @param assigner             The assigner that is used to perform the assignment.
         * @param considerRuntimeType  If {@code true}, the assignment is allowed to consider runtime types.
         * @param parameterOffset      The offset of the source method's parameter.
         * @return A binding considering the chosen binding mechanic.
         */
        protected abstract MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription sourceType,
                                                                                  TypeDescription targetType,
                                                                                  int sourceParameterIndex,
                                                                                  Assigner assigner,
                                                                                  boolean considerRuntimeType,
                                                                                  int parameterOffset);
    }

    /**
     * A binder for handling the
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Argument> {
        INSTANCE;

        @Override
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(Argument argument,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               TypeDescription instrumentedType,
                                                               Assigner assigner) {
            if (argument.value() < 0) {
                throw new IllegalArgumentException(String.format("Argument annotation on %d's argument virtual " +
                        "%s holds negative index", targetParameterIndex, target));
            } else if (source.getParameterTypes().size() <= argument.value()) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return argument.bindingMechanic().makeBinding(source.getParameterTypes().get(argument.value()),
                    target.getParameterTypes().get(targetParameterIndex),
                    argument.value(),
                    assigner,
                    RuntimeType.Verifier.check(target, targetParameterIndex),
                    source.getParameterOffset(argument.value()));
        }
    }

    /**
     * If this defaults provider is active, a non-annotated parameter is assumed to be implicitly bound to the next
     * source method parameter that is not bound by any other target method parameter, i.e. a target method
     * {@code bar(Object, String)} would be equivalent to a {@code bar(@Argument(0) Object, @Argument(1) String)}.
     *
     * @see TargetMethodAnnotationDrivenBinder.DefaultsProvider
     */
    static enum NextUnboundAsDefaultsProvider implements TargetMethodAnnotationDrivenBinder.DefaultsProvider {
        INSTANCE;

        private static class DefaultArgument implements Argument {

            private final int parameterIndex;

            private DefaultArgument(int parameterIndex) {
                this.parameterIndex = parameterIndex;
            }

            @Override
            public int value() {
                return parameterIndex;
            }

            @Override
            public BindingMechanic bindingMechanic() {
                return BindingMechanic.UNIQUE;
            }

            @Override
            public Class<Argument> annotationType() {
                return Argument.class;
            }
        }

        private static class NextUnboundArgumentIterator implements Iterator<Argument> {

            private final Iterator<Integer> iterator;

            private NextUnboundArgumentIterator(Iterator<Integer> iterator) {
                this.iterator = iterator;
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Argument next() {
                return new DefaultArgument(iterator.next());
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        }

        @Override
        public Iterator<Argument> makeIterator(TypeDescription typeDescription, MethodDescription source, MethodDescription target) {
            return new NextUnboundArgumentIterator(makeFreeIndexList(source, target));
        }

        private static Iterator<Integer> makeFreeIndexList(MethodDescription source, MethodDescription target) {
            LinkedHashSet<Integer> results = new LinkedHashSet<Integer>(source.getParameterTypes().size());
            for (int sourceIndex = 0; sourceIndex < source.getParameterTypes().size(); sourceIndex++) {
                results.add(sourceIndex);
            }
            for (Annotation[] parameterAnnotation : target.getParameterAnnotations()) {
                for (Annotation aParameterAnnotation : parameterAnnotation) {
                    if (aParameterAnnotation.annotationType() == Argument.class) {
                        results.remove(((Argument) aParameterAnnotation).value());
                        break;
                    }
                }
            }
            return results.iterator();
        }
    }

    /**
     * The index of the parameter of the source method that should be bound to this parameter.
     *
     * @return The required parameter index.
     */
    int value();

    /**
     * Determines if the argument binding is to be considered by a
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver}
     * for resolving ambiguous bindings of two methods. If
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument.BindingMechanic#UNIQUE},
     * of two bindable target methods such as for example {@code foo(String)} and {@code bar(Object)}, the {@code foo}
     * method would be considered as dominant over the {@code bar} method because of its more specific argument type. As
     * a side effect, only one parameter of any target method can be bound to a source method parameter with a given
     * index unless the {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument.BindingMechanic#ANONYMOUS}
     * option is used for any other binding.
     *
     * @return The binding type that should be applied to this parameter binding.
     * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver
     */
    BindingMechanic bindingMechanic() default BindingMechanic.UNIQUE;
}
