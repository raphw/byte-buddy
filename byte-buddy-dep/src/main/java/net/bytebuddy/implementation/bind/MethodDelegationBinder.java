/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.implementation.bind;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.MethodVisitor;

import java.io.PrintStream;
import java.util.*;

/**
 * A method delegation binder is responsible for creating a method binding for a <i>source method</i> to a
 * <i>target method</i>. Such a binding allows to implement the source method by calling the target method.
 * <p>&nbsp;</p>
 * Usually, an implementation will attempt to bind a specific source method to a set of target method candidates
 * where all legal bindings are considered for binding. To chose a specific candidate, an
 * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}
 * will be consulted for selecting a <i>best</i> binding.
 */
public interface MethodDelegationBinder {

    /**
     * Compiles this method delegation binder for a target method.
     *
     * @param candidate The target method to bind.
     * @return A compiled target for binding.
     */
    Record compile(MethodDescription candidate);

    /**
     * A method delegation that was compiled to a target method.
     */
    interface Record {

        /**
         * Attempts a binding of a source method to this compiled target.
         *
         * @param implementationTarget The target of the current implementation onto which this binding is to be applied.
         * @param source               The method that is to be bound to the {@code target} method.
         * @param terminationHandler   The termination handler to apply.
         * @param methodInvoker        The method invoker to use.
         * @param assigner             The assigner to use.
         * @return A binding representing this attempt to bind the {@code source} method to the {@code target} method.
         */
        MethodBinding bind(Implementation.Target implementationTarget,
                           MethodDescription source,
                           TerminationHandler terminationHandler,
                           MethodInvoker methodInvoker,
                           Assigner assigner);

        /**
         * A compiled method delegation binder that only yields illegal bindings.
         */
        enum Illegal implements Record {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public MethodBinding bind(Implementation.Target implementationTarget,
                                      MethodDescription source,
                                      TerminationHandler terminationHandler,
                                      MethodInvoker methodInvoker,
                                      Assigner assigner) {
                return MethodBinding.Illegal.INSTANCE;
            }
        }
    }

    /**
     * Implementations are used as delegates for invoking a method that was bound
     * using a {@link net.bytebuddy.implementation.bind.MethodDelegationBinder}.
     */
    interface MethodInvoker {

        /**
         * Creates a method invocation for a given method.
         *
         * @param methodDescription The method to be invoked.
         * @return A stack manipulation encapsulating this method invocation.
         */
        StackManipulation invoke(MethodDescription methodDescription);

        /**
         * A simple method invocation that merely uses the most general form of method invocation as provided by
         * {@link net.bytebuddy.implementation.bytecode.member.MethodInvocation}.
         */
        enum Simple implements MethodInvoker {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public StackManipulation invoke(MethodDescription methodDescription) {
                return MethodInvocation.invoke(methodDescription);
            }
        }

        /**
         * A method invocation that enforces a virtual invocation that is dispatched on a given type.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Virtual implements MethodInvoker {

            /**
             * The type on which a method should be invoked virtually.
             */
            private final TypeDescription typeDescription;

            /**
             * Creates an immutable method invoker that dispatches all methods on a given type.
             *
             * @param typeDescription The type on which the method is invoked by virtual invocation.
             */
            public Virtual(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation invoke(MethodDescription methodDescription) {
                return MethodInvocation.invoke(methodDescription).virtual(typeDescription);
            }
        }
    }

    /**
     * A binding attempt for a single parameter. Implementations of this type are a suggestion of composing a
     * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.MethodBinding}
     * by using a
     * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.MethodBinding.Builder}.
     * However, method bindings can also be composed without this type which is merely a suggestion.
     *
     * @param <T> The type of the identification token for this parameter binding.
     */
    interface ParameterBinding<T> extends StackManipulation {

        /**
         * Returns an identification token for this binding.
         *
         * @return An identification token unique to this binding.
         */
        T getIdentificationToken();

        /**
         * A singleton representation of an illegal binding for a method parameter. An illegal binding usually
         * suggests that a source method cannot be bound to a specific target method.
         */
        enum Illegal implements ParameterBinding<Void> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Void getIdentificationToken() {
                throw new IllegalStateException("An illegal binding does not define an identification token");
            }

