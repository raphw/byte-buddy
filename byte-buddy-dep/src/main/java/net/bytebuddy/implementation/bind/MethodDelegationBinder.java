package net.bytebuddy.implementation.bind;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;

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
     * @param target The target method to bind.
     * @return A compiled target for binding.
     */
    Compiled compile(MethodDescription target);

    /**
     * A method delegation that was compiled to a target method.
     */
    interface Compiled {

        /**
         * Attempts a binding of a source method to this compiled target.
         *
         * @param implementationTarget The target of the current implementation onto which this binding is to be applied.
         * @param source               The method that is to be bound to the {@code target} method.
         * @return A binding representing this attempt to bind the {@code source} method to the {@code target} method.
         */
        MethodBinding bind(Implementation.Target implementationTarget, MethodDescription source);

        /**
         * A compiled method delegation binder that only yields illegal bindings.
         */
        enum Ignored implements Compiled {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public MethodBinding bind(Implementation.Target implementationTarget, MethodDescription source) {
                return MethodBinding.Illegal.INSTANCE;
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.Compiled.Ignored." + name();
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

            @Override
            public StackManipulation invoke(MethodDescription methodDescription) {
                return MethodInvocation.invoke(methodDescription);
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.MethodInvoker.Simple." + name();
            }
        }

        /**
         * A method invocation that enforces a virtual invocation that is dispatched on a given type.
         */
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

            @Override
            public StackManipulation invoke(MethodDescription methodDescription) {
                return MethodInvocation.invoke(methodDescription).virtual(typeDescription);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && typeDescription.equals(((Virtual) other).typeDescription);
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.MethodInvoker.Virtual{typeDescription=" + typeDescription + '}';
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

            @Override
            public Void getIdentificationToken() {
                throw new IllegalStateException("An illegal binding does not define an identification token");
            }

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                throw new IllegalStateException("An illegal parameter binding must not be applied");
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.ParameterBinding.Illegal." + name();
            }
        }

        /**
         * An anonymous binding of a target method parameter.
         */
        class Anonymous implements ParameterBinding<Object> {

            /**
             * A pseudo-token that is not exposed and therefore anonymous.
             */
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

            @Override
            public Object getIdentificationToken() {
                return anonymousToken;
            }

            @Override
            public boolean isValid() {
                return delegate.isValid();
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                return delegate.apply(methodVisitor, implementationContext);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && delegate.equals(((Anonymous) other).delegate);
            }

            @Override
            public int hashCode() {
                return 31 * delegate.hashCode();
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.ParameterBinding.Anonymous{" +
                        "anonymousToken=" + anonymousToken +
                        ", delegate=" + delegate +
                        '}';
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

            @Override
            public T getIdentificationToken() {
                return identificationToken;
            }

            @Override
            public boolean isValid() {
                return delegate.isValid();
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                return delegate.apply(methodVisitor, implementationContext);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Unique<?> unique = (Unique<?>) other;
                return identificationToken.equals(unique.identificationToken) && delegate.equals(unique.delegate);
            }

            @Override
            public int hashCode() {
                int result = identificationToken.hashCode();
                result = 31 * result + delegate.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.ParameterBinding.Unique{" +
                        "identificationToken=" + identificationToken +
                        ", delegate=" + delegate +
                        '}';
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

            @Override
            public Integer getTargetParameterIndex(Object parameterBindingToken) {
                throw new IllegalStateException("Method is not bound");
            }

            @Override
            public MethodDescription getTarget() {
                throw new IllegalStateException("Method is not bound");
            }

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                throw new IllegalStateException("Cannot delegate to an unbound method");
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.MethodBinding.Illegal." + name();
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
            private final MethodDescription target;

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
             * @param methodInvoker The method invoker that is used to create the method invocation of the {@code target}
             *                      method.
             * @param target        The target method that is target of the binding.
             */
            public Builder(MethodInvoker methodInvoker, MethodDescription target) {
                this.methodInvoker = methodInvoker;
                this.target = target;
                parameterStackManipulations = new ArrayList<StackManipulation>(target.getParameters().size());
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
                if (target.getParameters().size() != nextParameterIndex) {
                    throw new IllegalStateException("The number of parameters bound does not equal the target's number of parameters");
                }
                return new Build(target,
                        registeredTargetIndices,
                        methodInvoker.invoke(target),
                        parameterStackManipulations,
                        terminatingManipulation);
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.MethodBinding.Builder{" +
                        "methodInvoker=" + methodInvoker +
                        ", target=" + target +
                        ", parameterStackManipulations=" + parameterStackManipulations +
                        ", registeredTargetIndices=" + registeredTargetIndices +
                        ", nextParameterIndex=" + nextParameterIndex +
                        '}';
            }

            /**
             * A method binding that was created by a
             * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.MethodBinding.Builder}.
             */
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

                @Override
                public boolean isValid() {
                    boolean result = methodInvocation.isValid() && terminatingStackManipulation.isValid();
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
                public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                    Size size = new Size(0, 0);
                    for (StackManipulation stackManipulation : parameterStackManipulations) {
                        size = size.aggregate(stackManipulation.apply(methodVisitor, implementationContext));
                    }
                    size = size.aggregate(methodInvocation.apply(methodVisitor, implementationContext));
                    return size.aggregate(terminatingStackManipulation.apply(methodVisitor, implementationContext));
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Build build = (Build) other;
                    return methodInvocation.equals(build.methodInvocation)
                            && parameterStackManipulations.equals(build.parameterStackManipulations)
                            && registeredTargetIndices.equals(build.registeredTargetIndices)
                            && terminatingStackManipulation.equals(build.terminatingStackManipulation)
                            && target.equals(build.target);
                }

                @Override
                public int hashCode() {
                    int result = target.hashCode();
                    result = 31 * result + registeredTargetIndices.hashCode();
                    result = 31 * result + methodInvocation.hashCode();
                    result = 31 * result + parameterStackManipulations.hashCode();
                    result = 31 * result + terminatingStackManipulation.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "MethodDelegationBinder.MethodBinding.Builder.Build{" +
                            "target=" + target +
                            ", registeredTargetIndices=" + registeredTargetIndices +
                            ", methodInvocation=" + methodInvocation +
                            ", parameterStackManipulations=" + parameterStackManipulations +
                            ", terminatingStackManipulation=" + terminatingStackManipulation +
                            '}';
                }
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
        AmbiguityResolver DEFAULT = new MethodDelegationBinder.AmbiguityResolver.Chain(BindingPriority.Resolver.INSTANCE,
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
                        return other == this ? this : AMBIGUOUS;
                    default:
                        throw new AssertionError();
                }
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.AmbiguityResolver.Resolution." + name();
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

            @Override
            public Resolution resolve(MethodDescription source, MethodBinding left, MethodBinding right) {
                return Resolution.UNKNOWN;
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.AmbiguityResolver.NoOp." + name();
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

            @Override
            public Resolution resolve(MethodDescription source, MethodBinding left, MethodBinding right) {
                return this.left
                        ? Resolution.LEFT
                        : Resolution.RIGHT;
            }

            @Override
            public String toString() {
                return "MethodDelegationBinder.AmbiguityResolver.Directional." + name();
            }
        }

        /**
         * A chain of {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}s
         * that are applied in the given order until two bindings can be resolved.
         */
        class Chain implements AmbiguityResolver {

            /**
             * A list of ambiguity resolvers that are applied by this chain in their order of application.
             */
            private final List<? extends AmbiguityResolver> ambiguityResolvers;

            /**
             * Creates an immutable chain of ambiguity resolvers.
             *
             * @param ambiguityResolver The ambiguity resolvers to chain in the order of their application.
             */
            public Chain(AmbiguityResolver... ambiguityResolver) {
                this(Arrays.asList(ambiguityResolver));
            }

            /**
             * Creates an immutable chain of ambiguity resolvers.
             *
             * @param ambiguityResolvers The ambiguity resolvers to chain in the order of their application.
             */
            public Chain(List<? extends AmbiguityResolver> ambiguityResolvers) {
                this.ambiguityResolvers = ambiguityResolvers;
            }

            @Override
            public Resolution resolve(MethodDescription source, MethodBinding left, MethodBinding right) {
                Resolution resolution = Resolution.UNKNOWN;
                Iterator<? extends AmbiguityResolver> iterator = ambiguityResolvers.iterator();
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
                return "MethodDelegationBinder.AmbiguityResolver.Chain{ambiguityResolvers=" + ambiguityResolvers + '}';
            }
        }
    }

    /**
     * A helper class that allows to identify a best binding for a given type and source method chosing from a list of given
     * target methods by using a given {@link net.bytebuddy.implementation.bind.MethodDelegationBinder}
     * and an {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}.
     * <p>&nbsp;</p>
     * The {@code Processor} will:
     * <ol>
     * <li>Try to bind the {@code source} method using the {@code MethodDelegationBinder}.</li>
     * <li>Find a best method among the successful bindings using the {@code AmbiguityResolver}.</li>
     * </ol>
     */
    class Processor {

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
         * This processor's method delegation binder.
         */
        private final MethodDelegationBinder methodDelegationBinder;

        /**
         * The processor's ambiguity resolver.
         */
        private final AmbiguityResolver ambiguityResolver;

        /**
         * Creates a new processor for a method delegation binder.
         *
         * @param methodDelegationBinder This processor's method delegation binder.
         * @param ambiguityResolver      The processor's ambiguity resolver.
         */
        public Processor(MethodDelegationBinder methodDelegationBinder, AmbiguityResolver ambiguityResolver) {
            this.methodDelegationBinder = methodDelegationBinder;
            this.ambiguityResolver = ambiguityResolver;
        }

        /**
         * @param implementationTarget The implementation target for binding the {@code source} method to.
         * @param source               The source method that is to be bound.
         * @param targetCandidates     All possible targets for the delegation binding that are to be considered.
         * @return The best binding that was identified. If no such binding can be identified, an exception is thrown.
         */
        public MethodBinding process(Implementation.Target implementationTarget, MethodDescription source, MethodList<?> targetCandidates) {
            List<MethodBinding> possibleDelegations = bind(implementationTarget, source, targetCandidates);
            if (possibleDelegations.isEmpty()) {
                throw new IllegalArgumentException("None of " + targetCandidates + " allows for delegation from " + source);
            }
            return resolve(source, possibleDelegations);
        }

        /**
         * Creates a list of method bindings for any legal target method.
         *
         * @param implementationTarget The implementation target for binding the {@code source} method to.
         * @param source               The method that is to be bound to any {@code targets} method.
         * @param targetCandidates     All possible targets for the delegation binding that are to be considered.
         * @return A list of valid method bindings representing a subset of the given target methods.
         */
        private List<MethodBinding> bind(Implementation.Target implementationTarget, MethodDescription source, MethodList<?> targetCandidates) {
            List<MethodBinding> possibleDelegations = new ArrayList<MethodBinding>();
            for (MethodDescription targetCandidate : targetCandidates.filter(isVisibleTo(implementationTarget.getInstrumentedType()))) {
                MethodBinding methodBinding = methodDelegationBinder.compile(targetCandidate).bind(implementationTarget, source);
                if (methodBinding.isValid()) {
                    possibleDelegations.add(methodBinding);
                }
            }
            return possibleDelegations;
        }

        /**
         * Resolves the most specific target method of a list of legal method bindings.
         *
         * @param source  The source method that is to be bound.
         * @param targets A list of possible binding targets.
         * @return The most specific method binding that was located from the given list of candidate targets.
         */
        private MethodBinding resolve(MethodDescription source, List<MethodBinding> targets) {
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
                                    throw new IllegalArgumentException("Cannot resolve ambiguous delegation of " + source + " to " + left + " or " + right);
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
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && ambiguityResolver.equals(((Processor) other).ambiguityResolver)
                    && methodDelegationBinder.equals(((Processor) other).methodDelegationBinder);
        }

        @Override
        public int hashCode() {
            return 31 * methodDelegationBinder.hashCode() + ambiguityResolver.hashCode();
        }

        @Override
        public String toString() {
            return "MethodDelegationBinder.Processor{"
                    + "methodDelegationBinder=" + methodDelegationBinder
                    + ", ambiguityResolver=" + ambiguityResolver + '}';
        }
    }
}
