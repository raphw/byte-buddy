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

    interface Token<T extends Token> {

        T accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor);

        class TokenList<S extends Token<S>> extends AbstractList<S> {

            private final List<? extends S> tokens;

            public TokenList(List<? extends S> tokens) {
                this.tokens = tokens;
            }

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
