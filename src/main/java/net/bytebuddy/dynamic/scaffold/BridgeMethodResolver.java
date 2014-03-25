package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

/**
 * Implementations of this interface serve as resolvers for bridge methods. For the Java compiler, a method
 * signature does not include any information on a method's return type. However, within Java byte code, a
 * distinction is made such that the Java compiler needs to include bridge methods where the method with the more
 * specific return type is called from the method with the less specific return type when a method is
 * overridden to return a more specific value. This resolution is important when auxiliary types are called since
 * an accessor required for {@code Foo#qux} on some type {@code Bar} with an overriden method {@code qux} that was
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

    static interface Factory {

        BridgeMethodResolver make(MethodList relevant);
    }

    static enum NoOp implements BridgeMethodResolver, Factory {
        INSTANCE;

        @Override
        public BridgeMethodResolver make(MethodList relevant) {
            return this;
        }

        @Override
        public MethodDescription resolve(MethodDescription methodDescription) {
            return methodDescription;
        }
    }

    static class Simple implements BridgeMethodResolver {

        public static enum Factory implements BridgeMethodResolver.Factory {

            FAIL_FAST(ConflictHandler.Default.FAIL_FAST),
            FAIL_ON_REQUEST(ConflictHandler.Default.FAIL_FAST),
            CALL_BRIDGE(ConflictHandler.Default.FAIL_FAST);

            private final ConflictHandler conflictHandler;

            private Factory(ConflictHandler conflictHandler) {
                this.conflictHandler = conflictHandler;
            }

            @Override
            public BridgeMethodResolver make(MethodList relevant) {
                return new Simple(relevant, conflictHandler);
            }
        }

        public static interface ConflictHandler {

            static enum Default implements ConflictHandler {

                FAIL_FAST,
                FAIL_ON_REQUEST,
                CALL_BRIDGE;

                @Override
                public BridgeTarget choose(MethodDescription bridgeMethod, MethodList targetCandidates) {
                    switch (this) {
                        case FAIL_FAST:
                            throw new IllegalStateException("Could not resolve bridge method " + bridgeMethod
                                    + " with multiple potential targets " + targetCandidates);
                        case FAIL_ON_REQUEST:
                            return BridgeTarget.Unknown.INSTANCE;
                        case CALL_BRIDGE:
                            return new BridgeTarget.Resolved(bridgeMethod);
                        default:
                            throw new AssertionError();
                    }
                }
            }

            BridgeTarget choose(MethodDescription bridgeMethod, MethodList targetCandidates);
        }

        public static interface BridgeTarget {

            static class Resolved implements BridgeTarget {

                private final MethodDescription target;

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
            }

            static class Candidate implements BridgeTarget {

                private final MethodDescription target;

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
            }

            static enum Unknown implements BridgeTarget {
                INSTANCE;

                @Override
                public MethodDescription extract() {
                    throw new IllegalStateException("Could not resolve bridge method target");
                }

                @Override
                public boolean isResolved() {
                    return true;
                }
            }

            MethodDescription extract();

            boolean isResolved();
        }

        private final Map<String, BridgeTarget> bridges;

        public Simple(MethodList relevant, ConflictHandler conflictHandler) {
            MethodList bridgeMethods = relevant.filter(isBridge());
            bridges = new HashMap<String, BridgeTarget>(bridgeMethods.size());
            for (MethodDescription bridgeMethod : bridgeMethods) {
                bridges.put(bridgeMethod.getUniqueSignature(), findBridgeTargetFor(bridgeMethod, conflictHandler));
            }
        }

        private static BridgeTarget findBridgeTargetFor(MethodDescription bridgeMethod,
                                                        ConflictHandler conflictHandler) {
            MethodList targetCandidates = bridgeMethod.getDeclaringType()
                    .getDeclaredMethods()
                    .filter(not(isBridge()).and(isBridgeMethodCompatibleTo(bridgeMethod)));
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
            BridgeTarget bridgeTarget = bridges.get(methodDescription.getUniqueSignature());
            if (bridgeTarget == null) { // The given method is not a bridge method.
                return methodDescription;
            } else if (bridgeTarget.isResolved()) { // There is a definite target for the given bridge method.
                return bridgeTarget.extract();
            } else { // There is a target for the bridge method which might however itself be a bridge method.
                return resolve(bridgeTarget.extract());
            }
        }
    }

    MethodDescription resolve(MethodDescription methodDescription);
}
