package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;

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
     * The origin type identifier to use.
     */
    protected final OriginTypeIdentifier originTypeIdentifier;

    /**
     * Creates a new subclass instrumentation target.
     *
     * @param finding                     The lookup of the instrumented type this instance should represent.
     * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver.
     * @param originTypeIdentifier        The origin type identifier to use.
     */
    protected SubclassInstrumentationTarget(MethodLookupEngine.Finding finding,
                                            BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                            OriginTypeIdentifier originTypeIdentifier) {
        super(finding, bridgeMethodResolverFactory);
        TypeDescription superType = finding.getTypeDescription().getSupertype();
        MethodList superConstructors = superType == null
                ? new MethodList.Empty()
                : superType.getDeclaredMethods().filter(isConstructor());
        this.superConstructors = new HashMap<TypeList, MethodDescription>(superConstructors.size());
        for (MethodDescription superConstructor : superConstructors) {
            this.superConstructors.put(superConstructor.getParameters().asTypeList(), superConstructor);
        }
        this.originTypeIdentifier = originTypeIdentifier;
    }

    @Override
    protected Instrumentation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        if (methodDescription.isConstructor()) {
            methodDescription = this.superConstructors.get(methodDescription.getParameters().asTypeList());
            if (methodDescription == null) {
                return Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE;
            }
        }
        return Instrumentation.SpecialMethodInvocation.Simple.of(methodDescription, typeDescription.getSupertype());
    }

    @Override
    public TypeDescription getOriginType() {
        return originTypeIdentifier.identify(typeDescription);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass()) && super.equals(other)
                && originTypeIdentifier == ((SubclassInstrumentationTarget) other).originTypeIdentifier;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + originTypeIdentifier.hashCode();
    }

    @Override
    public String toString() {
        return "SubclassInstrumentationTarget{" +
                "typeDescription=" + typeDescription +
                ", defaultMethods=" + defaultMethods +
                ", bridgeMethodResolver=" + bridgeMethodResolver +
                ", superConstructors=" + superConstructors +
                ", originTypeIdentifier=" + originTypeIdentifier +
                '}';
    }

    /**
     * Responsible for identifying the origin type that an instrumentation target represents when
     * {@link net.bytebuddy.instrumentation.Instrumentation.Target#getOriginType()} is invoked.
     */
    public static enum OriginTypeIdentifier {

        /**
         * Identifies the super type of an instrumented type as the origin type.
         */
        SUPER_TYPE {
            @Override
            protected TypeDescription identify(TypeDescription typeDescription) {
                return typeDescription.getSupertype();
            }
        },

        /**
         * Identifies the instrumented type as its own origin type.
         */
        LEVEL_TYPE {
            @Override
            protected TypeDescription identify(TypeDescription typeDescription) {
                return typeDescription;
            }
        };

        /**
         * Identifies the origin type to a given type description.
         *
         * @param typeDescription The type description for which an origin type should be identified.
         * @return The origin type to the given type description.
         */
        protected abstract TypeDescription identify(TypeDescription typeDescription);
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
         * The origin type identifier to use.
         */
        private final OriginTypeIdentifier originTypeIdentifier;

        /**
         * Creates a new factory for a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationTarget}.
         *
         * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver to be handed to the
         *                                    created subclass instrumentation target.
         * @param originTypeIdentifier        The origin type identifier to use.
         */
        public Factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                       OriginTypeIdentifier originTypeIdentifier) {
            this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
            this.originTypeIdentifier = originTypeIdentifier;
        }

        @Override
        public Instrumentation.Target make(MethodLookupEngine.Finding finding) {
            return new SubclassInstrumentationTarget(finding, bridgeMethodResolverFactory, originTypeIdentifier);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && bridgeMethodResolverFactory.equals(((Factory) other).bridgeMethodResolverFactory)
                    && originTypeIdentifier.equals(((Factory) other).originTypeIdentifier);
        }

        @Override
        public int hashCode() {
            return 31 * bridgeMethodResolverFactory.hashCode() + originTypeIdentifier.hashCode();
        }

        @Override
        public String toString() {
            return "SubclassInstrumentationTarget.Factory{" +
                    "bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                    "originTypeIdentifier=" + originTypeIdentifier +
                    '}';
        }
    }
}
