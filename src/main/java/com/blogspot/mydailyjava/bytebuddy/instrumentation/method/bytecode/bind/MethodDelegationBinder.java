package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

/**
 * A method delegation binder is responsible for creating a method binding for a <i>source method</i> to a
 * <i>target method</i>. Such a binding allows to implement the source method by calling the target method.
 * <p>&nbsp;</p>
 * Usually, an instrumentation will attempt to bind a specific source method to a set of target method candidates
 * where all legal bindings are considered for binding. To chose a specific candidate, an
 * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}
 * will be consulted for selecting a <i>best</i> binding.
 */
public interface MethodDelegationBinder {

    /**
     * Implementations are used as delegates for invoking a method that was bound
     * using a {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder}.
     */
    public static interface MethodInvoker {

        /**
         * A simple method invocation that merely uses the most general form of method invocation as provided by
         * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation}.
         */
        static enum Simple implements MethodInvoker {
            INSTANCE;

            @Override
            public StackManipulation invoke(MethodDescription methodDescription) {
                return MethodInvocation.invoke(methodDescription);
            }
        }

        /**
         * A method invocation that enforces a virtual invocation that is dispatched on a given type.
         */
        static class Virtual implements MethodInvoker {

            private final TypeDescription typeDescription;

            /**
             * Creates an immutable method invoker that dispatches all methods on a given type.
             *
             * @param typeDescription The type on which the method is invoked by virtual invocation.
             */
            public Virtual(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            @Override
            public StackManipulation invoke(MethodDescription methodDescription) {
                return MethodInvocation.invoke(methodDescription).virtual(typeDescription);
            }
        }

        /**
         * Creates a method invocation for a given method.
         *
         * @param methodDescription The method to be invoked.
         * @return A stack manipulation encapsulating this method invocation.
         */
        StackManipulation invoke(MethodDescription methodDescription);
    }

    /**
     * A binding attempt for a single parameter. Implementations of this type are a suggestion of composing a
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.MethodBinding}
     * by using a
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.MethodBinding.Builder}.
     * However, method bindings can also be composed without this type which is merely a suggestion.
     *
     * @param <T> The type of the identification token for this parameter binding.
     */
    static interface ParameterBinding<T> extends StackManipulation {

        /**
         * A singleton representation of an illegal binding for a method parameter. An illegal binding usually
         * suggests that a source method cannot be bound to a specific target method.
         */
        static enum Illegal implements ParameterBinding<Void> {
            INSTANCE;

            @Override
            public Void getIdentificationToken() {
                throw new IllegalStateException();
            }

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                throw new IllegalStateException();
            }
        }

        /**
         * An anonymous binding of a target method parameter.
         */
        static class Anonymous implements ParameterBinding<Object> {

            private final Object anonymousToken;
            private final StackManipulation delegate;

            /**
             * Creates a new, anonymous parameter binding.
             *
             * @param delegate The stack manipulation that is responsible for loading the parameter value for this
             *                 target method parameter onto the stack.
             */
            public Anonymous(StackManipulation delegate) {
                this.delegate = delegate;
                anonymousToken = new Object();
            }

            @Override
            public Object getIdentificationToken() {
                return anonymousToken;
            }

            @Override
            public boolean isValid() {
                return delegate.isValid();
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                return delegate.apply(methodVisitor, instrumentationContext);
            }
        }

        /**
         * A uniquely identifiable parameter binding for a target method. Such bindings are usually later processed by
         * a {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}
         * in order to resolve binding conflicts between several bindable target methods to the same source method.
         *
         * @param <T> The type of the identification token.
         * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver
         */
        static class Unique<T> implements ParameterBinding<T> {

            /**
             * A factory method for creating a unique binding that infers the tokens type.
             *
             * @param delegate            The stack manipulation delegate.
             * @param identificationToken The identification token.
             * @param <S>                 The type of the identification token.
             * @return A new instance representing this unique binding.
             */
            public static <S> Unique<S> of(StackManipulation delegate, S identificationToken) {
                return new Unique<S>(delegate, identificationToken);
            }

            private final StackManipulation delegate;
            private final T identificationToken;

