package net.bytebuddy.description.type;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Arrays;
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
     * Creates a new type variable token.
     *
     * @param symbol The type variable's symbol.
     * @param bound  The type variable's upper bounds.
     */
    public TypeVariableToken(String symbol, TypeDescription.Generic... bound) {
        this(symbol, Arrays.asList(bound));
    }

    /**
     * Creates a new type variable token.
     *
     * @param symbol The type variable's symbol.
     * @param bounds The type variable's upper bounds.
     */
    public TypeVariableToken(String symbol, List<? extends TypeDescription.Generic> bounds) {
        this.symbol = symbol;
        this.bounds = bounds;
    }

    /**
     * Transforms a type variable into a type variable token with its bounds detached.
     *
     * @param typeVariable A type variable in its attached state.
     * @param matcher      A matcher that identifies types to detach from the upper bound types.
     * @return A token representing the detached type variable.
     */
    public static TypeVariableToken of(TypeDescription.Generic typeVariable, ElementMatcher<? super TypeDescription> matcher) {
        return new TypeVariableToken(typeVariable.getSymbol(), typeVariable.getUpperBounds().accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(matcher)));
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

    @Override
    public TypeVariableToken accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
        return new TypeVariableToken(getSymbol(), getBounds().accept(visitor));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        TypeVariableToken that = (TypeVariableToken) other;
        return symbol.equals(that.symbol) && bounds.equals(that.bounds);
    }

    @Override
    public int hashCode() {
        int result = symbol.hashCode();
        result = 31 * result + bounds.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TypeVariableToken{" +
                "symbol='" + symbol + '\'' +
                ", bounds=" + bounds +
                '}';
    }
}
