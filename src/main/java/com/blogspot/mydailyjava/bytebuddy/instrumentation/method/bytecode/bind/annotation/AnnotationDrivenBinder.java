package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.IllegalStackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.IllegalMethodDelegation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.util.*;

public class AnnotationDrivenBinder implements MethodDelegationBinder {

    public static interface ArgumentBinder<T extends Annotation> {

        static class IdentifiedBinding<S> {

            public static IdentifiedBinding<?> makeIllegal() {
                return new IdentifiedBinding<Object>(IllegalStackManipulation.INSTANCE, new Object());
            }

            public static IdentifiedBinding<?> makeAnonymous(StackManipulation stackManipulation) {
                return new IdentifiedBinding<Object>(stackManipulation, new Object());
            }

            public static <U> IdentifiedBinding<U> makeIdentified(StackManipulation stackManipulation, U identificationToken) {
                return new IdentifiedBinding<U>(stackManipulation, identificationToken);
            }

            private final StackManipulation stackManipulation;
            private final S identificationToken;

            protected IdentifiedBinding(StackManipulation stackManipulation, S identificationToken) {
                this.stackManipulation = stackManipulation;
                this.identificationToken = identificationToken;
            }

            public StackManipulation getStackManipulation() {
                return stackManipulation;
            }

            public S getIdentificationToken() {
                return identificationToken;
            }

            public boolean isValid() {
                return stackManipulation.isValid();
            }
        }

        Class<T> getHandledType();

        IdentifiedBinding<?> bind(T annotation,
                                  int targetParameterIndex,
                                  MethodDescription source,
                                  MethodDescription target,
                                  TypeDescription instrumentedType,
                                  Assigner assigner);
    }

    public static interface DefaultProvider<T extends Annotation> {

        static enum Empty implements DefaultProvider<Annotation> {
            INSTANCE;

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

            @Override
            public Iterator<Annotation> makeIterator(TypeDescription typeDescription,
                                                     MethodDescription source,
                                                     MethodDescription target) {
                return EmptyIterator.INSTANCE;
            }
        }

        Iterator<T> makeIterator(TypeDescription typeDescription, MethodDescription source, MethodDescription target);
    }

    private static class DelegationProcessor {

        private static interface Handler {

            static class Bound<T extends Annotation> implements Handler {

                private final ArgumentBinder<T> argumentBinder;
                private final T annotation;

                public Bound(ArgumentBinder<T> argumentBinder, T annotation) {
                    this.argumentBinder = argumentBinder;
                    this.annotation = annotation;
                }

                @Override
                public ArgumentBinder.IdentifiedBinding<?> handle(int targetParameterIndex,
                                                                  MethodDescription source,
                                                                  MethodDescription target,
                                                                  TypeDescription typeDescription,
                                                                  Assigner assigner) {
                    return argumentBinder.bind(annotation, targetParameterIndex, source, target, typeDescription, assigner);
                }
            }

            static enum Unbound implements Handler {
                INSTANCE;

                @Override
                public ArgumentBinder.IdentifiedBinding<?> handle(int targetParameterIndex,
                                                                  MethodDescription source,
                                                                  MethodDescription target,
                                                                  TypeDescription typeDescription,
                                                                  Assigner assigner) {
                    return ArgumentBinder.IdentifiedBinding.makeIllegal();
                }
            }

            ArgumentBinder.IdentifiedBinding<?> handle(int targetParameterIndex,
                                                       MethodDescription source,
                                                       MethodDescription target,
                                                       TypeDescription typeDescription,
                                                       Assigner assigner);
        }

        private final Map<Class<? extends Annotation>, ArgumentBinder<?>> argumentBinders;

