package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Implementations of this interface serve as resolvers for bridge methods. For the Java compiler, a method
 * signature does not include any information on a method's return type. However, within Java byte code, a
 * distinction is made such that the Java compiler needs to include bridge methods where the method with the more
 * specific return type is called from the method with the less specific return type when a method is
 * overridden to return a more specific value. This resolution is important when auxiliary types are called since
 * an accessor required for {@code Foo#qux} on some type {@code Bar} with an overridden method {@code qux} that was
 * defined with a more specific return type would call the bridge method internally and not the intended method.
 * This can be problematic if the following chain is the result of an instrumentation:
 * <ol>
 * <li>A {@code super} method accessor is registered for {@code Foo#qux} for some auxiliary type {@code Baz}.</li>
 * <li>The accessor is a bridging method which calls {@code Bar#qux} with the more specific return type.</li>
 * <li>The method {@code Bar#qux} is intercepted by an instrumentation.</li>
 * <li>Within the instrumented implementation, the auxiliary type {@code Baz} is used to invoke {@code Foo#qux}.</li>
 * <li>The {@code super} method invocation hits the bridge which delegates to the intercepted implementation what
 * results in endless recursion.</li>
 * </ol>
 */
public interface BridgeMethodResolver {

    /**
     * Resolves a method which is potentially a bridge method.
     *
     * @param methodDescription The method to resolve in cases it is a bridge method.
     * @return The intended invocation target of the given method.
     */
    MethodDescription resolve(MethodDescription methodDescription);

    /**
     * A no-op implementation of a {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver} which is simply
     * returning the method it is given to resolve.
     */
    enum NoOp implements BridgeMethodResolver, Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public BridgeMethodResolver make(MethodList relevant) {
            return this;
        }

        @Override
        public MethodDescription resolve(MethodDescription methodDescription) {
            return methodDescription;
        }

        @Override
        public String toString() {
            return "BridgeMethodResolver.NoOp." + name();
        }
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver} for a given list of
     * relevant methods that can be called in a given context.
     */
    interface Factory {

        /**
         * Creates a bridge method resolver for a given list of methods.
         *
         * @param methodList The relevant methods which can be called in a given context.
         * @return A bridge method resolver that reflects the given methods.
         */
        BridgeMethodResolver make(MethodList methodList);
    }

    /**
     * A simple bridge method resolver which applies its resolution by analyzing non-generic types. When a type
     * inherits from a generic type and additionally overloads this method, this resolution might be ambiguous.
     */
    class Simple implements BridgeMethodResolver {

        private final Map<MethodDescription.Token, BridgeTarget> bridges;

        /**
         * Creates a new simple bridge method resolver.
         *
         * @param bridges A map of all bridges mapped by their unique signature.
         */
        protected Simple(Map<MethodDescription.Token, BridgeTarget> bridges) {
            this.bridges = bridges;
        }

        /**
         * Creates a new bridge method resolver for the given list of methods.
         *
         * @param methodList      The relevant methods which can be called in a given context.
         * @param conflictHandler A conflict handler that is queried for handling ambiguous resolutions.
         * @return A corresponding bridge method resolver.
         */
        public static BridgeMethodResolver of(MethodList methodList, ConflictHandler conflictHandler) {
            MethodList bridgeMethods = methodList.filter(isBridge());
            HashMap<MethodDescription.Token, BridgeTarget> bridges = new HashMap<MethodDescription.Token, BridgeTarget>(bridgeMethods.size());
            for (MethodDescription bridgeMethod : bridgeMethods) {
                bridges.put(bridgeMethod.asToken(), findBridgeTargetFor(bridgeMethod, conflictHandler));
            }
            return new Simple(bridges);
        }

        /**
         * Attempts to find a bridge target for a given bridge method.
         *
         * @param bridgeMethod    The bridge method to resolve.
         * @param conflictHandler A conflict handler that is queried for handling ambiguous resolutions.
         * @return The resolved bridge method target.
         */
        private static BridgeTarget findBridgeTargetFor(MethodDescription bridgeMethod,
                                                        ConflictHandler conflictHandler) {
            MethodList targetCandidates = bridgeMethod.getDeclaringType()
                    .getDeclaredMethods()
                    .filter(not(isBridge()).and(isSpecializationOf(bridgeMethod)));
            switch (targetCandidates.size()) {
                case 0:
                    return new BridgeTarget.Resolved(bridgeMethod);
                case 1:
                    return new BridgeTarget.Candidate(targetCandidates.getOnly());
                default:
                    return conflictHandler.choose(bridgeMethod, targetCandidates);
            }
        }

        @Override
        public MethodDescription resolve(MethodDescription methodDescription) {
            BridgeTarget bridgeTarget = bridges.get(methodDescription.asToken());
            if (bridgeTarget == null) { // The given method is not a bridge method.
                return methodDescription;
            } else if (bridgeTarget.isResolved()) { // There is a definite target for the given bridge method.
                return bridgeTarget.extract();
            } else { // There is a target for the bridge method which might however itself be a bridge method.
                return resolve(bridgeTarget.extract());
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && bridges.equals(((Simple) other).bridges);
        }

        @Override
        public int hashCode() {
            return bridges.hashCode();
        }

        @Override
        public String toString() {
            return "BridgeMethodResolver.Simple{bridges=" + bridges + '}';
        }

        /**
         * A factory for creating {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Simple} instances
         * for any given default {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Simple.ConflictHandler}.
         */
        public enum Factory implements BridgeMethodResolver.Factory {

            /**
             * A factory for a {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Factory} that implements the
             * {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Simple.ConflictHandler.Default#FAIL_FAST}
             * strategy.
             */
            FAIL_FAST(ConflictHandler.Default.FAIL_FAST),

            /**
             * A factory for a {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Factory} that implements the
             * {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Simple.ConflictHandler.Default#FAIL_ON_REQUEST}
             * strategy.
             */
            FAIL_ON_REQUEST(ConflictHandler.Default.FAIL_ON_REQUEST),

            /**
             * A factory for a {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Factory} that implements the
             * {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Simple.ConflictHandler.Default#CALL_BRIDGE}
             * strategy.
             */
            CALL_BRIDGE(ConflictHandler.Default.CALL_BRIDGE);

            /**
             * This factory's conflict handler.
             */
            private final ConflictHandler conflictHandler;

            /**
             * Creates a new factory.
             *
             * @param conflictHandler The conflict handler for this factory.
             */
            Factory(ConflictHandler conflictHandler) {
                this.conflictHandler = conflictHandler;
            }

            @Override
            public BridgeMethodResolver make(MethodList methodList) {
                return Simple.of(methodList, conflictHandler);
            }

            @Override
            public String toString() {
                return "BridgeMethodResolver.Simple.Factory." + name();
            }
        }

        /**
         * A conflict handler is queried for resolving a bridge method with multiple possible target methods.
         */
        public interface ConflictHandler {

            /**
             * Returns a target method for the given bridge method out of the given list of candidate methods.
             * Alternatively, a runtime exception might be thrown if no unambiguous target method can be identified.
             *
             * @param bridgeMethod     The bridge method to resolve.
             * @param targetCandidates The list of possible candidates.
             * @return A representation of the target of the bridge method.
             */
            BridgeTarget choose(MethodDescription bridgeMethod, MethodList targetCandidates);

            /**
             * Default implementations of a
             * {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Simple.ConflictHandler}.
             */
            enum Default implements ConflictHandler {

                /**
                 * A strategy that fails immediately when an ambiguous resolution is discovered.
                 */
                FAIL_FAST {
                    @Override
                    public BridgeTarget choose(MethodDescription bridgeMethod, MethodList targetCandidates) {
                        throw new IllegalStateException("Could not resolve bridge method " + bridgeMethod
                                + " with multiple potential targets " + targetCandidates);
                    }
                },

                /**
                 * A strategy that fails when an ambiguous resolution is attempted to be used.
                 */
                FAIL_ON_REQUEST {
                    @Override
                    public BridgeTarget choose(MethodDescription bridgeMethod, MethodList targetCandidates) {
                        return BridgeTarget.Unknown.INSTANCE;
                    }
                },

                /**
                 * A strategy that calls the unresolved bridge method when its target resolution is ambiguous.
                 */
                CALL_BRIDGE {
                    @Override
                    public BridgeTarget choose(MethodDescription bridgeMethod, MethodList targetCandidates) {
                        return new BridgeTarget.Resolved(bridgeMethod);
                    }
                };

                @Override
                public String toString() {
                    return "BridgeMethodResolver.Simple.ConflictHandler.Default." + name();
                }
            }
        }

        /**
         * A target of a resolved bridge method which is created by a {@link net.bytebuddy.dynamic.scaffold.BridgeMethodResolver.Simple}.
         */
        public interface BridgeTarget {

            /**
             * Extracts the resolved bridge method target or throws an exception if no such target exists.
             *
             * @return The bridge method's target method.
             */
            MethodDescription extract();

            /**
             * Returns {@code true} if the bridge method resolution process was finalized.
             *
             * @return {@code true} if the bridge method resolution process was finalized.
             */
            boolean isResolved();

            /**
             * Represents a bridge method with an unknown target method.
             */
            enum Unknown implements BridgeTarget {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public MethodDescription extract() {
                    throw new IllegalStateException("Could not resolve bridge method target");
                }

                @Override
                public boolean isResolved() {
                    return true;
                }

                @Override
                public String toString() {
                    return "BridgeMethodResolver.Simple.BridgeTarget.Unknown." + name();
                }
            }

            /**
             * Represents a bridge method with an unambiguously resolved target method.
             */
            class Resolved implements BridgeTarget {

                /**
                 * The target method for this resolved bridge method.
                 */
                private final MethodDescription target;

                /**
                 * Creates a new resolved bridge method target.
                 *
                 * @param target The target method for a bridge method.
                 */
                public Resolved(MethodDescription target) {
                    this.target = target;
                }

                @Override
                public MethodDescription extract() {
                    return target;
                }

                @Override
                public boolean isResolved() {
                    return true;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && target.equals(((Resolved) other).target);
                }

                @Override
                public int hashCode() {
                    return target.hashCode();
                }

                @Override
                public String toString() {
                    return "BridgeMethodResolver.Simple.BridgeTarget.Resolved{target=" + target + '}';
                }
            }

            /**
             * Represents a bridge method with a possible candidate target method which might however be another
             * bridge method.
             */
            class Candidate implements BridgeTarget {

                /**
                 * The target method candidate for this resolved bridge method.
                 */
                private final MethodDescription target;

                /**
                 * Creates a new bridge method target candidate.
                 *
                 * @param target The target method candidate for a bridge method.
                 */
                public Candidate(MethodDescription target) {
                    this.target = target;
                }

                @Override
                public MethodDescription extract() {
                    return target;
                }

                @Override
                public boolean isResolved() {
                    return false;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && target.equals(((Candidate) other).target);
                }

                @Override
                public int hashCode() {
                    return target.hashCode();
                }

                @Override
                public String toString() {
                    return "BridgeMethodResolver.Simple.BridgeTarget.Candidate{target=" + target + '}';
                }
            }
        }
    }
}
