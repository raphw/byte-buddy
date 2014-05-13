package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * This {@link net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder} binds
 * method by analyzing annotations found on the <i>target</i> method that is subject to a method binding.
 */
public class TargetMethodAnnotationDrivenBinder implements MethodDelegationBinder {

    private final DelegationProcessor delegationProcessor;
    private final DefaultsProvider<?> defaultsProvider;
    private final Assigner assigner;
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
    public MethodBinding bind(TypeDescription instrumentedType, MethodDescription source, MethodDescription target) {
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
        Iterator<? extends Annotation> defaults = defaultsProvider.makeIterator(instrumentedType, source, target);
        for (int targetParameterIndex = 0;
             targetParameterIndex < target.getParameterTypes().size();
             targetParameterIndex++) {
            ParameterBinding<?> parameterBinding = delegationProcessor
                    .handler(target.getParameterAnnotations()[targetParameterIndex], defaults)
                    .handle(targetParameterIndex,
                            source,
                            target,
                            instrumentedType,
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
         * @param annotation           The annotation that was cause for the delegation to this argument binder.
         * @param targetParameterIndex The index of the target method's parameter to be bound.
         * @param source               The source method that is bound to the {@code target} method.
         * @param target               Tge target method that is subject to be bound by the {@code source} method.
         * @param instrumentedType     The instrumented type that is subject to this binding.
         * @param assigner             An assigner that can be used for applying the binding.
         * @return A parameter binding for the requested target method parameter.
         */
        ParameterBinding<?> bind(T annotation,
                                 int targetParameterIndex,
                                 MethodDescription source,
                                 MethodDescription target,
                                 TypeDescription instrumentedType,
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
         * @param typeDescription A description of the type that is instrumented.
         * @param source          The source method that is bound to the {@code target} method.
         * @param target          Tge target method that is subject to be bound by the {@code source} method.
         * @return An iterator that supplies default annotations for
         */
        Iterator<T> makeIterator(TypeDescription typeDescription, MethodDescription source, MethodDescription target);

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
            public Iterator<Annotation> makeIterator(TypeDescription typeDescription,
                                                     MethodDescription source,
                                                     MethodDescription target) {
                return EmptyIterator.INSTANCE;
            }

            private static enum EmptyIterator implements Iterator<Annotation> {

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

    private static class DelegationProcessor {

        private final Map<Class<? extends Annotation>, ParameterBinder<?>> argumentBinders;

        private DelegationProcessor(List<ParameterBinder<?>> parameterBinders) {
            Map<Class<? extends Annotation>, ParameterBinder<?>> argumentBinderMap = new HashMap<Class<? extends Annotation>, ParameterBinder<?>>();
            for (ParameterBinder<?> parameterBinder : parameterBinders) {
                if (argumentBinderMap.put(parameterBinder.getHandledType(), parameterBinder) != null) {
                    throw new IllegalArgumentException("Attempt to bind two handlers to " + parameterBinder.getHandledType());
                }
            }
            this.argumentBinders = Collections.unmodifiableMap(argumentBinderMap);
        }

        private Handler handler(Annotation[] annotation, Iterator<? extends Annotation> defaults) {
            Handler handler = null;
            for (Annotation anAnnotation : annotation) {
                ParameterBinder<?> parameterBinder = argumentBinders.get(anAnnotation.annotationType());
                if (parameterBinder != null && handler != null) {
                    throw new IllegalStateException("Ambiguous binding for parameter annotated with two handled annotation types");
                } else if (parameterBinder != null /* && handler == null */) {
                    handler = makeDelegate(parameterBinder, anAnnotation);
                }
            }
            if (handler == null) { // No handler was found: attempt using defaults provider.
                if (defaults.hasNext()) {
                    Annotation defaultAnnotation = defaults.next();
                    ParameterBinder<?> parameterBinder = argumentBinders.get(defaultAnnotation.annotationType());
                    if (parameterBinder == null) {
                        return Handler.Unbound.INSTANCE;
                    } else {
                        handler = makeDelegate(parameterBinder, defaultAnnotation);
                    }
                } else {
                    return Handler.Unbound.INSTANCE;
                }
            }
            return handler;
        }

        @SuppressWarnings("unchecked")
        private Handler makeDelegate(ParameterBinder<?> parameterBinder, Annotation annotation) {
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

        private static interface Handler {

            ParameterBinding<?> handle(int targetParameterIndex,
                                       MethodDescription source,
                                       MethodDescription target,
                                       TypeDescription typeDescription,
                                       Assigner assigner);

            static enum Unbound implements Handler {

                INSTANCE;

                @Override
                public ParameterBinding<?> handle(int targetParameterIndex,
                                                  MethodDescription source,
                                                  MethodDescription target,
                                                  TypeDescription typeDescription,
                                                  Assigner assigner) {
                    return ParameterBinding.Illegal.INSTANCE;
                }
            }

            static class Bound<T extends Annotation> implements Handler {

                private final ParameterBinder<T> parameterBinder;
                private final T annotation;

                public Bound(ParameterBinder<T> parameterBinder, T annotation) {
                    this.parameterBinder = parameterBinder;
                    this.annotation = annotation;
                }

                @Override
                public ParameterBinding<?> handle(int targetParameterIndex,
                                                  MethodDescription source,
                                                  MethodDescription target,
                                                  TypeDescription typeDescription,
                                                  Assigner assigner) {
                    return parameterBinder.bind(annotation,
                            targetParameterIndex,
                            source,
                            target,
                            typeDescription,
                            assigner);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && annotation.equals(((Bound) other).annotation)
                            && parameterBinder.equals(((Bound) other).parameterBinder);
                }

                @Override
                public int hashCode() {
                    return 31 * parameterBinder.hashCode() + annotation.hashCode();
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