        private DelegationProcessor(List<ArgumentBinder<?>> argumentBinders) {
            Map<Class<? extends Annotation>, ArgumentBinder<?>> argumentBinderMap = new HashMap<Class<? extends Annotation>, ArgumentBinder<?>>();
            for (ArgumentBinder<?> argumentBinder : argumentBinders) {
                if (argumentBinderMap.put(argumentBinder.getHandledType(), argumentBinder) != null) {
                    throw new IllegalArgumentException("Attempt to bind two handlers to " + argumentBinder.getHandledType());
                }
            }
            this.argumentBinders = Collections.unmodifiableMap(argumentBinderMap);
        }

        private Handler handler(Annotation[] annotation, Iterator<? extends Annotation> defaults) {
            Handler handler = null;
            for (Annotation anAnnotation : annotation) {
                ArgumentBinder<?> argumentBinder = argumentBinders.get(anAnnotation.annotationType());
                if (argumentBinder != null && handler != null) {
                    throw new IllegalArgumentException("Ambiguous binding for parameter annotated with two handled annotation types");
                } else if (argumentBinder != null /* && handler == null */) {
                    handler = makeDelegate(argumentBinder, anAnnotation);
                }
            }
            if (handler == null) { // No handler was found: attempt default.
                if (defaults.hasNext()) {
                    Annotation defaultAnnotation = defaults.next();
                    ArgumentBinder<?> argumentBinder = argumentBinders.get(defaultAnnotation.annotationType());
                    if (argumentBinder == null) {
                        return Handler.Unbound.INSTANCE;
                    } else {
                        handler = makeDelegate(argumentBinder, defaultAnnotation);
                    }
                } else {
                    return Handler.Unbound.INSTANCE;
                }
            }
            return handler;
        }

        @SuppressWarnings("unchecked")
        private Handler makeDelegate(ArgumentBinder<?> argumentBinder, Annotation annotation) {
            return new Handler.Bound<Annotation>((ArgumentBinder<Annotation>) argumentBinder, annotation);
        }
    }

    private final DelegationProcessor delegationProcessor;
    private final DefaultProvider<?> defaultProvider;
    private final Assigner assigner;
    private final MethodInvoker methodInvoker;

    public AnnotationDrivenBinder(List<ArgumentBinder<?>> argumentBinders,
                                  DefaultProvider<?> defaultProvider,
                                  Assigner assigner,
                                  MethodInvoker methodInvoker) {
        this.delegationProcessor = new DelegationProcessor(argumentBinders);
        this.defaultProvider = defaultProvider;
        this.assigner = assigner;
        this.methodInvoker = methodInvoker;
    }

    @Override
    public Binding bind(TypeDescription proxyType, MethodDescription source, MethodDescription target) {
        if (IgnoreForBinding.Verifier.check(target)) {
            return IllegalMethodDelegation.INSTANCE;
        }
        StackManipulation returningStackManipulation = assigner.assign(target.getReturnType(),
                source.getReturnType(),
                RuntimeType.Verifier.check(target));
        if (!returningStackManipulation.isValid()) {
            return IllegalMethodDelegation.INSTANCE;
        }
        Binding.Builder methodDelegationBindingBuilder = new Binding.Builder(methodInvoker, target);
        Iterator<? extends Annotation> defaults = defaultProvider.makeIterator(proxyType, source, target);
        for (int targetParameterIndex = 0;
             targetParameterIndex < target.getParameterTypes().size();
             targetParameterIndex++) {
            ArgumentBinder.IdentifiedBinding<?> identifiedBinding = delegationProcessor
                    .handler(target.getParameterAnnotations()[targetParameterIndex], defaults)
                    .handle(targetParameterIndex,
                            source,
                            target,
                            proxyType,
                            assigner);
            if (!identifiedBinding.isValid()
                    || !methodDelegationBindingBuilder.append(
                    identifiedBinding.getStackManipulation(),
                    targetParameterIndex,
                    identifiedBinding.getIdentificationToken())) {
                return IllegalMethodDelegation.INSTANCE;
            }
        }
        return methodDelegationBindingBuilder.build(returningStackManipulation);
    }
}
