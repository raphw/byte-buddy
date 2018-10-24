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
package net.bytebuddy.description.type;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Collections;
import java.util.List;

/**
 * A tokenized representation of a type variable.
 */
public class TypeVariableToken implements ByteCodeElement.Token<TypeVariableToken> {

    /**
     * The type variable's symbol.
     */
    private final String symbol;

    /**
     * The type variable's upper bounds.
     */
    private final List<? extends TypeDescription.Generic> bounds;

    /**
     * The annotations of the type variable.
     */
    private final List<? extends AnnotationDescription> annotations;

    /**
     * Creates a new type variable token without annotations.
     *
     * @param symbol The type variable's symbol.
     * @param bounds The type variable's upper bounds.
     */
    public TypeVariableToken(String symbol, List<? extends TypeDescription.Generic> bounds) {
        this(symbol, bounds, Collections.<AnnotationDescription>emptyList());
    }

    /**
     * Creates a new type variable token.
     *
     * @param symbol      The type variable's symbol.
     * @param bounds      The type variable's upper bounds.
     * @param annotations The annotations of the type variable.
     */
    public TypeVariableToken(String symbol, List<? extends TypeDescription.Generic> bounds, List<? extends AnnotationDescription> annotations) {
        this.symbol = symbol;
        this.bounds = bounds;
        this.annotations = annotations;
    }

    /**
     * Transforms a type variable into a type variable token with its bounds detached.
     *
     * @param typeVariable A type variable in its attached state.
     * @param matcher      A matcher that identifies types to detach from the upper bound types.
     * @return A token representing the detached type variable.
     */
    public static TypeVariableToken of(TypeDescription.Generic typeVariable, ElementMatcher<? super TypeDescription> matcher) {
        return new TypeVariableToken(typeVariable.getSymbol(),
                typeVariable.getUpperBounds().accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(matcher)),
                typeVariable.getDeclaredAnnotations());
    }

    /**
     * Returns the type variable's symbol.
     *
     * @return The type variable's symbol.
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns the type variable's upper bounds.
     *
     * @return The type variable's upper bounds.
     */
    public TypeList.Generic getBounds() {
        return new TypeList.Generic.Explicit(bounds);
    }

    /**
     * Returns the annotations on this type variable.
     *
     * @return The annotations on this variable.
     */
    public AnnotationList getAnnotations() {
        return new AnnotationList.Explicit(annotations);
    }

    /**
     * {@inheritDoc}
     */
    public TypeVariableToken accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
        return new TypeVariableToken(symbol, getBounds().accept(visitor), annotations);
    }

    @Override
    public int hashCode() {
        int result = symbol.hashCode();
        result = 31 * result + bounds.hashCode();
        result = 31 * result + annotations.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof TypeVariableToken)) {
            return false;
        }
        TypeVariableToken typeVariableToken = (TypeVariableToken) other;
        return symbol.equals(typeVariableToken.symbol)
                && bounds.equals(typeVariableToken.bounds)
                && annotations.equals(typeVariableToken.annotations);
    }

    @Override
    public String toString() {
        return symbol;
    }
}
