package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isVisibilityBridge;
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
    protected final OriginTypeIdentifier originTypeIdentifier;

    protected SubclassImplementationTarget(TypeDescription instrumentedType, MethodGraph.Linked methodGraph, OriginTypeIdentifier originTypeIdentifier) {
        super(instrumentedType, methodGraph);
        GenericTypeDescription superType = instrumentedType.getSuperType();
        MethodList<?> superConstructors = superType == null
                ? new MethodList.Empty()
                : superType.getDeclaredMethods().filter(isConstructor().and(isVisibleTo(instrumentedType)));
        this.superConstructors = new HashMap<MethodDescription.Token, MethodDescription>(superConstructors.size());
        for (MethodDescription superConstructor : superConstructors) {
            this.superConstructors.put(superConstructor.asToken(), superConstructor);
        }
        this.originTypeIdentifier = originTypeIdentifier;
    }

    @Override
    public Implementation.SpecialMethodInvocation invokeSuper(MethodDescription.Token methodToken) {
        return methodToken.getInternalName().equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                ? invokeConstructor(methodToken)
                : invokeMethod(methodToken);
    }

    private Implementation.SpecialMethodInvocation invokeConstructor(MethodDescription.Token methodToken) {
        MethodDescription methodDescription = superConstructors.get(methodToken);
        return methodDescription == null
                ? Implementation.SpecialMethodInvocation.Illegal.INSTANCE
                : Implementation.SpecialMethodInvocation.Simple.of(methodDescription, instrumentedType.getSuperType().asRawType());
    }

    private Implementation.SpecialMethodInvocation invokeMethod(MethodDescription.Token methodToken) {
        MethodGraph.Node methodNode = methodGraph.getSuperGraph().locate(methodToken);
        return methodNode.getSort().isUnique()
                ? Implementation.SpecialMethodInvocation.Simple.of(methodNode.getRepresentative(), instrumentedType.getSuperType().asRawType())
                : Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
    }

    @Override
    public TypeDescription getOriginType() {
        return originTypeIdentifier.identify(instrumentedType);
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
         * The origin type identifier to use.
         */
        private final OriginTypeIdentifier originTypeIdentifier;

        public Factory(OriginTypeIdentifier originTypeIdentifier) {
            this.originTypeIdentifier = originTypeIdentifier;
        }

        @Override
        public Implementation.Target make(TypeDescription instrumentedType, MethodGraph.Linked methodGraph) {
            return new SubclassImplementationTarget(instrumentedType, methodGraph, originTypeIdentifier);
        }
    }
}