            /**
             * Creates a new unique parameter binding representant.
             *
             * @param delegate            The stack manipulation that loads the argument for this parameter onto the operand stack.
             * @param identificationToken The token used for identifying this parameter binding.
             */
            public Unique(StackManipulation delegate, T identificationToken) {
                this.delegate = delegate;
                this.identificationToken = identificationToken;
            }

            @Override
            public T getIdentificationToken() {
                return identificationToken;
            }

            @Override
            public boolean isValid() {
                return delegate.isValid();
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                return delegate.apply(methodVisitor, instrumentationContext);
            }
        }

        /**
         * Returns an identification token for
         *
         * @return An identification token unique to this binding.
         */
        T getIdentificationToken();
    }

    /**
     * A binding attempt created by a
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder}.
     */
    static interface MethodBinding extends StackManipulation {

        /**
         * Representation of an attempt to bind a source method to a target method that is not applicable.
         *
         * @see com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder
         */
        static enum Illegal implements MethodBinding {
            INSTANCE;

            @Override
            public Integer getTargetParameterIndex(Object parameterBindingToken) {
                throw new IllegalStateException();
            }

            @Override
            public MethodDescription getTarget() {
                throw new IllegalStateException();
            }


            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                throw new IllegalStateException();
            }
        }

        /**
         * A mutable builder that allows to compose a
         * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.MethodBinding}
         * by adding parameter bindings incrementally.
         */
        static class Builder {

            private static class Build implements MethodBinding {

                private final MethodDescription target;
                private final Map<?, Integer> registeredTargetIndices;
                private final StackManipulation methodInvocation;
                private final List<StackManipulation> parameterStackManipulations;
                private final StackManipulation returnValueStackManipulation;

                private Build(MethodDescription target,
                              Map<?, Integer> registeredTargetIndices,
                              StackManipulation methodInvocation,
                              List<StackManipulation> parameterStackManipulations,
                              StackManipulation returnValueStackManipulation) {
                    this.target = target;
                    this.registeredTargetIndices = new HashMap<Object, Integer>(registeredTargetIndices);
                    this.methodInvocation = methodInvocation;
                    this.parameterStackManipulations = new ArrayList<StackManipulation>(parameterStackManipulations);
                    this.returnValueStackManipulation = returnValueStackManipulation;
                }

                @Override
                public boolean isValid() {
                    boolean result = returnValueStackManipulation.isValid() && methodInvocation.isValid();
                    Iterator<StackManipulation> assignment = parameterStackManipulations.iterator();
                    while (result && assignment.hasNext()) {
                        result = assignment.next().isValid();
                    }
                    return result;
                }

                @Override
                public Integer getTargetParameterIndex(Object parameterBindingToken) {
                    return registeredTargetIndices.get(parameterBindingToken);
                }

                @Override
                public MethodDescription getTarget() {
                    return target;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                    Size size = new Size(0, 0);
                    for (StackManipulation stackManipulation : parameterStackManipulations) {
                        size = size.aggregate(stackManipulation.apply(methodVisitor, instrumentationContext));
                    }
                    size = size.aggregate(methodInvocation.apply(methodVisitor, instrumentationContext));
                    return size.aggregate(returnValueStackManipulation.apply(methodVisitor, instrumentationContext));
                }

                @Override
                public String toString() {
                    return "MethodBinding to " + target + " (" + (isValid() ? "valid" : "invalid") + ')';
                }
            }

            private final MethodInvoker methodInvoker;
            private final MethodDescription target;
            private final List<StackManipulation> parameterStackManipulations;
            private final LinkedHashMap<Object, Integer> registeredTargetIndices;

            private int currentParameterIndex;

            /**
             * Creates a new builder for the binding of a given method.
             *
             * @param methodInvoker The method invoker that is used to create the method invocation of the {@code target}
             *                      method.
             * @param target        The target method that is target of the binding.
             */
            public Builder(MethodInvoker methodInvoker, MethodDescription target) {
                this.methodInvoker = methodInvoker;
                this.target = target;
                parameterStackManipulations = new ArrayList<StackManipulation>(target.getParameterTypes().size());
                registeredTargetIndices = new LinkedHashMap<Object, Integer>(target.getParameterTypes().size());
                currentParameterIndex = 0;
            }

