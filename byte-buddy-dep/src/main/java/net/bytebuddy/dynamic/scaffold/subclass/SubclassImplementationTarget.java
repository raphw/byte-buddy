package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.MethodLookupEngine;
import net.bytebuddy.implementation.Implementation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;

/**
 * An implementation target for creating a subclass of a given type.
 */
public class SubclassImplementationTarget extends Implementation.Target.AbstractBase {

    /**
     * The constructor of the super type, mapped by the constructor's method token.
     */
    protected final Map<MethodDescription.Token, MethodDescription> superConstructors;

    /**
     * The origin type identifier to use.
     */
    protected final OriginTypeIdentifier originTypeIdentifier;

    /**
     * Creates a new subclass implementation target.
     *
     * @param finding                     The lookup of the instrumented type this instance should represent.
     * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver.
     * @param originTypeIdentifier        The origin type identifier to use.
     */
    protected SubclassImplementationTarget(MethodLookupEngine.Finding finding,
                                           BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                                           OriginTypeIdentifier originTypeIdentifier) {
        super(finding, bridgeMethodResolverFactory);
        GenericTypeDescription superType = finding.getTypeDescription().getSuperType();
        MethodList<?> superConstructors = superType == null
                ? new MethodList.Empty()
                : superType.asRawType().getDeclaredMethods().filter(isConstructor());
        this.superConstructors = new HashMap<MethodDescription.Token, MethodDescription>(superConstructors.size());
        for (MethodDescription superConstructor : superConstructors) {
            this.superConstructors.put(superConstructor.asToken(), superConstructor);
        }
        this.originTypeIdentifier = originTypeIdentifier;
    }

    @Override
    public Implementation.SpecialMethodInvocation invokeSuper(MethodDescription.Token methodToken) {
        MethodDescription methodDescription = superConstructors.get(methodToken);
        return methodDescription == null
                ? super.invokeSuper(methodToken)
                : invokeSuper(methodDescription);
    }

    @Override
    protected Implementation.SpecialMethodInvocation invokeSuper(MethodDescription methodDescription) {
        return Implementation.SpecialMethodInvocation.Simple.of(methodDescription, typeDescription.getSuperType().asRawType());
    }

    @Override
    public TypeDescription getOriginType() {
        return originTypeIdentifier.identify(typeDescription);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass()) && super.equals(other)
                && originTypeIdentifier == ((SubclassImplementationTarget) other).originTypeIdentifier;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + originTypeIdentifier.hashCode();
    }

    @Override
    public String toString() {
        return "SubclassImplementationTarget{" +
                "typeDescription=" + typeDescription +
                ", defaultMethods=" + defaultMethods +
                ", bridgeMethodResolver=" + bridgeMethodResolver +
                ", superConstructors=" + superConstructors +
                ", originTypeIdentifier=" + originTypeIdentifier +
                '}';
    }

    /**
     * Responsible for identifying the origin type that an implementation target represents when
     * {@link Implementation.Target#getOriginType()} is invoked.
     */
    public enum OriginTypeIdentifier {

        /**
         * Identifies the super type of an instrumented type as the origin type.
         */
        SUPER_TYPE {
            @Override
            protected TypeDescription identify(TypeDescription typeDescription) {
                return typeDescription.getSuperType().asRawType();
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

        @Override
        public String toString() {
            return "SubclassImplementationTarget.OriginTypeIdentifier." + name();
        }
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget}.
     */
    public static class Factory implements Implementation.Target.Factory {

        /**
         * A factory for creating a bridge method resolver to be handed to the created subclass implementation target.
         */
        private final BridgeMethodResolver.Factory bridgeMethodResolverFactory;

        /**
         * The origin type identifier to use.
         */
        private final OriginTypeIdentifier originTypeIdentifier;

        /**
         * Creates a new factory for a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget}.
         *
         * @param bridgeMethodResolverFactory A factory for creating a bridge method resolver to be handed to the
         *                                    created subclass implementation target.
         * @param originTypeIdentifier        The origin type identifier to use.
         */
        public Factory(BridgeMethodResolver.Factory bridgeMethodResolverFactory,
                       OriginTypeIdentifier originTypeIdentifier) {
            this.bridgeMethodResolverFactory = bridgeMethodResolverFactory;
            this.originTypeIdentifier = originTypeIdentifier;
        }

        @Override
        public Implementation.Target make(MethodLookupEngine.Finding finding, List<? extends MethodDescription> instrumentedMethods) {
            return new SubclassImplementationTarget(finding, bridgeMethodResolverFactory, originTypeIdentifier);
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
            return "SubclassImplementationTarget.Factory{" +
                    "bridgeMethodResolverFactory=" + bridgeMethodResolverFactory +
                    "originTypeIdentifier=" + originTypeIdentifier +
                    '}';
        }
    }
}
