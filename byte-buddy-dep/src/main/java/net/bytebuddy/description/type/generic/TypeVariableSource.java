package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * A type variable source represents a byte code element that can declare type variables.
 */
public interface TypeVariableSource extends ByteCodeElement {

    /**
     * Returns the type variables that are declared by this element.
     *
     * @return The type variables that are declared by this element.
     */
    GenericTypeList getTypeVariables();

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
    GenericTypeDescription findVariable(String symbol);

    /**
     * Applies a visitor on this type variable source.
     *
     * @param visitor The visitor to apply.
     * @param <T>     The visitor's return type.
     * @return The visitor's return value.
     */
    <T> T accept(Visitor<T> visitor);

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
        T onMethod(MethodDescription methodDescription);

        /**
         * A none-operational implementation of a type variable visitor that simply returns the visited source.
         */
        enum NoOp implements Visitor<TypeVariableSource> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public TypeVariableSource onType(TypeDescription typeDescription) {
                return typeDescription;
            }

            @Override
            public TypeVariableSource onMethod(MethodDescription methodDescription) {
                return methodDescription;
            }

            @Override
            public String toString() {
                return "TypeVariableSource.Visitor.NoOp." + name();
            }
        }
    }
}
