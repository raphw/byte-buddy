package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * This {@link net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder} binds
 * method by analyzing annotations found on the <i>target</i> method that is subject to a method binding.
 */
public class TargetMethodAnnotationDrivenBinder implements MethodDelegationBinder {

    /**
     * The processor for performing an actual method delegation.
     */
    private final DelegationProcessor delegationProcessor;

    /**
     * The provider for annotations to be supplied for binding of non-annotated parameters.
     */
    private final DefaultsProvider<?> defaultsProvider;

    /**
     * An user-supplied assigner to use for variable assignments.
     */
    private final Assigner assigner;

    /**
     * A delegate for actually invoking a method.
     */
    private final MethodInvoker methodInvoker;

    /**
     * Creates a new method delegation binder that binds method based on annotations found on the target method.
     *
     * @param parameterBinders A list of parameter binder delegates. Each such delegate is responsible for creating a
     *                         {@link net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.ParameterBinding}
     *                         for a specific annotation.
     * @param defaultsProvider A provider that creates an annotation for parameters that are not annotated by any annotation
     *                         that is handled by any of the registered {@code parameterBinders}.
     * @param assigner         An assigner that is supplied to the {@code parameterBinders} and that is used for binding the return value.
     * @param methodInvoker    A delegate for applying the actual method invocation of the target method.
     */
    public TargetMethodAnnotationDrivenBinder(List<ParameterBinder<?>> parameterBinders,
                                              DefaultsProvider<?> defaultsProvider,
                                              Assigner assigner,
                                              MethodInvoker methodInvoker) {
        this.delegationProcessor = new DelegationProcessor(parameterBinders);
        this.defaultsProvider = defaultsProvider;
        this.assigner = assigner;
        this.methodInvoker = methodInvoker;
    }

    @Override
    public MethodBinding bind(Instrumentation.Target instrumentationTarget,
                              MethodDescription source,
                              MethodDescription target) {
        if (IgnoreForBinding.Verifier.check(target)) {
            return MethodBinding.Illegal.INSTANCE;
        }
        StackManipulation returningStackManipulation = assigner.assign(
                target.isConstructor() ? target.getDeclaringType() : target.getReturnType(),
                source.getReturnType(),
                RuntimeType.Verifier.check(target));
        if (!returningStackManipulation.isValid()) {
            return MethodBinding.Illegal.INSTANCE;
        }
        MethodBinding.Builder methodDelegationBindingBuilder = new MethodBinding.Builder(methodInvoker, target);
        Iterator<? extends Annotation> defaults = defaultsProvider.makeIterator(instrumentationTarget, source, target);
        for (int targetParameterIndex = 0;
             targetParameterIndex < target.getParameterTypes().size();
             targetParameterIndex++) {
            ParameterBinding<?> parameterBinding = delegationProcessor
                    .handler(target.getParameterAnnotations()[targetParameterIndex], defaults)
                    .bind(targetParameterIndex,
                            source,
                            target,
                            instrumentationTarget,
                            assigner);
            if (!parameterBinding.isValid() || !methodDelegationBindingBuilder.append(parameterBinding)) {
                return MethodBinding.Illegal.INSTANCE;
            }
        }
        return methodDelegationBindingBuilder.build(returningStackManipulation);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        TargetMethodAnnotationDrivenBinder that = (TargetMethodAnnotationDrivenBinder) other;
        return assigner.equals(that.assigner)
                && defaultsProvider.equals(that.defaultsProvider)
                && delegationProcessor.equals(that.delegationProcessor)
                && methodInvoker.equals(that.methodInvoker);
    }

