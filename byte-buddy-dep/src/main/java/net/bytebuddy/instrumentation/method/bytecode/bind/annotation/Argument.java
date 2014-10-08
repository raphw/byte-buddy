package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.*;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Parameters that are annotated with this annotation will be assigned the value of the parameter of the source method
 * with the given parameter. For example, if source method {@code foo(String, Integer)} is bound to target method
 * {@code bar(@Argument(1) Integer)}, the second parameter of {@code foo} will be bound to the first argument of
 * {@code bar}.
 * <p>&nbsp;</p>
 * If a source method has less parameters than specified by {@link Argument#value()}, the method carrying this parameter
 * annotation is excluded from the list of possible binding candidates to this particular source method. The same happens,
 * if the source method parameter at the specified index is not assignable to the annotated parameter.
 *
 * @see net.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 * @see net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Argument {

    /**
     * The index of the parameter of the source method that should be bound to this parameter.
     *
     * @return The required parameter index.
     */
    int value();

    /**
     * Determines if the argument binding is to be considered by a
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver}
     * for resolving ambiguous bindings of two methods. If
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument.BindingMechanic#UNIQUE},
     * of two bindable target methods such as for example {@code foo(String)} and {@code bar(Object)}, the {@code foo}
     * method would be considered as dominant over the {@code bar} method because of its more specific argument type. As
     * a side effect, only one parameter of any target method can be bound to a source method parameter with a given
     * index unless the {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument.BindingMechanic#ANONYMOUS}
     * option is used for any other binding.
     *
     * @return The binding type that should be applied to this parameter binding.
     * @see net.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver
     */
    BindingMechanic bindingMechanic() default BindingMechanic.UNIQUE;

    /**
     * Determines if a parameter binding should be considered for resolving ambiguous method bindings.
     *
     * @see Argument#bindingMechanic()
     * @see net.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver
     */
    static enum BindingMechanic {

        /**
         * The binding is unique, i.e. only one such binding must be present among all parameters of a method. As a
         * consequence, the binding can be latter identified by an
         * {@link net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}.
         */
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
                        new MostSpecificTypeResolver.ParameterIndexToken(sourceParameterIndex)
                );
            }
        },

        /**
         * The binding is anonymous, i.e. it can be present on several parameters of the same method.
         */
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
                                assigner.assign(sourceType, targetType, considerRuntimeType))
                );
            }
        };

        /**
         * Creates a binding that corresponds to this binding mechanic.
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
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Argument> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Argument> annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            Argument argument = annotation.load();
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

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * Creates a list of all parameter indices of a source method that are not explicitly referenced
         * by any {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument} annotation on
         * the target method.
         *
         * @param source The source method.
         * @param target The target method.
         * @return An iterator over all parameter indices of the source method that are not explicitly referenced
         * by the target method in increasing order.
         */
        private static Iterator<Integer> makeFreeIndexList(MethodDescription source, MethodDescription target) {
            LinkedHashSet<Integer> results = new LinkedHashSet<Integer>(source.getParameterTypes().size());
            for (int sourceIndex = 0; sourceIndex < source.getParameterTypes().size(); sourceIndex++) {
                results.add(sourceIndex);
            }
            for (AnnotationList parameterAnnotations : target.getParameterAnnotations()) {
                AnnotationDescription.Loadable<Argument> annotation = parameterAnnotations.ofType(Argument.class);
                if (annotation != null) {
                    results.remove(annotation.load().value());
                }
            }
            return results.iterator();
        }

        @Override
        public Iterator<AnnotationDescription> makeIterator(Instrumentation.Target instrumentationTarget,
                                                            MethodDescription source,
                                                            MethodDescription target) {
            return new NextUnboundArgumentIterator(makeFreeIndexList(source, target));
        }

        /**
         * An iterator that creates {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument}
         * annotations for any non-referenced index of the source method.
         */
        private static class NextUnboundArgumentIterator implements Iterator<AnnotationDescription> {

            /**
             * An iterator over all free indices.
             */
            private final Iterator<Integer> iterator;

            /**
             * Creates a new iterator for {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument}
             * annotations of non-referenced parameter indices of the source method.
             *
             * @param iterator An iterator of free indices of the source method.
             */
            private NextUnboundArgumentIterator(Iterator<Integer> iterator) {
                this.iterator = iterator;
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public AnnotationDescription next() {
                return AnnotationDescription.ForLoadedAnnotation.of(new DefaultArgument(iterator.next()));
            }

            @Override
            public void remove() {
                iterator.remove();
            }

            /**
             * A default implementation of an {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument}
             * annotation.
             */
            private static class DefaultArgument implements Argument {

                private static final String VALUE = "value";

                /**
                 * The index of the source method parameter to be bound.
                 */
                private final int parameterIndex;

                /**
                 * Creates a new instance of an argument annotation.
                 *
                 * @param parameterIndex The index of the source method parameter to be bound.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || other instanceof Argument && parameterIndex == ((Argument) other).value();
                }

                @Override
                public int hashCode() {
                    return (127 * VALUE.hashCode()) ^ parameterIndex; // OpenJDK implementation of the hash code method
                }

                @Override
                public String toString() {
                    return "Argument.NextUnboundAsDefaultsProvider.DefaultArgument{parameterIndex=" + parameterIndex + '}';
                }
            }
        }
    }
}
