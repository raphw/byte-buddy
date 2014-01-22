package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.IllegalMethodDelegation;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.util.*;

public class AnnotationDrivenBinder implements MethodDelegationBinder {

    public static interface ArgumentBinder<T extends Annotation> {

        static class IdentifiedBinding<S> {

            public static IdentifiedBinding<?> makeIllegal() {
                return new IdentifiedBinding<Object>(IllegalAssignment.INSTANCE, new Object());
            }

            public static IdentifiedBinding<?> makeAnonymous(Assignment assignment) {
                return new IdentifiedBinding<Object>(assignment, new Object());
            }

            public static <U> IdentifiedBinding<U> makeIdentified(Assignment assignment, U identificationToken) {
                return new IdentifiedBinding<U>(assignment, identificationToken);
            }

            private final Assignment assignment;
            private final S identificationToken;

            protected IdentifiedBinding(Assignment assignment, S identificationToken) {
                this.assignment = assignment;
                this.identificationToken = identificationToken;
            }

            public Assignment getAssignment() {
                return assignment;
            }

            public S getIdentificationToken() {
                return identificationToken;
            }

            public boolean isValid() {
                return assignment.isAssignable();
            }
        }

        Class<T> getHandledType();

        IdentifiedBinding<?> bind(T annotation,
                                  int targetParameterIndex,
                                  MethodDescription source,
                                  MethodDescription target,
                                  TypeDescription typeDescription,
                                  Assigner assigner);
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
            if (handler == null) {
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

    private static class MethodDelegationBuilder {

        private static class Build implements BoundMethodDelegation {

            private final MethodDescription targetMethodDescription;
            private final Assignment returningAssignment;
            private final List<Assignment> assignments;
            private final Map<Object, Integer> registeredTargetIndices;

            private Build(MethodDescription targetMethodDescription,
                          Assignment returningAssignment,
                          List<Assignment> assignments,
                          Map<Object, Integer> registeredTargetIndices) {
                this.targetMethodDescription = targetMethodDescription;
                this.returningAssignment = returningAssignment;
                this.assignments = assignments;
                this.registeredTargetIndices = registeredTargetIndices;
            }

            @Override
            public boolean isBound() {
                return true;
            }

            @Override
            public Integer getBindingIndex(Object identificationToken) {
                return registeredTargetIndices.get(identificationToken);
            }

            @Override
            public MethodDescription getBindingTarget() {
                return targetMethodDescription;
            }

            @Override
            public ByteCodeAppender.Size apply(MethodVisitor methodVisitor, MethodDescription sourceMethod) {
                Assignment.Size argumentSize = new Assignment.Size(0, 0);
                for (Assignment assignment : assignments) {
                    argumentSize = argumentSize.aggregate(assignment.apply(methodVisitor));
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        targetMethodDescription.getDeclaringClassInternalName(),
                        targetMethodDescription.getInternalName(),
                        targetMethodDescription.getDescriptor());
                Assignment.Size returnSize = TypeSize.of(targetMethodDescription.getReturnType()).toIncreasingSize();
                returnSize = returnSize.aggregate(returningAssignment.apply(methodVisitor));
                returnSize = returnSize.aggregate(MethodReturn.returning(sourceMethod.getReturnType()).apply(methodVisitor));
                return new ByteCodeAppender.Size(
                        Math.max(argumentSize.getMaximalSize(), returnSize.getMaximalSize()),
                        TypeSize.sizeOf(sourceMethod));
            }
        }

        private final MethodDescription target;
        private final Assignment returningAssignment;
        private final List<Assignment> assignments;
        private final Map<Object, Integer> registeredTargetIndices;

        private MethodDelegationBuilder(MethodDescription target, Assignment returningAssignment) {
            this.target = target;
            this.returningAssignment = returningAssignment;
            this.assignments = new ArrayList<Assignment>(target.getParameterTypes().length);
            this.registeredTargetIndices = new LinkedHashMap<Object, Integer>(target.getParameterTypes().length);
        }

        public boolean append(ArgumentBinder.IdentifiedBinding<?> identifiedBinding, int targetParameterIndex) {
            assignments.add(identifiedBinding.getAssignment());
            return registeredTargetIndices.put(identifiedBinding.getIdentificationToken(), targetParameterIndex) == null;
        }

        public BoundMethodDelegation build() {
            return new Build(target, returningAssignment, assignments, registeredTargetIndices);
        }
    }

    public static interface AnnotationDefaultHandler<T extends Annotation> {

        static enum Empty implements AnnotationDefaultHandler<Annotation> {
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

    private final DelegationProcessor delegationProcessor;
    private final AnnotationDefaultHandler<?> annotationDefaultHandler;
    private final Assigner assigner;

    public AnnotationDrivenBinder(List<ArgumentBinder<?>> argumentBinders,
                                  AnnotationDefaultHandler<?> annotationDefaultHandler,
                                  Assigner assigner) {
        this.delegationProcessor = new DelegationProcessor(argumentBinders);
        this.annotationDefaultHandler = annotationDefaultHandler;
        this.assigner = assigner;
    }

    @Override
    public BoundMethodDelegation bind(TypeDescription typeDescription, MethodDescription source, MethodDescription target) {
        Assignment returningAssignment = assigner.assign(target.getReturnType(),
                source.getReturnType(),
                target.isAnnotationPresent(RuntimeType.class));
        if (!returningAssignment.isAssignable()) {
            return IllegalMethodDelegation.INSTANCE;
        }
        MethodDelegationBuilder methodDelegationBuilder = new MethodDelegationBuilder(target, returningAssignment);
        Iterator<? extends Annotation> defaults = annotationDefaultHandler.makeIterator(typeDescription, source, target);
        for (int targetParameterIndex = 0;
             targetParameterIndex < target.getParameterTypes().length;
             targetParameterIndex++) {
            ArgumentBinder.IdentifiedBinding<?> identifiedBinding = delegationProcessor
                    .handler(target.getParameterAnnotations()[targetParameterIndex], defaults)
                    .handle(targetParameterIndex,
                            source,
                            target,
                            typeDescription,
                            assigner);
            if (!identifiedBinding.isValid() || !methodDelegationBuilder.append(identifiedBinding, targetParameterIndex)) {
                return IllegalMethodDelegation.INSTANCE;
            }
        }
        return methodDelegationBuilder.build();
    }
}
