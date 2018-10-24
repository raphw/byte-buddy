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
package net.bytebuddy.description;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A type variable source represents a code element that can declare type variables.
 */
public interface TypeVariableSource extends ModifierReviewable.OfAbstraction {

    /**
     * Indicates that a type variable source is undefined.
     */
    TypeVariableSource UNDEFINED = null;

    /**
     * Returns the type variables that are declared by this element.
     *
     * @return The type variables that are declared by this element.
     */
    TypeList.Generic getTypeVariables();

    /**
     * Returns the enclosing source of type variables that are valid in the scope of this type variable source.
     *
     * @return The enclosing source or {@code null} if no such source exists.
     */
    TypeVariableSource getEnclosingSource();

    /**
     * Finds a particular variable with the given name in the closes type variable source that is visible from this instance.
     *
     * @param symbol The symbolic name of the type variable.
     * @return The type variable.
     */
    TypeDescription.Generic findVariable(String symbol);

    /**
     * Applies a visitor on this type variable source.
     *
     * @param visitor The visitor to apply.
     * @param <T>     The visitor's return type.
     * @return The visitor's return value.
     */
    <T> T accept(Visitor<T> visitor);

    /**
     * Checks if this type variable source has a generic declaration. This means:
     * <ul>
     * <li>A type declares type variables or is an inner class of a type with a generic declaration.</li>
     * <li>A method declares at least one type variable.</li>
     * </ul>
     *
     * @return {@code true} if this type code element has a generic declaration.
     */
    boolean isGenerified();

    /**
     * A visitor that can be applied to a type variable source.
     *
     * @param <T> The visitor's return type.
     */
    interface Visitor<T> {

        /**
         * Applies the visitor on a type.
         *
         * @param typeDescription The type onto which this visitor is applied.
         * @return The visitor's return value.
         */
        T onType(TypeDescription typeDescription);

        /**
         * Applies the visitor on a method.
         *
         * @param methodDescription The method onto which this visitor is applied.
         * @return The visitor's return value.
         */
        T onMethod(MethodDescription.InDefinedShape methodDescription);

        /**
         * A none-operational implementation of a type variable visitor that simply returns the visited source.
         */
        enum NoOp implements Visitor<TypeVariableSource> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public TypeVariableSource onType(TypeDescription typeDescription) {
                return typeDescription;
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableSource onMethod(MethodDescription.InDefinedShape methodDescription) {
                return methodDescription;
            }
        }
    }

    /**
     * An abstract base implementation of a type variable source.
     */
    abstract class AbstractBase extends ModifierReviewable.AbstractBase implements TypeVariableSource {

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic findVariable(String symbol) {
            TypeList.Generic typeVariables = getTypeVariables().filter(named(symbol));
            if (typeVariables.isEmpty()) {
                TypeVariableSource enclosingSource = getEnclosingSource();
                return enclosingSource == null
                        ? TypeDescription.Generic.UNDEFINED
                        : enclosingSource.findVariable(symbol);
            } else {
                return typeVariables.getOnly();
            }
        }
    }
}
