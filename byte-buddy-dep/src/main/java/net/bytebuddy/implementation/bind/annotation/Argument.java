package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.ArgumentTypeResolver;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

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
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 * @see net.bytebuddy.implementation.bind.annotation.RuntimeType
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
     * {@link net.bytebuddy.implementation.bind.ArgumentTypeResolver}
     * for resolving ambiguous bindings of two methods. If
     * {@link net.bytebuddy.implementation.bind.annotation.Argument.BindingMechanic#UNIQUE},
     * of two bindable target methods such as for example {@code foo(String)} and {@code bar(Object)}, the {@code foo}
     * method would be considered as dominant over the {@code bar} method because of its more specific argument type. As
     * a side effect, only one parameter of any target method can be bound to a source method parameter with a given
     * index unless the {@link net.bytebuddy.implementation.bind.annotation.Argument.BindingMechanic#ANONYMOUS}
     * option is used for any other binding.
     *
     * @return The binding type that should be applied to this parameter binding.
     * @see net.bytebuddy.implementation.bind.ArgumentTypeResolver
     */
    BindingMechanic bindingMechanic() default BindingMechanic.UNIQUE;

    /**
     * Determines if a parameter binding should be considered for resolving ambiguous method bindings.
     *
     * @see Argument#bindingMechanic()
     * @see net.bytebuddy.implementation.bind.ArgumentTypeResolver
     */
    enum BindingMechanic {

        /**
         * The binding is unique, i.e. only one such binding must be present among all parameters of a method. As a
         * consequence, the binding can be latter identified by an
         * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}.
         */
        UNIQUE {
            @Override
            protected MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription sourceType,
                                                                             TypeDescription targetType,
                                                                             int sourceParameterIndex,
                                                                             Assigner assigner,
                                                                             Assigner.Typing typing,
                                                                             int parameterOffset) {
                return MethodDelegationBinder.ParameterBinding.Unique.of(
                        new StackManipulation.Compound(
                                MethodVariableAccess.forType(sourceType).loadOffset(parameterOffset),
                                assigner.assign(sourceType, targetType, typing)),
                        new ArgumentTypeResolver.ParameterIndexToken(sourceParameterIndex)
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
                                                                             Assigner.Typing typing,
                                                                             int parameterOffset) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(
                        new StackManipulation.Compound(
                                MethodVariableAccess.forType(sourceType).loadOffset(parameterOffset),
                                assigner.assign(sourceType, targetType, typing))
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
         * @param typing               Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param parameterOffset      The offset of the source method's parameter.
         * @return A binding considering the chosen binding mechanic.
         */
        protected abstract MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription sourceType,
                                                                                  TypeDescription targetType,
                                                                                  int sourceParameterIndex,
                                                                                  Assigner assigner,
                                                                                  Assigner.Typing typing,
                                                                                  int parameterOffset);

        @Override
        public String toString() {
            return "Argument.BindingMechanic." + name();
        }
    }

    /**
     * A binder for handling the
     * {@link net.bytebuddy.implementation.bind.annotation.Argument}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Argument> {

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
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            Argument argument = annotation.loadSilent();
            if (argument.value() < 0) {
                throw new IllegalArgumentException("@Argument annotation on " + target + " specifies negative index");
            } else if (source.getParameters().size() <= argument.value()) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return argument.bindingMechanic().makeBinding(source.getParameters().get(argument.value()).getType().asErasure(),
                    target.getType().asErasure(),
                    argument.value(),
                    assigner,
                    RuntimeType.Verifier.check(target),
                    source.getParameters().get(argument.value()).getOffset());
        }

        @Override
        public String toString() {
            return "Argument.Binder." + name();
        }
    }

    /**
     * If this defaults provider is active, a non-annotated parameter is assumed to be implicitly bound to the next
     * source method parameter that is not bound by any other target method parameter, i.e. a target method
     * {@code bar(Object, String)} would be equivalent to a {@code bar(@Argument(0) Object, @Argument(1) String)}.
     *
     * @see TargetMethodAnnotationDrivenBinder.DefaultsProvider
     */
    enum NextUnboundAsDefaultsProvider implements TargetMethodAnnotationDrivenBinder.DefaultsProvider {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * Creates a list of all parameter indices of a source method that are not explicitly referenced
         * by any {@link net.bytebuddy.implementation.bind.annotation.Argument} annotation on
         * the target method.
         *
         * @param source The source method.
         * @param target The target method.
         * @return An iterator over all parameter indices of the source method that are not explicitly referenced
         * by the target method in increasing order.
         */
        private static Iterator<Integer> makeFreeIndexList(MethodDescription source, MethodDescription target) {
            LinkedHashSet<Integer> results = new LinkedHashSet<Integer>(source.getParameters().size());
            for (int sourceIndex = 0; sourceIndex < source.getParameters().size(); sourceIndex++) {
                results.add(sourceIndex);
            }
            for (ParameterDescription parameterDescription : target.getParameters()) {
                AnnotationDescription.Loadable<Argument> annotation = parameterDescription.getDeclaredAnnotations().ofType(Argument.class);
                if (annotation != null) {
                    results.remove(annotation.loadSilent().value());
                }
            }
            return results.iterator();
        }

        @Override
        public Iterator<AnnotationDescription> makeIterator(Implementation.Target implementationTarget,
                                                            MethodDescription source,
                                                            MethodDescription target) {
            return new NextUnboundArgumentIterator(makeFreeIndexList(source, target));
        }

        @Override
        public String toString() {
            return "Argument.NextUnboundAsDefaultsProvider." + name();
        }

        /**
         * An iterator that creates {@link net.bytebuddy.implementation.bind.annotation.Argument}
         * annotations for any non-referenced index of the source method.
         */
        protected static class NextUnboundArgumentIterator implements Iterator<AnnotationDescription> {

            /**
             * An iterator over all free indices.
             */
            private final Iterator<Integer> iterator;

            /**
             * Creates a new iterator for {@link net.bytebuddy.implementation.bind.annotation.Argument}
             * annotations of non-referenced parameter indices of the source method.
             *
             * @param iterator An iterator of free indices of the source method.
             */
            protected NextUnboundArgumentIterator(Iterator<Integer> iterator) {
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

            @Override
            public String toString() {
                return "Argument.NextUnboundAsDefaultsProvider.NextUnboundArgumentIterator{" +
                        "iterator=" + iterator +
                        '}';
            }

            /**
             * A default implementation of an {@link net.bytebuddy.implementation.bind.annotation.Argument}
             * annotation.
             */
            protected static class DefaultArgument implements Argument {

                /**
                 * The name of the value annotation parameter.
                 */
                private static final String VALUE = "value";

                /**
                 * The name of the value binding mechanic parameter.
                 */
                private static final String BINDING_MECHANIC = "bindingMechanic";

                /**
                 * The index of the source method parameter to be bound.
                 */
                private final int parameterIndex;

                /**
                 * Creates a new instance of an argument annotation.
                 *
                 * @param parameterIndex The index of the source method parameter to be bound.
                 */
                protected DefaultArgument(int parameterIndex) {
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
                    return ((127 * BINDING_MECHANIC.hashCode()) ^ BindingMechanic.UNIQUE.hashCode())
                            + ((127 * VALUE.hashCode()) ^ parameterIndex);
                }

                @Override
                public String toString() {
                    return "@" + Argument.class.getName()
                            + "(bindingMechanic=" + BindingMechanic.UNIQUE.toString()
                            + ", value=" + parameterIndex + ")";
                }
            }
        }
    }
}
