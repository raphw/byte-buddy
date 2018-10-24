/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    StackManipulation withValues(List<? extends StackManipulation> stackManipulations);
}
