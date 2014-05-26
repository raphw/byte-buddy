package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;

/**
 * An instrumentation target for creating a subclass of a given type.
 */
public class SubclassInstrumentationTarget extends Instrumentation.Target.AbstractBase {

    /**
     * Creates a new subclass instrumentation target.
     *
     * @param finding                     The lookup of the instrumented type this instance should represent.
     * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver.
     */
    protected SubclassInstrumentationTarget(MethodLookupEngine.Finding finding,
                                            BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
        super(finding, bridgeMethodResolverFactory);
    }

    @Override
    protected Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        return methodDescription.isSpecializableFor(typeDescription.getSupertype())
                ? new Instrumentation.SpecialMethodInvocation.Legal(methodDescription, typeDescription.getSupertype())
                : Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return 7 * super.hashCode();
    }

    @Override
    public String toString() {
        return "SubclassInstrumentationTarget{" +
                "typeDescription=" + typeDescription +
                ", defaultMethods=" + defaultMethods +
                ", bridgeMethodResolver=" + bridgeMethodResolver +
                '}';
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget}.
     */
    public static class Factory implements Instrumentation.Target.Factory {

        /**
         * A factory for creating a bridge method resolver to be handed to the created subclass instrumentation target.
         */
        private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

        /**
         * Creates a new factory for a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget}.
         *
         * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver to be handed to the
         *                                    created subclass instrumentation target.
         */
        public Factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
            this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
        }

        @Override
        public Instrumentation.Target make(MethodLookupEngine.Finding methodLookupEngineFinding) {
            return new SubclassInstrumentationTarget(methodLookupEngineFinding, bridgeMethodResolverFactory);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && bridgeMethodResolverFactory.equals(((Factory) other).bridgeMethodResolverFactory);
        }

        @Override
        public int hashCode() {
            return bridgeMethodResolverFactory.hashCode();
        }

        @Override
        public String toString() {
            return "SubclassInstrumentationTarget.Factory{" +
                    "bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                    '}';
        }
    }
}