            /**
             * Appends a stack manipulation for the next parameter of the target method.
             *
             * @param parameterBinding A binding representing the next subsequent parameter of the method.
             * @return {@code false} if the {@code parameterBindingToken} was already bound. A conflicting binding should
             * usually abort the attempt of binding a method and this {@code Builder} should be discarded.
             */
            public boolean append(ParameterBinding<?> parameterBinding) {
                parameterStackManipulations.add(parameterBinding);
                return registeredTargetIndices.put(parameterBinding.getIdentificationToken(), currentParameterIndex++) == null;
            }

            /**
             * Creates a binding that represents the bindings collected by this {@code Builder.}
             *
             * @param returnValueStackManipulation A stack manipulation applied to the target method's return value.
             * @return A binding representing the parameter bindings collected by this builder.
             */
            public MethodBinding build(StackManipulation returnValueStackManipulation) {
                if (target.getParameterTypes().size() != currentParameterIndex) {
                    throw new IllegalStateException("The number of parameters bound does not equal the target's number of parameters");
                }
                return new Build(target,
                        registeredTargetIndices,
                        methodInvoker.invoke(target),
                        parameterStackManipulations,
                        returnValueStackManipulation);
            }

            /**
             * Returns the current parameter index that will be bound on the next call of
             * {@link Builder#append(com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.ParameterBinding)}.
             *
             * @return The next index to be bound.
             */
            public int getCurrentParameterIndex() {
                return currentParameterIndex;
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.MethodBinding.Builder{" + "methodInvoker=" + methodInvoker
                        + ", target=" + target
                        + ", parameterStackManipulations=" + parameterStackManipulations +
                        ", registeredTargetIndices=" + registeredTargetIndices + '}';
            }
        }

        /**
         * Returns the target method's parameter index for a given parameter binding token.
         * <p>&nbsp;</p>
         * A binding token can be any object
         * that implements valid {@link Object#hashCode()} and {@link Object#equals(Object)} methods in order
         * to look up a given binding. This way, two bindings can be evaluated of having performed a similar type of
         * binding such that these bindings can be compared and a dominant binding can be identified by an
         * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}.
         * Furthermore, a binding is implicitly required to insure the uniqueness of such a parameter binding.
         *
         * @param parameterBindingToken A token which is used to identify a specific unique binding for a given parameter
         *                              of the target method.
         * @return The target method's parameter index of this binding or {@code null} if no such argument binding
         * was applied for this binding.
         */
        Integer getTargetParameterIndex(Object parameterBindingToken);

        /**
         * Returns the target method of the method binding attempt.
         *
         * @return The target method to which the
         */
        MethodDescription getTarget();
    }

    /**
     * Implementations of this interface are able to attempt the resolution of two successful bindings of a method
     * to two different target methods in order to identify a dominating binding.
     */
    static interface AmbiguityResolver {

        /**
         * A resolution state of an attempt to resolve two conflicting bindings.
         */
        static enum Resolution {

            /**
             * Describes a resolution state where no information about dominance could be gathered.
             */
            UNKNOWN(true),
            /**
             * Describes a resolution state where the left method dominates the right method.
             */
            LEFT(false),
            /**
             * Describes a resolution state where the right method dominates the left method.
             */
            RIGHT(false),
            /**
             * Describes a resolution state where both methods have inflicting dominance over each other.
             */
            AMBIGUOUS(true);

            private final boolean unresolved;

            private Resolution(boolean unresolved) {
                this.unresolved = unresolved;
            }

            /**
             * Checks if this binding is unresolved.
             *
             * @return {@code true} if this binding is unresolved.
             */
            public boolean isUnresolved() {
                return unresolved;
            }

            /**
             * Merges two resolutions in order to determine their compatibility.
             *
             * @param other The resolution this resolution is to be checked against.
             * @return The merged resolution.
             */
            public Resolution merge(Resolution other) {
                switch (this) {
                    case UNKNOWN:
                        return other;
                    case AMBIGUOUS:
                        return AMBIGUOUS;
                    case LEFT:
                    case RIGHT:
                        return other == this ? this : AMBIGUOUS;
                    default:
                        throw new AssertionError();
                }
            }
        }

