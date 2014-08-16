package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

public class RebaseInstrumentationTarget extends SubclassInstrumentationTarget {

    private final List<MethodDescription> redefinedMethods;

    private final List<MethodDescription> ignoredMethods;

    protected RebaseInstrumentationTarget(MethodLookupEngine.Finding finding,
                                          BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                          MethodMatcher ignoredMethods) {
        super(finding, bridgeMethodResolverFactory);
        List<MethodDescription> declaredMethods = finding.getTypeDescription().getDeclaredMethods();
        redefinedMethods = new ArrayList<MethodDescription>(declaredMethods.size());
        this.ignoredMethods = new ArrayList<MethodDescription>(declaredMethods.size());
        for (MethodDescription methodDescription : declaredMethods) {
            if (ignoredMethods.matches(methodDescription)) {
                redefinedMethods.add(methodDescription);
            } else {
                this.ignoredMethods.add(methodDescription);
            }
        }
        // Check same level methods, use original type.
        // Watch out for non-synchron class file (wrong class file)
    }

    @Override
    protected Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        // Super call if no "same level method" is available.
        return null;
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.inline.RebaseInstrumentationTarget}.
     */
    public static class Factory implements Instrumentation.Target.Factory {

        /**
         * A factory for creating a bridge method resolver to be handed to the created rebase instrumentation target.
         */
        private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

        /**
         * Creates a new factory for a {@link net.bytebuddy.dynamic.scaffold.inline.RebaseInstrumentationTarget}.
         *
         * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver to be handed to the
         *                                    created subclass instrumentation target.
         */
        public Factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
            this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
        }

        @Override
        public Instrumentation.Target make(MethodLookupEngine.Finding methodLookupEngineFinding) {
            return new RebaseInstrumentationTarget(methodLookupEngineFinding, bridgeMethodResolverFactory, null); // TODO
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && bridgeMethodResolverFactory.equals(((Factory) other).bridgeMethodResolverFactory);
        }

        @Override
        public int hashCode() {
            return 11 * bridgeMethodResolverFactory.hashCode();
        }

        @Override
        public String toString() {
            return "RebaseInstrumentationTarget.Factory{" +
                    "bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                    '}';
        }
    }
}
