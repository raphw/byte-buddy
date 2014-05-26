package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;

public class SubclassInstrumentationTarget extends Instrumentation.Target.AbstractBase {

    public static class Factory implements Instrumentation.Target.Factory {

        private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

        public Factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
            this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
        }

        @Override
        public Instrumentation.Target make(MethodLookupEngine.Finding methodLookupEngineFinding) {
            return new SubclassInstrumentationTarget(methodLookupEngineFinding, bridgeMethodResolverFactory);
        }
    }

    public SubclassInstrumentationTarget(MethodLookupEngine.Finding finding,
                                         BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
        super(finding, bridgeMethodResolverFactory);
    }

    @Override
    protected Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        return methodDescription.isSpecializableFor(typeDescription.getSupertype())
                ? new Instrumentation.SpecialMethodInvocation.Legal(methodDescription, typeDescription.getSupertype())
                : Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE;
    }
}