        /**
         * An ambiguity resolver that does not attempt to resolve a conflicting binding.
         */
        static enum NoOp implements AmbiguityResolver {
            INSTANCE;

            @Override
            public Resolution resolve(MethodDescription source, MethodBinding left, MethodBinding right) {
                return Resolution.UNKNOWN;
            }
        }

        /**
         * A chain of {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}s
         * that are applied in the given order until two bindings can be resolved.
         */
        static class Chain implements AmbiguityResolver {

            /**
             * Chains a given number of ambiguity resolvers.
             *
             * @param ambiguityResolver The ambiguity resolvers to chain in the order of their application.
             * @return A chained ambiguity resolver representing the given ambiguity resolvers.
             */
            public static AmbiguityResolver of(AmbiguityResolver... ambiguityResolver) {
                if (ambiguityResolver.length == 1) {
                    return ambiguityResolver[0];
                } else {
                    return new Chain(ambiguityResolver);
                }
            }

            private final List<AmbiguityResolver> ambiguityResolvers;

            /**
             * Creates an immutable chain of ambiguity resolvers.
             *
             * @param ambiguityResolver The ambiguity resolvers to chain in the order of their application.
             */
            protected Chain(AmbiguityResolver... ambiguityResolver) {
                ambiguityResolvers = unchained(Arrays.asList(ambiguityResolver));
            }

            private static List<AmbiguityResolver> unchained(List<AmbiguityResolver> chained) {
                List<AmbiguityResolver> ambiguityResolvers = new ArrayList<AmbiguityResolver>();
                for (AmbiguityResolver ambiguityResolver : chained) {
                    if (ambiguityResolver instanceof Chain) {
                        ambiguityResolvers.addAll(unchained(((Chain) ambiguityResolver).ambiguityResolvers));
                    } else {
                        ambiguityResolvers.add(ambiguityResolver);
                    }
                }
                return ambiguityResolvers;
            }

            @Override
            public Resolution resolve(MethodDescription source,
                                      MethodBinding left,
                                      MethodBinding right) {
                Resolution resolution = Resolution.UNKNOWN;
                Iterator<AmbiguityResolver> iterator = ambiguityResolvers.iterator();
                while (resolution.isUnresolved() && iterator.hasNext()) {
                    resolution = iterator.next().resolve(source, left, right);
                }
                return resolution;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && ambiguityResolvers.equals(((Chain) other).ambiguityResolvers);
            }

            @Override
            public int hashCode() {
                return ambiguityResolvers.hashCode();
            }

            @Override
            public String toString() {
                return "AmbiguityResolver.Chain{ambiguityResolvers=" + ambiguityResolvers + '}';
            }
        }

        /**
         * Attempts to resolve to conflicting bindings.
         *
         * @param source The source method that was bound to both target methods.
         * @param left   The first successful binding of the {@code source} method.
         * @param right  The second successful binding of the {@code source} method.
         * @return The resolution state when resolving a conflicting binding where
         * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver.Resolution#LEFT}
         * indicates a successful binding to the {@code left} binding while
         * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver.Resolution#RIGHT}
         * indicates a successful binding to the {@code right} binding.
         */
        Resolution resolve(MethodDescription source, MethodBinding left, MethodBinding right);
    }

    /**
     * A helper class that allows to identify a best binding for a given type and source method chosing from a list of given
     * target methods by using a given {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder}
     * and an {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}.
     * <p>&nbsp;</p>
     * The {@code Processor} will:
     * <ol>
     * <li>Try to bind the {@code source} method using the {@code MethodDelegationBinder}.</li>
     * <li>Find a best method among the successful bindings using the {@code AmbiguityResolver}.</li>
     * </ol>
     */
    static class Processor {

        private static final int ONLY = 0;
        private static final int LEFT = 0;
        private static final int RIGHT = 1;

        private final MethodDelegationBinder methodDelegationBinder;
        private final AmbiguityResolver ambiguityResolver;

        public Processor(MethodDelegationBinder methodDelegationBinder,
                         AmbiguityResolver ambiguityResolver) {
            this.methodDelegationBinder = methodDelegationBinder;
            this.ambiguityResolver = ambiguityResolver;
        }

