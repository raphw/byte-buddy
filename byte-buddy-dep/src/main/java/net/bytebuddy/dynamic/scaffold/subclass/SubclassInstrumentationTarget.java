package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeList;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isConstructor;

/**
 * An instrumentation target for creating a subclass of a given type.
 */
public class SubclassInstrumentationTarget extends Instrumentation.Target.AbstractBase {

    /**
     * The constructor of the super type, mapped by the constructor parameters of each constructor which is
     * sufficient for a constructor's unique identification.
     */
    protected final Map<TypeList, MethodDescription> superConstructors;

    /**
     * Creates a new subclass instrumentation target.
     *
     * @param finding                     The lookup of the instrumented type this instance should represent.
     * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver.
     */
    protected SubclassInstrumentationTarget(MethodLookupEngine.Finding finding,
                                            BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
        super(finding, bridgeMethodResolverFactory);
        MethodList superConstructors = finding.getTypeDescription().getSupertype().getDeclaredMethods().filter(isConstructor());
        this.superConstructors = new HashMap<TypeList, MethodDescription>(superConstructors.size());
        for (MethodDescription superConstructor : superConstructors) {
            this.superConstructors.put(superConstructor.getParameterTypes(), superConstructor);
        }
    }

    @Override
    protected Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        if (methodDescription.isConstructor()) {
            methodDescription = this.superConstructors.get(methodDescription.getParameterTypes());
            if (methodDescription == null) {
                return Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE;
            }
        }
        return Instrumentation.SpecialMethodInvocation.Simple.of(methodDescription, typeDescription.getSupertype());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass()) && super.equals(other);
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
        public Instrumentation.Target make(MethodLookupEngine.Finding finding) {
            return new SubclassInstrumentationTarget(finding, bridgeMethodResolverFactory);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && bridgeMethodResolverFactory.equals(((Factory) other).bridgeMethodResolverFactory);
        }

        @Override
        public int hashCode() {
            return 7 * bridgeMethodResolverFactory.hashCode();
        }

        @Override
        public String toString() {
            return "SubclassInstrumentationTarget.Factory{" +
                    "bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                    '}';
        }
    }
}
