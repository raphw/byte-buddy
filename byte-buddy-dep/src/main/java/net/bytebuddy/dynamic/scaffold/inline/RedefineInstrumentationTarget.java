package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isOverridable;

/**
 * An instrumentation target for a redefinition of a given type. This instrumentation target is fairly similar to
 * a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget} as the original implementation of
 * any redefined method is simply discarded. However, as the method lookup does not preserve overridden methods,
 * the redefinition instrumentation target needs to apply an additional lookup on the super type in order to avoid
 * the failure of super method invocations of methods that are overriden on the redefined type. This lookup has a
 * minor performance cost over the {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget}.
 */
public class RedefineInstrumentationTarget extends SubclassInstrumentationTarget {

    /**
     * A mapping of unique method signatures of methods that are invokable on the super type to their description.
     */
    private final Map<String, MethodDescription> superTypeMethods;

    /**
     * Creates a redefine instrumentation target.
     *
     * @param finding                     The lookup of the instrumented type this instance should represent.
     * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver.
     * @param methodLookupEngine          A method lookup engine for analyzing the super type of the instrumented type.
     */
    protected RedefineInstrumentationTarget(MethodLookupEngine.Finding finding,
                                            BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                            MethodLookupEngine methodLookupEngine) {
        super(finding, bridgeMethodResolverFactory);
        MethodList overridableSuperMethods = methodLookupEngine.process(typeDescription.getSupertype())
                .getInvokableMethods().filter(isOverridable());
        superTypeMethods = new HashMap<String, MethodDescription>(overridableSuperMethods.size());
        for (MethodDescription methodDescription : overridableSuperMethods) {
            superTypeMethods.put(methodDescription.getUniqueSignature(), methodDescription);
        }
    }

    @Override
    protected Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        return methodDescription.getDeclaringType().equals(typeDescription)
                ? superTypeLookup(methodDescription.getUniqueSignature())
                : super.invokeSuper(methodDescription);
    }

    /**
     * Performs an explicit lookup on the super type for methods that are defined on the instrumented type
     * that is redefined.
     *
     * @param uniqueSignature The unique method signature.
     * @return A special method invocation of the
     */
    private Instrumentation.SpecialMethodInvocation superTypeLookup(String uniqueSignature) {
        MethodDescription superMethod = superTypeMethods.get(uniqueSignature);
        return superMethod == null
                ? Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE
                : Instrumentation.SpecialMethodInvocation.Simple.of(superMethod, typeDescription.getSupertype());
    }

    @Override
    public String toString() {
        return "RedefineInstrumentationTarget{" +
                "typeDescription=" + typeDescription +
                ", defaultMethods=" + defaultMethods +
                ", bridgeMethodResolver=" + bridgeMethodResolver +
                "superTypeMethods=" + superTypeMethods +
                '}';
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.inline.RedefineInstrumentationTarget}.
     */
    public static class Factory implements Instrumentation.Target.Factory {

        /**
         * A method lookup engine to use for analyzing the super type.
         */
        private final MethodLookupEngine methodLookupEngine;

        /**
         * A factory for creating a bridge method resolver to be handed to the created subclass instrumentation target.
         */
        private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

        /**
         * @param methodLookupEngine          A method lookup engine to use for analyzing the super type.
         * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver to be handed to the
         *                                    created subclass instrumentation target.
         */
        public Factory(MethodLookupEngine methodLookupEngine, BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
            this.methodLookupEngine = methodLookupEngine;
            this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
        }

        @Override
        public Instrumentation.Target make(MethodLookupEngine.Finding finding) {
            return new RedefineInstrumentationTarget(finding, bridgeMethodResolverFactory, methodLookupEngine);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && bridgeMethodResolverFactory.equals(((Factory) other).bridgeMethodResolverFactory)
                    && methodLookupEngine.equals(((Factory) other).methodLookupEngine);
        }

        @Override
        public int hashCode() {
            return 31 * methodLookupEngine.hashCode() + bridgeMethodResolverFactory.hashCode();
        }

        @Override
        public String toString() {
            return "RedefineInstrumentationTarget.Factory{" +
                    "methodLookupEngine=" + methodLookupEngine +
                    ", bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                    '}';
        }
    }
}
