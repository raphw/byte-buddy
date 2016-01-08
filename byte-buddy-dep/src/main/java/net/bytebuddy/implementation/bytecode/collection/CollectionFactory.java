package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;

import java.util.List;

/**
 * Implementations of this interface are able to create collection types (including arrays) by sequentially
 * storing given values that must be compatible to the collection factory's component type.
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
     * the given stack manipulations.
     *
     * @param stackManipulations A list of stack manipulations loading the values to be stored in the collection that is
     *                           created by this factory in their given order.
     * @return A stack manipulation that creates the collection represented by this collection factory.
     */
    StackManipulation withValues(List<StackManipulation> stackManipulations);
}