        /**
         * Returns the {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder}
         * used by this {@code Processor}.
         *
         * @return The method delegation binder used by this {@code Processor}.
         */
        public MethodDelegationBinder getMethodDelegationBinder() {
            return methodDelegationBinder;
        }

        /**
         * Returns the {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}
         * used by this {@code Processor}.
         *
         * @return The ambiguity resolver used by this {@code Processor}.
         */
        public AmbiguityResolver getAmbiguityResolver() {
            return ambiguityResolver;
        }

        /**
         * @param instrumentedType The instrumented type that is target of binding the {@code source} method
         *                         to a delegate method.
         * @param source           The source method that is to be bound.
         * @param targets          All possible targets for the delegation binding that are to be considered.
         * @return The best binding that was identified. If no such binding can be identified, an exception is thrown.
         */
        public MethodBinding process(TypeDescription instrumentedType,
                                     MethodDescription source,
                                     Iterable<? extends MethodDescription> targets) {
            List<MethodBinding> possibleDelegations = bind(instrumentedType, source, targets);
            if (possibleDelegations.size() == 0) {
                throw new IllegalArgumentException("No method can be bound to " + source);
            }
            return resolve(source, possibleDelegations);
        }

        private List<MethodBinding> bind(TypeDescription instrumentedType,
                                         MethodDescription source,
                                         Iterable<? extends MethodDescription> targets) {
            List<MethodBinding> possibleDelegations = new LinkedList<MethodBinding>();
            for (MethodDescription target : targets) {
                MethodBinding methodBinding = methodDelegationBinder.bind(instrumentedType, source, target);
                if (methodBinding.isValid()) {
                    possibleDelegations.add(methodBinding);
                }
            }
            return possibleDelegations;
        }

        private MethodBinding resolve(MethodDescription source,
                                      List<MethodBinding> targets) {
            switch (targets.size()) {
                case 1:
                    return targets.get(ONLY);
                case 2: {
                    MethodBinding left = targets.get(LEFT);
                    MethodBinding right = targets.get(RIGHT);
                    switch (ambiguityResolver.resolve(source, left, right)) {
                        case LEFT:
                            return left;
                        case RIGHT:
                            return right;
                        case AMBIGUOUS:
                        case UNKNOWN:
                            throw new IllegalArgumentException("Could not resolve ambiguous delegation to " + left + " or " + right);
                        default:
                            throw new AssertionError();
                    }
                }
                default: /* case 3+: */ {
                    MethodBinding left = targets.get(LEFT);
                    MethodBinding right = targets.get(RIGHT);
                    switch (ambiguityResolver.resolve(source, left, right)) {
                        case LEFT:
                            targets.remove(RIGHT);
                            return resolve(source, targets);
                        case RIGHT:
                            targets.remove(LEFT);
                            return resolve(source, targets);
                        case AMBIGUOUS:
                        case UNKNOWN:
                            targets.remove(RIGHT); // Remove right element first due to index alteration!
                            targets.remove(LEFT);
                            MethodBinding subResult = resolve(source, targets);
                            switch (ambiguityResolver.resolve(source, left, subResult).merge(ambiguityResolver.resolve(source, right, subResult))) {
                                case RIGHT:
                                    return subResult;
                                case LEFT:
                                case AMBIGUOUS:
                                case UNKNOWN:
                                    throw new IllegalArgumentException("Could not resolve ambiguous delegation to either " + left + " or " + right);
                                default:
                                    throw new AssertionError();
                            }
                        default:
                            throw new AssertionError();
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "MethodDelegationBinder.Processor{" + "methodDelegationBinder=" + methodDelegationBinder
                    + ", ambiguityResolver=" + ambiguityResolver + '}';
        }
    }

    /**
     * Attempts a binding of a source method to a given target method.
     *
     * @param instrumentedType The type which is subject to instrumentation and onto which this binding
     *                         is to be applied.
     * @param source           The method that is to be bound to the {@code target} method.
     * @param target           The method that is to be invoked as a delegate.
     * @return A binding representing this attempt to bind the {@code source} method to the {@code target} method.
     */
    MethodBinding bind(TypeDescription instrumentedType, MethodDescription source, MethodDescription target);
}
