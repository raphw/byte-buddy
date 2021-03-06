package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;

import java.util.List;

/**
 * Implementations of this interface are able to create collection types (including arrays) by sequentially
 * storing given values that must be compatible to the collection factory's component type. 此接口的实现能够通过顺序存储必须与收集工厂的组件类型兼容的给定值来创建收集类型（包括数组）
 */
public interface CollectionFactory {

    /**
     * The component type of this factory.
     *
     * @return A type description of this factory's component type.
     */
    TypeDescription.Generic getComponentType();

    /**
     * Applies this collection factory in order to build a new collection where each element is represented by
     * the given stack manipulations. 应用此集合工厂以构建新集合，其中每个元素均由给定的堆栈操作表示
     *
     * @param stackManipulations A list of stack manipulations loading the values to be stored in the collection that is
     *                           created by this factory in their given order.
     * @return A stack manipulation that creates the collection represented by this collection factory.
     */
    StackManipulation withValues(List<? extends StackManipulation> stackManipulations);
}
