package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.IllegalMethodDelegation;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MethodDelegationBinder;
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
                                  Assigner assigner);
    }

    private static class DefaultAnnotation implements Argument {

        private final int parameterIndex;

        private DefaultAnnotation(int parameterIndex) {
            this.parameterIndex = parameterIndex;
        }

        @Override
        public int value() {
            return parameterIndex;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Argument.class;
        }
    }

    private static class ArgumentHandler {

        private static interface Delegate {

            static class Explicit<T extends Annotation> implements Delegate {
                private final ArgumentBinder<T> argumentBinder;
                private final T annotation;

                private Explicit(ArgumentBinder<T> argumentBinder, T annotation) {
                    this.argumentBinder = argumentBinder;
                    this.annotation = annotation;
                }

                @Override
                public ArgumentBinder.IdentifiedBinding<?> handle(int targetParameterIndex,
                                                                  MethodDescription source,
                                                                  MethodDescription target,
                                                                  Assigner assigner) {
                    return argumentBinder.bind(annotation,
                            targetParameterIndex,
                            source,
                            target,
                            assigner);
                }
            }

            static class Default implements Delegate {

                private final int sourceParameterIndex;

                public Default(int sourceParameterIndex) {
                    this.sourceParameterIndex = sourceParameterIndex;
                }

                @Override
                public ArgumentBinder.IdentifiedBinding<?> handle(int targetParameterIndex,
                                                                  MethodDescription source,
                                                                  MethodDescription target,
                                                                  Assigner assigner) {
                    return new Argument.Binder().bind(new DefaultAnnotation(sourceParameterIndex),
                            targetParameterIndex,
                            source,
                            target,
                            assigner);
                }
            }

            ArgumentBinder.IdentifiedBinding<?> handle(int targetParameterIndex,
                                                       MethodDescription source,
                                                       MethodDescription target,
                                                       Assigner assigner);
        }

        private final Map<Class<? extends Annotation>, ArgumentBinder<?>> argumentBinders;

        private ArgumentHandler(List<ArgumentBinder<?>> argumentBinders) {
            Map<Class<? extends Annotation>, ArgumentBinder<?>> argumentBinderMap = new HashMap<Class<? extends Annotation>, ArgumentBinder<?>>();
            for (ArgumentBinder<?> argumentBinder : argumentBinders) {
                if (argumentBinderMap.put(argumentBinder.getHandledType(), argumentBinder) != null) {
                    throw new IllegalArgumentException("Attempt to bind two handlers to " + argumentBinder.getHandledType());
                }
            }
            this.argumentBinders = Collections.unmodifiableMap(argumentBinderMap);
        }

        private Delegate select(Annotation[] annotation) {
            Delegate delegate = null;
            for (Annotation anAnnotation : annotation) {
                ArgumentBinder<?> argumentBinder = argumentBinders.get(anAnnotation.annotationType());
                if (argumentBinder != null && delegate != null) {
                    throw new IllegalArgumentException("Ambiguous binding for parameter annotated with two handled annotation types");
                } else if (argumentBinder != null /* && delegate == null */) {
                    delegate = makeDelegate(argumentBinder, anAnnotation);
                }
            }
            return delegate == null ? new Delegate.Default(-1) : delegate;
        }

        @SuppressWarnings("unchecked")
        private Delegate makeDelegate(ArgumentBinder<?> argumentBinder, Annotation annotation) {
            return new Delegate.Explicit<Annotation>((ArgumentBinder<Annotation>) argumentBinder, annotation);
        }
    }

    private static class DelegationBuilder {

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
            public Size apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                Assignment.Size argumentSize = new Assignment.Size(0, 0);
                for (Assignment assignment : assignments) {
                    argumentSize.aggregate(assignment.apply(methodVisitor));
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        targetMethodDescription.getDeclaringClassInternalName(),
                        targetMethodDescription.getInternalName(),
                        targetMethodDescription.getDescriptor());
                Assignment.Size returnSize = returningAssignment.apply(methodVisitor);
                return new Size(Math.max(argumentSize.getMaximalSize(), returnSize.getMaximalSize()), TypeSize.sizeOf(methodDescription));
            }
        }

        private final MethodDescription target;
        private final Assignment returningAssignment;
        private final List<Assignment> assignments;
        private final Map<Object, Integer> registeredTargetIndices;

        private DelegationBuilder(MethodDescription target, Assignment returningAssignment) {
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

    private final ArgumentHandler argumentHandler;
    private final Assigner assigner;

    public AnnotationDrivenBinder(List<ArgumentBinder<?>> argumentBinders, Assigner assigner) {
        this.argumentHandler = new ArgumentHandler(argumentBinders);
        this.assigner = assigner;
    }

    @Override
    public BoundMethodDelegation bind(MethodDescription source, MethodDescription target) {
        Assignment returningAssignment = assigner.assign(target.getReturnType(),
                source.getReturnType(),
                target.isAnnotationPresent(RuntimeType.class));
        if (!returningAssignment.isAssignable()) {
            return IllegalMethodDelegation.INSTANCE;
        }
        DelegationBuilder delegationBuilder = new DelegationBuilder(target, returningAssignment);
        for (int targetParameterIndex = 0;
             targetParameterIndex < target.getParameterTypes().length;
             targetParameterIndex++) {
            ArgumentBinder.IdentifiedBinding<?> identifiedBinding = argumentHandler
                    .select(target.getParameterAnnotations()[targetParameterIndex])
                    .handle(targetParameterIndex,
                            source,
                            target,
                            assigner);
            if (!identifiedBinding.isValid() || !delegationBuilder.append(identifiedBinding, targetParameterIndex)) {
                return IllegalMethodDelegation.INSTANCE;
            }
        }
        return delegationBuilder.build();
    }
}
