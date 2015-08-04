package net.bytebuddy.description;

import net.bytebuddy.description.annotation.AnnotatedCodeElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementations describe an element represented in byte code, i.e. a type, a field or a method or a constructor.
 */
public interface ByteCodeElement extends NamedElement.WithRuntimeName, ModifierReviewable, DeclaredByType, AnnotatedCodeElement {

    /**
     * The generic type signature of a non-generic byte code element.
     */
    String NON_GENERIC_SIGNATURE = null;

    /**
     * Returns the descriptor of this byte code element.
     *
     * @return The descriptor of this byte code element.
     */
    String getDescriptor();

    /**
     * Returns the generic signature of this byte code element. If this element does not reference generic types
     * or references malformed generic types, {@code null} is returned as a signature.
     *
     * @return The generic signature or {@code null} if this element is not generic.
     */
    String getGenericSignature();

    /**
     * Checks if this element is visible from a given type. Methods are only considered visible if their return type and their parameter
     * types are also visible to the given type. Similarly, fields are only considered visible if the field's type is visible to the
     * given type. For array types, a type is considered visible only if the component type is visible to the given type.
     *
     * @param typeDescription The type which is checked for its access of this element.
     * @return {@code true} if this element is visible for {@code typeDescription}.
     */
    boolean isVisibleTo(TypeDescription typeDescription);

    /**
     * A type dependant describes an element that is an extension of a type definition, i.e. a field, method or method parameter.
     *
     * @param <T> The type dependant's type.
     * @param <S> The type dependant's token type.
     */
    interface TypeDependant<T extends TypeDependant<?, S>, S extends ByteCodeElement.Token<S>> {

        /**
         * Returns this type dependant in its defined shape, i.e. the form it is declared in and without its type variable's resolved.
         *
         * @return This type dependant in its defined shape.
         */
        T asDefined();

        /**
         * Returns a token representative of this type dependant.
         *
         * @return A token representative of this type dependant.
         */
        S asToken();

        /**
         * Returns a token representative of this type dependant. All types that are matched by the supplied matcher are replaced by
         * {@link net.bytebuddy.dynamic.TargetType} descriptions.
         *
         * @param targetTypeMatcher A matcher to identify types to be replaced by {@link net.bytebuddy.dynamic.TargetType} descriptions.
         * @return A token representative of this type dependant.
         */
        S asToken(ElementMatcher<? super GenericTypeDescription> targetTypeMatcher);
    }

    /**
     * Representation of a tokenized, detached byte code element.
     *
     * @param <T> The actual token type.
     */
    interface Token<T extends Token<T>> {

        /**
         * Checks if this token is fully identical to the provided token.
         *
         * @param token The token to compare this token with.
         * @return {@code true} if this token is identical to the given token.
         */
        boolean isIdenticalTo(T token);

        /**
         * Transforms the types represented by this token by applying the given visitor to them.
         *
         * @param visitor The visitor to transform all types that are represented by this token.
         * @return This token with all of its represented types transformed by the supplied visitor.
         */
        T accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor);

        /**
         * A list of tokens.
         *
         * @param <S> The actual token type.
         */
        class TokenList<S extends Token<S>> extends FilterableList.AbstractBase<S, TokenList<S>> {

            /**
             * The tokens that this list represents.
             */
            private final List<? extends S> tokens;

            /**
             * Creates a list of tokens.
             *
             * @param tokens The tokens that this list represents.
             */
            public TokenList(List<? extends S> tokens) {
                this.tokens = tokens;
            }

            /**
             * Transforms all tokens that are represented by this list.
             *
             * @param visitor The visitor to apply to all tokens.
             * @return A list containing the transformed tokens.
             */
            public TokenList<S> accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
                List<S> tokens = new ArrayList<S>(this.tokens.size());
                for (S token : this.tokens) {
                    tokens.add(token.accept(visitor));
                }
                return new TokenList<S>(tokens);
            }

            @Override
            protected TokenList<S> wrap(List<S> values) {
                return new TokenList<S>(values);
            }

            @Override
            public S get(int index) {
                return tokens.get(index);
            }

            @Override
            public int size() {
                return tokens.size();
            }
        }
    }
}
