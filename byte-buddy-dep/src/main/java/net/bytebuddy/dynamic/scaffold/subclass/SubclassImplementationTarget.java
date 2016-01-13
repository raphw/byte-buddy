package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
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
    protected final Map<MethodDescription.SignatureToken, MethodDescription> superConstructors;

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
        TypeDescription.Generic superClass = instrumentedType.getSuperClass();
        MethodList<?> superConstructors = superClass == null
                ? new MethodList.Empty<MethodDescription.InGenericShape>()
                : superClass.getDeclaredMethods().filter(isConstructor().and(isVisibleTo(instrumentedType)));
        this.superConstructors = new HashMap<MethodDescription.SignatureToken, MethodDescription>();
        for (MethodDescription superConstructor : superConstructors) {
            this.superConstructors.put(superConstructor.asSignatureToken(), superConstructor);
        }
        this.originTypeResolver = originTypeResolver;
    }

    @Override
    public Implementation.SpecialMethodInvocation invokeSuper(MethodDescription.SignatureToken token) {
        return token.getName().equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                ? invokeConstructor(token)
                : invokeMethod(token);
    }

    /**
     * Resolves a special method invocation for a constructor invocation.
     *
     * @param token A token describing the constructor to be invoked.
     * @return A special method invocation for a constructor representing the given method token, if available.
     */
    private Implementation.SpecialMethodInvocation invokeConstructor(MethodDescription.SignatureToken token) {
        MethodDescription methodDescription = superConstructors.get(token);
        return methodDescription == null
                ? Implementation.SpecialMethodInvocation.Illegal.INSTANCE
                : Implementation.SpecialMethodInvocation.Simple.of(methodDescription, instrumentedType.getSuperClass().asErasure());
    }

    /**
     * Resolves a special method invocation for a non-constructor invocation.
     *
     * @param token A token describing the method to be invoked.
     * @return A special method invocation for a method representing the given method token, if available.
     */
    private Implementation.SpecialMethodInvocation invokeMethod(MethodDescription.SignatureToken token) {
        MethodGraph.Node methodNode = methodGraph.getSuperClassGraph().locate(token);
        return methodNode.getSort().isUnique()
                ? Implementation.SpecialMethodInvocation.Simple.of(methodNode.getRepresentative(), instrumentedType.getSuperClass().asErasure())
                : Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
    }

    @Override
    public TypeDefinition getOriginType() {
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
         * Identifies the super type of an instrumented type as the origin class.
         */
        SUPER_CLASS {
            @Override
            protected TypeDefinition identify(TypeDescription typeDescription) {
                return typeDescription.getSuperClass();
            }
        },

        /**
         * Identifies the instrumented type as its own origin type.
         */
        LEVEL_TYPE {
            @Override
            protected TypeDefinition identify(TypeDescription typeDescription) {
                return typeDescription;
            }
        };

        /**
         * Identifies the origin type to a given type description.
         *
         * @param typeDescription The type description for which an origin type should be identified.
         * @return The origin type to the given type description.
         */
        protected abstract TypeDefinition identify(TypeDescription typeDescription);

        @Override
        public String toString() {
            return "SubclassImplementationTarget.OriginTypeResolver." + name();
        }
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget}.
     */
    public enum Factory implements Implementation.Target.Factory {

        /**
         * A factory creating a subclass implementation target with a {@link OriginTypeResolver#SUPER_CLASS}.
         */
        SUPER_CLASS(OriginTypeResolver.SUPER_CLASS),

        /**
         * A factory creating a subclass implementation target with a {@link OriginTypeResolver#LEVEL_TYPE}.
         */
        LEVEL_TYPE(OriginTypeResolver.LEVEL_TYPE);

        /**
         * The origin type resolver that this factory hands to the created {@link SubclassImplementationTarget}.
         */
        private final OriginTypeResolver originTypeResolver;

        /**
         * Creates a new factory.
         *
         * @param originTypeResolver The origin type resolver that this factory hands to the created {@link SubclassImplementationTarget}.
         */
        Factory(OriginTypeResolver originTypeResolver) {
            this.originTypeResolver = originTypeResolver;
        }

        @Override
        public Implementation.Target make(TypeDescription instrumentedType, MethodGraph.Linked methodGraph) {
            return new SubclassImplementationTarget(instrumentedType, methodGraph, originTypeResolver);
        }

        @Override
        public String toString() {
            return "SubclassImplementationTarget.Factory." + name();
        }
    }
}
