package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;

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
    protected final OriginTypeResolver originTypeResolver;

    /**
     * Creates a new subclass implementation target.
     *
     * @param instrumentedType   The instrumented type.
     * @param methodGraph        A method graph of the instrumented type.
     * @param originTypeResolver A resolver for the origin type.
     */
    protected SubclassImplementationTarget(TypeDescription instrumentedType, MethodGraph.Linked methodGraph, OriginTypeResolver originTypeResolver) {
        super(instrumentedType, methodGraph);
        GenericTypeDescription superType = instrumentedType.getSuperType();
        MethodList<?> superConstructors = superType == null
                ? new MethodList.Empty()
                : superType.getDeclaredMethods().filter(isConstructor().and(isVisibleTo(instrumentedType)));
        this.superConstructors = new HashMap<MethodDescription.Token, MethodDescription>(superConstructors.size());
        for (MethodDescription superConstructor : superConstructors) {
            this.superConstructors.put(superConstructor.asToken(), superConstructor);
        }
        this.originTypeResolver = originTypeResolver;
    }

    @Override
    public Implementation.SpecialMethodInvocation invokeSuper(MethodDescription.Token methodToken) {
        return methodToken.getInternalName().equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                ? invokeConstructor(methodToken)
                : invokeMethod(methodToken);
    }

    /**
     * Resolves a special method invocation for a constructor invocation.
     *
     * @param methodToken A token describing the constructor to be invoked.
     * @return A special method invocation for a constructor representing the given method token, if available.
     */
    private Implementation.SpecialMethodInvocation invokeConstructor(MethodDescription.Token methodToken) {
        MethodDescription methodDescription = superConstructors.get(methodToken);
        return methodDescription == null
                ? Implementation.SpecialMethodInvocation.Illegal.INSTANCE
                : Implementation.SpecialMethodInvocation.Simple.of(methodDescription, instrumentedType.getSuperType().asErasure());
    }

    /**
     * Resolves a special method invocation for a non-constructor invocation.
     *
     * @param methodToken A token describing the method to be invoked.
     * @return A special method invocation for a method representing the given method token, if available.
     */
    private Implementation.SpecialMethodInvocation invokeMethod(MethodDescription.Token methodToken) {
        MethodGraph.Node methodNode = methodGraph.getSuperGraph().locate(methodToken);
        return methodNode.getSort().isUnique()
                ? Implementation.SpecialMethodInvocation.Simple.of(methodNode.getRepresentative(), instrumentedType.getSuperType().asErasure())
                : Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
    }

    @Override
    public TypeDescription getOriginType() {
        return originTypeResolver.identify(instrumentedType);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && super.equals(other)
                && superConstructors.equals(((SubclassImplementationTarget) other).superConstructors)
                && originTypeResolver == ((SubclassImplementationTarget) other).originTypeResolver;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + superConstructors.hashCode();
        result = 31 * result + originTypeResolver.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SubclassImplementationTarget{" +
                "superConstructors=" + superConstructors +
                ", originTypeResolver=" + originTypeResolver +
                ", instrumentedType=" + instrumentedType +
                ", methodGraph=" + methodGraph +
                '}';
    }

    /**
     * Responsible for identifying the origin type that an implementation target represents when
     * {@link Implementation.Target#getOriginType()} is invoked.
     */
    public enum OriginTypeResolver {

        /**
         * Identifies the super type of an instrumented type as the origin type.
         */
        SUPER_TYPE {
            @Override
            protected TypeDescription identify(TypeDescription typeDescription) {
                return typeDescription.getSuperType().asErasure();
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
            return "SubclassImplementationTarget.OriginTypeResolver." + name();
        }
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget}.
     */
    public static class Factory implements Implementation.Target.Factory {

        /**
         * The origin type identifier to use.
         */
        private final OriginTypeResolver originTypeResolver;

        /**
         * Creates a factory for creating a subclass implementation target.
         *
         * @param originTypeResolver The origin type identifier to use.
         */
        public Factory(OriginTypeResolver originTypeResolver) {
            this.originTypeResolver = originTypeResolver;
        }

        @Override
        public Implementation.Target make(TypeDescription instrumentedType, MethodGraph.Linked methodGraph) {
            return new SubclassImplementationTarget(instrumentedType, methodGraph, originTypeResolver);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && originTypeResolver == ((Factory) other).originTypeResolver;
        }

        @Override
        public int hashCode() {
            return originTypeResolver.hashCode();
        }

        @Override
        public String toString() {
            return "SubclassImplementationTarget.Factory{" +
                    "originTypeResolver=" + originTypeResolver +
                    '}';
        }
    }
}