            /**
             * {@inheritDoc}
             */
            public boolean isValid() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                throw new IllegalStateException("An illegal parameter binding must not be applied");
            }
        }

        /**
         * An anonymous binding of a target method parameter.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Anonymous implements ParameterBinding<Object> {

            /**
             * A pseudo-token that is not exposed and therefore anonymous.
             */
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
            private final Object anonymousToken;

            /**
             * The stack manipulation that represents the loading of the parameter binding onto the stack.
             */
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

            /**
             * {@inheritDoc}
             */
            public Object getIdentificationToken() {
                return anonymousToken;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isValid() {
                return delegate.isValid();
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                return delegate.apply(methodVisitor, implementationContext);
            }
        }

        /**
         * A uniquely identifiable parameter binding for a target method. Such bindings are usually later processed by
         * a {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}
         * in order to resolve binding conflicts between several bindable target methods to the same source method.
         *
         * @param <T> The type of the identification token.
         * @see net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Unique<T> implements ParameterBinding<T> {

            /**
             * The token that identifies this parameter binding as unique.
             */
            private final T identificationToken;

            /**
             * The stack manipulation that represents the loading of the parameter binding onto the stack.
             */
            private final StackManipulation delegate;

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

            /**
             * {@inheritDoc}
             */
            public T getIdentificationToken() {
                return identificationToken;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isValid() {
                return delegate.isValid();
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                return delegate.apply(methodVisitor, implementationContext);
            }
        }
    }

    /**
     * A binding attempt created by a
     * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder}.
     */
    interface MethodBinding extends StackManipulation {

        /**
         * Returns the target method's parameter index for a given parameter binding token.
         * <p>&nbsp;</p>
         * A binding token can be any object
         * that implements valid {@link Object#hashCode()} and {@link Object#equals(Object)} methods in order
         * to look up a given binding. This way, two bindings can be evaluated of having performed a similar type of
         * binding such that these bindings can be compared and a dominant binding can be identified by an
         * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}.
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

        /**
         * Representation of an attempt to bind a source method to a target method that is not applicable.
         *
         * @see net.bytebuddy.implementation.bind.MethodDelegationBinder
         */
        enum Illegal implements MethodBinding {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Integer getTargetParameterIndex(Object parameterBindingToken) {
                throw new IllegalStateException("Method is not bound");
            }

            /**
             * {@inheritDoc}
             */
            public MethodDescription getTarget() {
                throw new IllegalStateException("Method is not bound");
            }

            /**
             * {@inheritDoc}
             */
            public boolean isValid() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                throw new IllegalStateException("Cannot delegate to an unbound method");
            }
        }

        /**
         * A mutable builder that allows to compose a
         * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.MethodBinding}
         * by adding parameter bindings incrementally.
         */
        class Builder {

            /**
             * The method invoker for invoking the actual method that is bound.
             */
            private final MethodInvoker methodInvoker;

            /**
             * The target method that for which a binding is to be constructed by this builder..
             */
            private final MethodDescription candidate;

            /**
             * The current list of stack manipulations for loading values for each parameter onto the operand stack.
             */
            private final List<StackManipulation> parameterStackManipulations;

            /**
             * A mapping of identification tokens to the parameter index they were bound for.
             */
            private final LinkedHashMap<Object, Integer> registeredTargetIndices;

            /**
             * The index of the next parameter that is to be bound.
             */
            private int nextParameterIndex;

            /**
             * Creates a new builder for the binding of a given method.
             *
             * @param methodInvoker The method invoker that is used to create the method invocation of the {@code target} method.
             * @param candidate     The target method that is target of the binding.
             */
            public Builder(MethodInvoker methodInvoker, MethodDescription candidate) {
                this.methodInvoker = methodInvoker;
                this.candidate = candidate;
                parameterStackManipulations = new ArrayList<StackManipulation>(candidate.getParameters().size());
                registeredTargetIndices = new LinkedHashMap<Object, Integer>();
                nextParameterIndex = 0;
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
                return registeredTargetIndices.put(parameterBinding.getIdentificationToken(), nextParameterIndex++) == null;
            }

            /**
             * Creates a binding that represents the bindings collected by this {@code Builder}.
             *
             * @param terminatingManipulation A stack manipulation that is applied after the method invocation.
             * @return A binding representing the parameter bindings collected by this builder.
             */
            public MethodBinding build(StackManipulation terminatingManipulation) {
                if (candidate.getParameters().size() != nextParameterIndex) {
                    throw new IllegalStateException("The number of parameters bound does not equal the target's number of parameters");
                }
                return new Build(candidate,
                        registeredTargetIndices,
                        methodInvoker.invoke(candidate),
                        parameterStackManipulations,
                        terminatingManipulation);
            }

            /**
             * A method binding that was created by a
             * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.MethodBinding.Builder}.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Build implements MethodBinding {

                /**
                 * The target method this binding represents.
                 */
                private final MethodDescription target;

                /**
                 * A map of identification tokens to the indices of their binding parameters.
                 */
                private final Map<?, Integer> registeredTargetIndices;

                /**
                 * A stack manipulation that represents the actual method invocation.
                 */
                private final StackManipulation methodInvocation;

                /**
                 * A list of manipulations that each represent the loading of a parameter value onto the operand stack.
                 */
                private final List<StackManipulation> parameterStackManipulations;

                /**
                 * The stack manipulation that is applied after the method invocation.
                 */
                private final StackManipulation terminatingStackManipulation;

                /**
                 * Creates a new method binding.
                 *
                 * @param target                       The target method this binding represents.
                 * @param registeredTargetIndices      A map of identification tokens to the indices of their binding
                 *                                     parameters.
                 * @param methodInvocation             A stack manipulation that represents the actual method invocation.
                 * @param parameterStackManipulations  A list of manipulations that each represent the loading of a
                 *                                     parameter value onto the operand stack.
                 * @param terminatingStackManipulation The stack manipulation that is applied after the method invocation.
                 */
                protected Build(MethodDescription target,
                                Map<?, Integer> registeredTargetIndices,
                                StackManipulation methodInvocation,
                                List<StackManipulation> parameterStackManipulations,
                                StackManipulation terminatingStackManipulation) {
                    this.target = target;
                    this.registeredTargetIndices = new HashMap<Object, Integer>(registeredTargetIndices);
                    this.methodInvocation = methodInvocation;
                    this.parameterStackManipulations = new ArrayList<StackManipulation>(parameterStackManipulations);
                    this.terminatingStackManipulation = terminatingStackManipulation;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isValid() {
                    boolean result = methodInvocation.isValid() && terminatingStackManipulation.isValid();
                    Iterator<StackManipulation> assignment = parameterStackManipulations.iterator();
                    while (result && assignment.hasNext()) {
                        result = assignment.next().isValid();
                    }
                    return result;
                }

                /**
                 * {@inheritDoc}
                 */
                public Integer getTargetParameterIndex(Object parameterBindingToken) {
                    return registeredTargetIndices.get(parameterBindingToken);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDescription getTarget() {
                    return target;
                }

                /**
                 * {@inheritDoc}
                 */
                public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                    return new Compound(
                            CompoundList.of(parameterStackManipulations, Arrays.asList(methodInvocation, terminatingStackManipulation))
                    ).apply(methodVisitor, implementationContext);
                }
            }
        }
    }

    /**
     * A binding resolver is responsible to choose a method binding between several possible candidates.
     */
    interface BindingResolver {

        /**
         * Resolves a method binding for the {@code source} method.
         *
         * @param ambiguityResolver The ambiguity resolver to use.
         * @param source            The source method being bound.
         * @param targets           The possible target candidates. The list contains at least one element.
         * @return The method binding that was chosen.
         */
        MethodBinding resolve(AmbiguityResolver ambiguityResolver, MethodDescription source, List<MethodBinding> targets);

        /**
         * A default implementation of a binding resolver that fully relies on an {@link AmbiguityResolver}.
         */
        enum Default implements BindingResolver {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * Represents the index of the only value of two elements in a list.
             */
            private static final int ONLY = 0;

            /**
             * Represents the index of the left value of two elements in a list.
             */
            private static final int LEFT = 0;

            /**
             * Represents the index of the right value of two elements in a list.
             */
            private static final int RIGHT = 1;

            /**
             * {@inheritDoc}
             */
            public MethodBinding resolve(AmbiguityResolver ambiguityResolver, MethodDescription source, List<MethodBinding> targets) {
                return doResolve(ambiguityResolver, source, new ArrayList<MethodBinding>(targets));
            }

            /**
             * Resolves a method binding for the {@code source} method.
             *
             * @param ambiguityResolver The ambiguity resolver to use.
             * @param source            The source method being bound.
             * @param targets           The possible target candidates. The list contains at least one element and is mutable.
             * @return The method binding that was chosen.
             */
            private MethodBinding doResolve(AmbiguityResolver ambiguityResolver, MethodDescription source, List<MethodBinding> targets) {
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
                                throw new IllegalArgumentException("Cannot resolve ambiguous delegation of " + source + " to " + left + " or " + right);
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
                                return doResolve(ambiguityResolver, source, targets);
                            case RIGHT:
                                targets.remove(LEFT);
                                return doResolve(ambiguityResolver, source, targets);
                            case AMBIGUOUS:
                            case UNKNOWN:
                                targets.remove(RIGHT); // Remove right element first due to index alteration!
                                targets.remove(LEFT);
                                MethodBinding subResult = doResolve(ambiguityResolver, source, targets);
                                switch (ambiguityResolver.resolve(source, left, subResult).merge(ambiguityResolver.resolve(source, right, subResult))) {
                                    case RIGHT:
                                        return subResult;
                                    case LEFT:
                                    case AMBIGUOUS:
                                    case UNKNOWN:
                                        throw new IllegalArgumentException("Cannot resolve ambiguous delegation of " + source + " to " + left + " or " + right);
                                    default:
                                        throw new AssertionError();
                                }
                            default:
                                throw new IllegalStateException("Unexpected amount of targets: " + targets);
                        }
                    }
                }
            }
        }

        /**
         * A binding resolver that only binds a method if it has a unique binding.
         */
        enum Unique implements BindingResolver {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * Indicates the first index of a list only containing one element.
             */
            private static final int ONLY = 0;

            /**
             * {@inheritDoc}
             */
            public MethodBinding resolve(AmbiguityResolver ambiguityResolver, MethodDescription source, List<MethodBinding> targets) {
                if (targets.size() == 1) {
                    return targets.get(ONLY);
                } else {
                    throw new IllegalStateException(source + " allowed for more than one binding: " + targets);
                }
            }
        }

        /**
         * Binds a method using another resolver and prints the selected binding to a {@link PrintStream}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class StreamWriting implements BindingResolver {

            /**
             * The delegate binding resolver.
             */
            private final BindingResolver delegate;

            /**
             * The print stream to bind write the chosen binding to.
             */
            private final PrintStream printStream;

            /**
             * Creates a new stream writing binding resolver.
             *
             * @param delegate    The delegate binding resolver.
             * @param printStream The print stream to bind write the chosen binding to.
             */
            public StreamWriting(BindingResolver delegate, PrintStream printStream) {
                this.delegate = delegate;
                this.printStream = printStream;
            }

            /**
             * Creates a binding resolver that writes results to {@link System#out} and delegates to the {@link Default} resolver.
             *
             * @return An appropriate binding resolver.
             */
            public static BindingResolver toSystemOut() {
                return toSystemOut(Default.INSTANCE);
            }

            /**
             * Creates a binding resolver that writes results to {@link System#out} and delegates to the {@link Default} resolver.
             *
             * @param bindingResolver The delegate binding resolver.
             * @return An appropriate binding resolver.
             */
            public static BindingResolver toSystemOut(BindingResolver bindingResolver) {
                return new StreamWriting(bindingResolver, System.out);
            }

            /**
             * Creates a binding resolver that writes results to {@link System#err} and delegates to the {@link Default} resolver.
             *
             * @return An appropriate binding resolver.
             */
            public static BindingResolver toSystemError() {
                return toSystemError(Default.INSTANCE);
            }

            /**
             * Creates a binding resolver that writes results to {@link System#err}.
             *
             * @param bindingResolver The delegate binding resolver.
             * @return An appropriate binding resolver.
             */
            public static BindingResolver toSystemError(BindingResolver bindingResolver) {
                return new StreamWriting(bindingResolver, System.err);
            }

            /**
             * {@inheritDoc}
             */
            public MethodBinding resolve(AmbiguityResolver ambiguityResolver, MethodDescription source, List<MethodBinding> targets) {
                MethodBinding methodBinding = delegate.resolve(ambiguityResolver, source, targets);
                printStream.println("Binding " + source + " as delegation to " + methodBinding.getTarget());
                return methodBinding;
            }
        }
    }

    /**
     * Implementations of this interface are able to attempt the resolution of two successful bindings of a method
     * to two different target methods in order to identify a dominating binding.
     */
    @SuppressFBWarnings(value = "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", justification = "Safe initialization is implied")
    interface AmbiguityResolver {

        /**
         * The default ambiguity resolver to use.
         */
        AmbiguityResolver DEFAULT = new MethodDelegationBinder.AmbiguityResolver.Compound(BindingPriority.Resolver.INSTANCE,
                DeclaringTypeResolver.INSTANCE,
                ArgumentTypeResolver.INSTANCE,
                MethodNameEqualityResolver.INSTANCE,
                ParameterLengthResolver.INSTANCE);

        /**
         * Attempts to resolve to conflicting bindings.
         *
         * @param source The source method that was bound to both target methods.
         * @param left   The first successful binding of the {@code source} method.
         * @param right  The second successful binding of the {@code source} method.
         * @return The resolution state when resolving a conflicting binding where
         * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver.Resolution#LEFT}
         * indicates a successful binding to the {@code left} binding while
         * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver.Resolution#RIGHT}
         * indicates a successful binding to the {@code right} binding.
         */
        Resolution resolve(MethodDescription source, MethodBinding left, MethodBinding right);

        /**
         * A resolution state of an attempt to resolve two conflicting bindings.
         */
        enum Resolution {

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

            /**
             * {@code true} if this resolution is unresolved.
             */
            private final boolean unresolved;

            /**
             * Creates a new resolution.
             *
             * @param unresolved {@code true} if this resolution is unresolved.
             */
            Resolution(boolean unresolved) {
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
                        return other == UNKNOWN || other == this
                                ? this
                                : AMBIGUOUS;
                    default:
                        throw new AssertionError();
                }
            }
        }

        /**
         * An ambiguity resolver that does not attempt to resolve a conflicting binding.
         */
        enum NoOp implements AmbiguityResolver {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Resolution resolve(MethodDescription source, MethodBinding left, MethodBinding right) {
                return Resolution.UNKNOWN;
            }
        }

        /**
         * An ambiguity resolver that always resolves in the specified direction.
         */
        enum Directional implements AmbiguityResolver {

            /**
             * A resolver that always resolves to
             * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver.Resolution#LEFT}.
             */
            LEFT(true),

            /**
             * A resolver that always resolves to
             * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver.Resolution#RIGHT}.
             */
            RIGHT(false);

            /**
             * {@code true} if this instance should resolve to the left side.
             */
            private final boolean left;

            /**
             * Creates a new directional resolver.
             *
             * @param left {@code true} if this instance should resolve to the left side.
             */
            Directional(boolean left) {
                this.left = left;
            }

            /**
             * {@inheritDoc}
             */
            public Resolution resolve(MethodDescription source, MethodBinding left, MethodBinding right) {
                return this.left
                        ? Resolution.LEFT
                        : Resolution.RIGHT;
            }
        }

        /**
         * A chain of {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}s
         * that are applied in the given order until two bindings can be resolved.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Compound implements AmbiguityResolver {

            /**
             * A list of ambiguity resolvers that are applied by this chain in their order of application.
             */
            private final List<AmbiguityResolver> ambiguityResolvers;

            /**
             * Creates an immutable chain of ambiguity resolvers.
             *
             * @param ambiguityResolver The ambiguity resolvers to chain in the order of their application.
             */
            public Compound(AmbiguityResolver... ambiguityResolver) {
                this(Arrays.asList(ambiguityResolver));
            }

            /**
             * Creates an immutable chain of ambiguity resolvers.
             *
             * @param ambiguityResolvers The ambiguity resolvers to chain in the order of their application.
             */
            public Compound(List<? extends AmbiguityResolver> ambiguityResolvers) {
                this.ambiguityResolvers = new ArrayList<AmbiguityResolver>();
                for (AmbiguityResolver ambiguityResolver : ambiguityResolvers) {
                    if (ambiguityResolver instanceof Compound) {
                        this.ambiguityResolvers.addAll(((Compound) ambiguityResolver).ambiguityResolvers);
                    } else if (!(ambiguityResolver instanceof NoOp)) {
                        this.ambiguityResolvers.add(ambiguityResolver);
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public Resolution resolve(MethodDescription source, MethodBinding left, MethodBinding right) {
                Resolution resolution = Resolution.UNKNOWN;
                Iterator<? extends AmbiguityResolver> iterator = ambiguityResolvers.iterator();
                while (resolution.isUnresolved() && iterator.hasNext()) {
                    resolution = iterator.next().resolve(source, left, right);
                }
                return resolution;
            }
        }
    }

    /**
     * A termination handler is responsible for terminating a method delegation.
     */
    interface TerminationHandler {

        /**
         * Creates a stack manipulation that is to be applied after the method return.
         *
         * @param assigner The supplied assigner.
         * @param typing   The typing to apply.
         * @param source   The source method that is bound to the {@code target} method.
         * @param target   The target method that is subject to be bound by the {@code source} method.
         * @return A stack manipulation that is applied after the method return.
         */
        StackManipulation resolve(Assigner assigner, Assigner.Typing typing, MethodDescription source, MethodDescription target);

        /**
         * Responsible for creating a {@link StackManipulation}
         * that is applied after the interception method is applied.
         */
        enum Default implements TerminationHandler {

            /**
             * A termination handler that returns the delegate method's return value.
             */
            RETURNING {
                /** {@inheritDoc} */
                public StackManipulation resolve(Assigner assigner, Assigner.Typing typing, MethodDescription source, MethodDescription target) {
                    return new StackManipulation.Compound(assigner.assign(target.isConstructor()
                                    ? target.getDeclaringType().asGenericType()
                                    : target.getReturnType(),
                            source.getReturnType(),
                            typing), MethodReturn.of(source.getReturnType()));
                }
            },

            /**
             * A termination handler that drops the delegate method's return value.
             */
            DROPPING {
                /** {@inheritDoc} */
                public StackManipulation resolve(Assigner assigner, Assigner.Typing typing, MethodDescription source, MethodDescription target) {
                    return Removal.of(target.isConstructor()
                            ? target.getDeclaringType()
                            : target.getReturnType());
                }
            };
        }
    }

    /**
     * A helper class that allows to identify a best binding for a given type and source method choosing from a list of given
     * target methods by using a given {@link net.bytebuddy.implementation.bind.MethodDelegationBinder}
     * and an {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}.
     * <p>&nbsp;</p>
     * The {@code Processor} will:
     * <ol>
     * <li>Try to bind the {@code source} method using the {@code MethodDelegationBinder}.</li>
     * <li>Find a best method among the successful bindings using the {@code AmbiguityResolver}.</li>
     * </ol>
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Processor implements MethodDelegationBinder.Record {

        /**
         * The delegation records to consider.
         */
        private final List<? extends Record> records;

        /**
         * The processor's ambiguity resolver.
         */
        private final AmbiguityResolver ambiguityResolver;

        /**
         * The binding resolver being used to select the relevant method binding.
         */
        private final BindingResolver bindingResolver;

        /**
         * Creates a new processor.
         *
         * @param records           The delegation records to consider.
         * @param ambiguityResolver The ambiguity resolver to apply.
         * @param bindingResolver   The binding resolver being used to select the relevant method binding.
         */
        public Processor(List<? extends Record> records, AmbiguityResolver ambiguityResolver, BindingResolver bindingResolver) {
            this.records = records;
            this.ambiguityResolver = ambiguityResolver;
            this.bindingResolver = bindingResolver;
        }

        /**
         * {@inheritDoc}
         */
        public MethodBinding bind(Implementation.Target implementationTarget,
                                  MethodDescription source,
                                  TerminationHandler terminationHandler,
                                  MethodInvoker methodInvoker,
                                  Assigner assigner) {
            List<MethodBinding> targets = new ArrayList<MethodBinding>();
            for (Record record : records) {
                MethodBinding methodBinding = record.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
                if (methodBinding.isValid()) {
                    targets.add(methodBinding);
                }
            }
            if (targets.isEmpty()) {
                throw new IllegalArgumentException("None of " + records + " allows for delegation from " + source);
            }
            return bindingResolver.resolve(ambiguityResolver, source, targets);
        }
    }
}
