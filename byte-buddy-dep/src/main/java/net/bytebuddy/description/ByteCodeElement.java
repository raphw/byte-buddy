package net.bytebuddy.description;

import net.bytebuddy.description.annotation.AnnotatedCodeElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

import java.util.AbstractList;
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
     * Returns the generic signature of this byte code element.
     *
     * @return The generic signature or {@code null} if this element is not generic.
     */
    String getGenericSignature();

    /**
     * Checks if this element is visible from a given type.
     *
     * @param typeDescription The type which is checked for its access of this element.
     * @return {@code true} if this element is visible for {@code typeDescription}.
     */
    boolean isVisibleTo(TypeDescription typeDescription);

    /**
     * Representation of a tokenized, detached byte code element.
     *
     * @param <T> The actual token type.
     */
    interface Token<T extends Token<T>> {

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
        class TokenList<S extends Token<S>> extends AbstractList<S> {

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
