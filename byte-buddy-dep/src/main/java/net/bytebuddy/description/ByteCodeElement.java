package net.bytebuddy.description;

import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations describe an element represented in byte code, i.e. a type, a field or a method or a constructor.
 */
public interface ByteCodeElement extends NamedElement.WithRuntimeName, ModifierReviewable, DeclaredByType, AnnotationSource {

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
     * <p>
     * Checks if this element is visible from a given type. Visibility is a wider criteria then accessibility which can be checked by
     * {@link ByteCodeElement#isAccessibleTo(TypeDescription)}. Visibility allows the invocation of a method on itself or on external
     * instances.
     * </p>
     * <p>
     * <b>Note</b>: A method or field might define a signature that includes types that are not visible to a type. Such methods can be
     * legally invoked from this type and can even be implemented as bridge methods by this type. It is however not legal to declare
     * a method with invisible types in its signature that are not bridges what might require additional validation.
     * </p>
     * <p>
     * <b>Important</b>: Virtual byte code elements, i.e. virtual methods, are only considered visible if the type they are invoked upon
     * is visible to a given type. The visibility of such virtual members can therefore not be determined by only investigating the invoked
     * method but requires an additional check of the target type.
     * </p>
     *
     * @param typeDescription The type which is checked for its visibility of this element.
     * @return {@code true} if this element is visible for {@code typeDescription}.
     */
    boolean isVisibleTo(TypeDescription typeDescription);

    /**
     * <p>
     * Checks if this element is accessible from a given type. Accessibility is a more narrow criteria then visibility which can be
     * checked by {@link ByteCodeElement#isVisibleTo(TypeDescription)}. Accessibility allows the invocation of a method on external
     * instances or on itself. Methods that can be invoked from within an instance might however not be considered accessible.
     * </p>
     * <p>
     * <b>Note</b>: A method or field might define a signature that includes types that are not visible to a type. Such methods can be
     * legally invoked from this type and can even be implemented as bridge methods by this type. It is however not legal to declare
     * a method with invisible types in its signature that are not bridges what might require additional validation.
     * </p>
     * <p>
     * <b>Important</b>: Virtual byte code elements, i.e. virtual methods, are only considered visible if the type they are invoked upon
     * is visible to a given type. The visibility of such virtual members can therefore not be determined by only investigating the invoked
     * method but requires an additional check of the target type.
     * </p>
     *
     * @param typeDescription The type which is checked for its accessibility of this element.
     * @return {@code true} if this element is accessible for {@code typeDescription}.
     */
    boolean isAccessibleTo(TypeDescription typeDescription);

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
         * Returns a token representative of this type dependant. All types that are matched by the supplied matcher are replaced by
         * {@link net.bytebuddy.dynamic.TargetType} descriptions.
         *
         * @param matcher A matcher to identify types to be replaced by {@link net.bytebuddy.dynamic.TargetType} descriptions.
         * @return A token representative of this type dependant.
         */
        S asToken(ElementMatcher<? super TypeDescription> matcher);
    }

    /**
     * A token representing a byte code element.
     *
     * @param <T> The type of the implementation.
     */
    interface Token<T extends Token<T>> {

        /**
         * Transforms the types represented by this token by applying the given visitor to them.
         *
         * @param visitor The visitor to transform all types that are represented by this token.
         * @return This token with all of its represented types transformed by the supplied visitor.
         */
        T accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor);

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
             * @param token The tokens that this list represents.
             */
            @SuppressWarnings("unchecked")
            public TokenList(S... token) {
                this(Arrays.asList(token));
            }

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
            public TokenList<S> accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
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

            /**
             * {@inheritDoc}
             */
            public S get(int index) {
                return tokens.get(index);
            }

            /**
             * {@inheritDoc}
             */
            public int size() {
                return tokens.size();
            }
        }
    }
}
