package net.bytebuddy.description;

import net.bytebuddy.description.type.TypeDefinition;

/**
 * This interface represents all elements that can be declared within a type, i.e. other types and type members.  此接口表示可以在一个类型中声明的所有元素，即其他类型和类型成员
 */
public interface DeclaredByType {

    /**
     * Returns the declaring type of this instance. 返回此实例的声明类型
     *
     * @return The declaring type or {@code null} if no such type exists.
     */
    TypeDefinition getDeclaringType();
}
