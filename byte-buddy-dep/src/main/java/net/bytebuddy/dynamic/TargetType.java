package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;

/**
 * This type is used as a place holder for creating methods or fields that refer to the type that currently subject
 * of creation within a {@link net.bytebuddy.dynamic.DynamicType.Builder}. 一个占位符 TargetType, 用来替代 methods 或者 fields
 */
public final class TargetType {

    /**
     * A description of the {@link net.bytebuddy.dynamic.TargetType}.  {@link net.bytebuddy.dynamic.TargetType} 的描述
     */
    public static final TypeDescription DESCRIPTION = TypeDescription.ForLoadedType.of(TargetType.class);

    /**
     * Resolves the given type description to the supplied target type if it represents the {@link TargetType} placeholder. 如果给定的类型描述表示{@link TargetType}占位符，则将其解析为提供的目标类型
     * Array types are resolved to their component type and rebuilt as an array of the actual target type, if necessary. 数组类型将解析为其组件类型，并在必要时重建为实际目标类型的数组
     *
     * @param typeDescription The type description that might represent the {@link TargetType} placeholder. 可能表示{@link TargetType}占位符的类型描述
     * @param targetType      The actual target type. 实际目标类型
     * @return A description of the resolved type. 已解析类型的描述
     */
    public static TypeDescription resolve(TypeDescription typeDescription, TypeDescription targetType) {
        int arity = 0;
        TypeDescription componentType = typeDescription;
        while (componentType.isArray()) {
            componentType = componentType.getComponentType();
            arity++;
        }
        return componentType.represents(TargetType.class)
                ? TypeDescription.ArrayProjection.of(targetType, arity)
                : typeDescription;
    }

    /**
     * An unusable constructor to avoid instance creation. 无法使用的构造函数，以避免创建实例
     */
    private TargetType() {
        throw new UnsupportedOperationException("This class only serves as a marker type and should not be instantiated");
    }
}
