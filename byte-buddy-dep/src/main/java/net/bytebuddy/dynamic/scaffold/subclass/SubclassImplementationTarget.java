package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;

import static net.bytebuddy.matcher.ElementMatchers.hasSignature;
import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;

/**
 * An implementation target for creating a subclass of a given type. 用于创建给定类型的子类的 implementation target
 */
@HashCodeAndEqualsPlugin.Enhance
public class SubclassImplementationTarget extends Implementation.Target.AbstractBase {

    /**
     * The origin type identifier to use.
     */
    protected final OriginTypeResolver originTypeResolver;

    /**
     * Creates a new subclass implementation target.
     *
     * @param instrumentedType        The instrumented type.
     * @param methodGraph             A method graph of the instrumented type.
     * @param defaultMethodInvocation The default method invocation mode to apply.
     * @param originTypeResolver      A resolver for the origin type.
     */
    protected SubclassImplementationTarget(TypeDescription instrumentedType,
                                           MethodGraph.Linked methodGraph,
                                           DefaultMethodInvocation defaultMethodInvocation,
                                           OriginTypeResolver originTypeResolver) {
        super(instrumentedType, methodGraph, defaultMethodInvocation);
        this.originTypeResolver = originTypeResolver;
    }

    @Override
    public Implementation.SpecialMethodInvocation invokeSuper(MethodDescription.SignatureToken token) {
        return token.getName().equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                ? invokeConstructor(token)
                : invokeMethod(token);
    }

    /**
     * Resolves a special method invocation for a constructor invocation. 解析构造函数调用的特殊方法调用
     *
     * @param token A token describing the constructor to be invoked. 描述要调用的构造函数的标记
     * @return A special method invocation for a constructor representing the given method token, if available. 表示给定方法令牌（如果可用）的构造函数的特殊方法调用
     */
    private Implementation.SpecialMethodInvocation invokeConstructor(MethodDescription.SignatureToken token) {
        TypeDescription.Generic superClass = instrumentedType.getSuperClass();
        MethodList<?> candidates = superClass == null
                ? new MethodList.Empty<MethodDescription.InGenericShape>()
                : superClass.getDeclaredMethods().filter(hasSignature(token).and(isVisibleTo(instrumentedType)));
        return candidates.size() == 1
                ? Implementation.SpecialMethodInvocation.Simple.of(candidates.getOnly(), instrumentedType.getSuperClass().asErasure())
                : Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
    }

    /**
     * Resolves a special method invocation for a non-constructor invocation. 解析非构造函数调用的特殊方法调用
     *
     * @param token A token describing the method to be invoked. 描述要调用的方法的标记
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

    /**
     * Responsible for identifying the origin type that an implementation target represents when
     * {@link Implementation.Target#getOriginType()} is invoked. 负责识别 {@link Implementation.Target#getOriginType()} 被调用时实现目标表示的源类型
     */
    public enum OriginTypeResolver {

        /**
         * Identifies the super type of an instrumented type as the origin class. 将插桩类型的超类标识为原始类
         */
        SUPER_CLASS {
            @Override
            protected TypeDefinition identify(TypeDescription typeDescription) {
                return typeDescription.getSuperClass();
            }
        },

        /**
         * Identifies the instrumented type as its own origin type. 将插桩类型标识为其自己的源类型
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
    }

    /**
     * A factory for creating a {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget}. 创建 {@link net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget} 的工厂类
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
        public Implementation.Target make(TypeDescription instrumentedType, MethodGraph.Linked methodGraph, ClassFileVersion classFileVersion) {
            return new SubclassImplementationTarget(instrumentedType, methodGraph, DefaultMethodInvocation.of(classFileVersion), originTypeResolver);
        }
    }
}