    @Override
    public int hashCode() {
        int result = delegationProcessor.hashCode();
        result = 31 * result + defaultsProvider.hashCode();
        result = 31 * result + assigner.hashCode();
        result = 31 * result + methodInvoker.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TargetMethodAnnotationDrivenBinder{" +
                "delegationProcessor=" + delegationProcessor +
                ", defaultsProvider=" + defaultsProvider +
                ", assigner=" + assigner +
                ", methodInvoker=" + methodInvoker +
                '}';
    }

    /**
     * A parameter binder is used as a delegate for binding a parameter according to a particular annotation type found
     * on this parameter.
     *
     * @param <T> The {@link java.lang.annotation.Annotation#annotationType()} handled by this parameter binder.
     */
    public static interface ParameterBinder<T extends Annotation> {

        /**
         * The annotation type that is handled by this parameter binder.
         *
         * @return The {@link java.lang.annotation.Annotation#annotationType()} handled by this parameter binder.
         */
        Class<T> getHandledType();

        /**
         * Creates a parameter binding for the given target parameter.
         *
         * @param annotation            The annotation that was cause for the delegation to this argument binder.
         * @param targetParameterIndex  The index of the target method's parameter to be bound.
         * @param source                The source method that is bound to the {@code target} method.
         * @param target                Tge target method that is subject to be bound by the {@code source} method.
         * @param instrumentationTarget The target of the current instrumentation that is subject to this binding.
         * @param assigner              An assigner that can be used for applying the binding.
         * @return A parameter binding for the requested target method parameter.
         */
        ParameterBinding<?> bind(T annotation,
                                 int targetParameterIndex,
                                 MethodDescription source,
                                 MethodDescription target,
                                 Instrumentation.Target instrumentationTarget,
                                 Assigner assigner);
    }

    /**
     * Implementations of the defaults provider interface create annotations for parameters that are not annotated with
     * a known annotation.
     *
     * @param <T> The annotation type that is emitted by the defaults provider.
     * @see net.bytebuddy.instrumentation.method.bytecode.bind.annotation.TargetMethodAnnotationDrivenBinder
     */
    public static interface DefaultsProvider<T extends Annotation> {

        /**
         * Creates an iterator from which a value is pulled each time no processable annotation is found on a
         * method parameter.
         *
         * @param instrumentationTarget The target of the current instrumentation.
         * @param source                The source method that is bound to the {@code target} method.
         * @param target                Tge target method that is subject to be bound by the {@code source} method.
         * @return An iterator that supplies default annotations for
         */
        Iterator<T> makeIterator(Instrumentation.Target instrumentationTarget,
                                 MethodDescription source,
                                 MethodDescription target);

        /**
         * A defaults provider that does not supply any defaults. If this defaults provider is used, a target
         * method is required to annotate each parameter with a known annotation.
         */
        static enum Empty implements DefaultsProvider<Annotation> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Iterator<Annotation> makeIterator(Instrumentation.Target instrumentationTarget,
                                                     MethodDescription source,
                                                     MethodDescription target) {
                return EmptyIterator.INSTANCE;
            }

            /**
             * A trivial iterator without any elements.
             */
            private static enum EmptyIterator implements Iterator<Annotation> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Annotation next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new NoSuchElementException();
                }
            }
        }
    }

    /**
     * A delegation processor is a helper class for a
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.TargetMethodAnnotationDrivenBinder}
     * for performing its actual logic. By outsourcing this logic to this helper class, a cleaner implementation
     * can be provided.
     */
    private static class DelegationProcessor {

        /**
         * A map of registered annotation types to the binder that is responsible for binding a parameter
         * that is annotated with the given annotation.
         */
        private final Map<Class<? extends Annotation>, ParameterBinder<?>> argumentBinders;

        /**
         * Creates a new delegation processor.
         *
         * @param parameterBinders A list of parameter binder delegates. Each such delegate is responsible for creating
         *                         a {@link net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.ParameterBinding}
         *                         for a specific annotation.
         */
        private DelegationProcessor(List<ParameterBinder<?>> parameterBinders) {
            Map<Class<? extends Annotation>, ParameterBinder<?>> argumentBinderMap = new HashMap<Class<? extends Annotation>, ParameterBinder<?>>();
            for (ParameterBinder<?> parameterBinder : parameterBinders) {
                if (argumentBinderMap.put(parameterBinder.getHandledType(), parameterBinder) != null) {
                    throw new IllegalArgumentException("Attempt to bind two handlers to " + parameterBinder.getHandledType());
                }
            }
            this.argumentBinders = Collections.unmodifiableMap(argumentBinderMap);
        }

        /**
         * Locates a handler which is responsible for processing the given parameter. If no explicit handler can
         * be located, a fallback handler is provided.
         *
         * @param annotation The annotations of the parameter for which a handler should be provided.
         * @param defaults   The defaults provider to be queried if no explicit handler mapping could be found.
         * @return A handler for processing the parameter with the given annotations.
         */
        private Handler handler(Annotation[] annotation, Iterator<? extends Annotation> defaults) {
            Handler handler = null;
            for (Annotation anAnnotation : annotation) {
                ParameterBinder<?> parameterBinder = argumentBinders.get(anAnnotation.annotationType());
                if (parameterBinder != null && handler != null) {
                    throw new IllegalStateException("Ambiguous binding for parameter annotated with two handled annotation types");
                } else if (parameterBinder != null /* && handler == null */) {
                    handler = makeHandler(parameterBinder, anAnnotation);
                }
            }
            if (handler == null) { // No handler was found: attempt using defaults provider.
                if (defaults.hasNext()) {
                    Annotation defaultAnnotation = defaults.next();
                    ParameterBinder<?> parameterBinder = argumentBinders.get(defaultAnnotation.annotationType());
                    if (parameterBinder == null) {
                        return Handler.Unbound.INSTANCE;
                    } else {
                        handler = makeHandler(parameterBinder, defaultAnnotation);
                    }
                } else {
                    return Handler.Unbound.INSTANCE;
                }
            }
            return handler;
        }

        /**
         * Creates a handler for a given annotation.
         *
         * @param parameterBinder The parameter binder that should process an annotation.
         * @param annotation      An annotation instance that can be understood by this parameter binder.
         * @return A handler for processing the given annotation.
         */
        @SuppressWarnings("unchecked")
        private Handler makeHandler(ParameterBinder<?> parameterBinder, Annotation annotation) {
            return new Handler.Bound<Annotation>((ParameterBinder<Annotation>) parameterBinder, annotation);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && argumentBinders.equals(((DelegationProcessor) other).argumentBinders);
        }

        @Override
        public int hashCode() {
            return argumentBinders.hashCode();
        }

        @Override
        public String toString() {
            return "TargetMethodAnnotationDrivenBinder.DelegationProcessor{" +
                    "argumentBinders=" + argumentBinders +
                    '}';
        }

        /**
         * A handler is responsible for processing a parameter's binding.
         */
        private static interface Handler {

            /**
             * Handles a parameter binding.
             *
             * @param targetParameterIndex  The index of the target method's parameter to be bound.
             * @param source                The source method that is bound to the {@code target} method.
             * @param target                The target method that is subject to be bound by the {@code source} method.
             * @param instrumentationTarget The target of the current instrumentation.
             * @param assigner              An assigner that can be used for applying the binding.
             * @return A parameter binding that reflects the given arguments.
             */
            ParameterBinding<?> bind(int targetParameterIndex,
                                     MethodDescription source,
                                     MethodDescription target,
                                     Instrumentation.Target instrumentationTarget,
                                     Assigner assigner);

            /**
             * An unbound handler is a fallback for returning an illegal binding for parameters for which no parameter
             * binder could be located.
             */
            static enum Unbound implements Handler {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public ParameterBinding<?> bind(int targetParameterIndex,
                                                MethodDescription source,
                                                MethodDescription target,
                                                Instrumentation.Target instrumentationTarget,
                                                Assigner assigner) {
                    return ParameterBinding.Illegal.INSTANCE;
                }
            }

            /**
             * A bound handler represents an unambiguous parameter binder that was located for a given array of
             * annotations.
             *
             * @param <T> The annotation type of a given handler.
             */
            static class Bound<T extends Annotation> implements Handler {

                /**
                 * The parameter binder that is actually responsible for binding the parameter.
                 */
                private final ParameterBinder<T> parameterBinder;

                /**
                 * The annotation value that lead to the binding of this handler.
                 */
                private final T annotation;

                /**
                 * Creates a new bound handler.
                 *
                 * @param parameterBinder The parameter binder that is actually responsible for binding the parameter.
                 * @param annotation      The annotation value that lead to the binding of this handler.
                 */
                public Bound(ParameterBinder<T> parameterBinder, T annotation) {
                    this.parameterBinder = parameterBinder;
                    this.annotation = annotation;
                }

                @Override
                public ParameterBinding<?> bind(int targetParameterIndex,
                                                MethodDescription source,
                                                MethodDescription target,
                                                Instrumentation.Target instrumentationTarget,
                                                Assigner assigner) {
                    return parameterBinder.bind(annotation,
                            targetParameterIndex,
                            source,
                            target,
                            instrumentationTarget,
                            assigner);
                }

                @Override
                public String toString() {
                    return "TargetMethodAnnotationDrivenBinder.DelegationProcessor.Handler.Bound{" +
                            "parameterBinder=" + parameterBinder +
                            ", annotation=" + annotation +
                            '}';
                }
            }
        }
    }
}
