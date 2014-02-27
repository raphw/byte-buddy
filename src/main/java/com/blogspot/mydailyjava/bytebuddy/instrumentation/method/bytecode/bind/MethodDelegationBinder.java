package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

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
     * A binding attempt created by a
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder}.
     */
    static interface Binding extends StackManipulation {

        /**
         * A mutable builder that allows to compose a
         * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.Binding}
         * by adding parameter bindings incrementally.
         */
        static class Builder {

            private static class Build implements Binding {

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
                    return "Binding to " + target + " (" + (isValid() ? "valid" : "invalid") + ')';
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
             * @param stackManipulation     The stack manipulation that applies the next parameter's binding.
             * @param parameterBindingToken The identification token that uniquely identifies this binding. If the binding
             *                              is anonymous, the token should be represented by for example {@code new Object()}
             *                              which is itself an anonymous token since it is only equal to itself.
             * @return {@code false} if the {@code parameterBindingToken} was already bound. A conflicting binding should
             * usually abort the attempt of binding a method and this {@code Builder} should be discarded.
             */
            public boolean append(StackManipulation stackManipulation, Object parameterBindingToken) {
                parameterStackManipulations.add(stackManipulation);
                return registeredTargetIndices.put(parameterBindingToken, currentParameterIndex++) == null;
            }

            /**
             * Creates a binding that represents the bindings collected by this {@code Builder.}
             *
             * @param returnValueStackManipulation A stack manipulation applied to the target method's return value.
             * @return A binding representing the parameter bindings collected by this builder.
             */
            public Binding build(StackManipulation returnValueStackManipulation) {
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
             * {@link Builder#append(com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation, Object)}.
             *
             * @return The next index to be bound.
             */
            public int getCurrentParameterIndex() {
                return currentParameterIndex;
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.Binding.Builder{" + "methodInvoker=" + methodInvoker
                        + ", target=" + target
                        + ", parameterStackManipulations=" + parameterStackManipulations +
                        ", registeredTargetIndices=" + registeredTargetIndices + '}';
            }
        }

        /**
         * Returns the target method's parameter index for a given parameter binding token.
         * <p/>
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
            public Resolution resolve(MethodDescription source, Binding left, Binding right) {
                return Resolution.UNKNOWN;
            }
        }

        /**
         * A chain of {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}s
         * that are applied in the given order until two bindings can be resolved.
         */
        static class Chain implements AmbiguityResolver {

            private final AmbiguityResolver[] ambiguityResolver;

            /**
             * Creates an immutable chain of ambiguity resolvers.
             *
             * @param ambiguityResolver The ambiguity resolvers to chain in the order of their application.
             */
            public Chain(AmbiguityResolver... ambiguityResolver) {
                this.ambiguityResolver = ambiguityResolver;
            }

            @Override
            public Resolution resolve(MethodDescription source,
                                      Binding left,
                                      Binding right) {
                Resolution resolution = Resolution.UNKNOWN;
                Iterator<AmbiguityResolver> iterator = Arrays.asList(ambiguityResolver).iterator();
                while (resolution.isUnresolved() && iterator.hasNext()) {
                    resolution = iterator.next().resolve(source, left, right);
                }
                return resolution;
            }

            @Override
            public String toString() {
                return "AmbiguityResolver.Chain{" + Arrays.toString(ambiguityResolver) + '}';
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
        Resolution resolve(MethodDescription source, Binding left, Binding right);
    }

    /**
     * A helper class that allows to identify a best binding for a given type and source method chosing from a list of given
     * target methods by using a given {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder}
     * and an {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder.AmbiguityResolver}.
     * <p/>
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
        public Binding process(TypeDescription instrumentedType,
                               MethodDescription source,
                               Iterable<? extends MethodDescription> targets) {
            List<Binding> possibleDelegations = bind(instrumentedType, source, targets);
            if (possibleDelegations.size() == 0) {
                throw new IllegalArgumentException("No method can be bound to " + source);
            }
            return resolve(source, possibleDelegations);
        }

        private List<Binding> bind(TypeDescription instrumentedType,
                                   MethodDescription source,
                                   Iterable<? extends MethodDescription> targets) {
            List<Binding> possibleDelegations = new LinkedList<Binding>();
            for (MethodDescription target : targets) {
                Binding binding = methodDelegationBinder.bind(instrumentedType, source, target);
                if (binding.isValid()) {
                    possibleDelegations.add(binding);
                }
            }
            return possibleDelegations;
        }

        private Binding resolve(MethodDescription source,
                                List<Binding> targets) {
            switch (targets.size()) {
                case 1:
                    return targets.get(ONLY);
                case 2: {
                    Binding left = targets.get(LEFT);
                    Binding right = targets.get(RIGHT);
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
                    Binding left = targets.get(LEFT);
                    Binding right = targets.get(RIGHT);
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
                            Binding subResult = resolve(source, targets);
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
    Binding bind(TypeDescription instrumentedType, MethodDescription source, MethodDescription target);
}
