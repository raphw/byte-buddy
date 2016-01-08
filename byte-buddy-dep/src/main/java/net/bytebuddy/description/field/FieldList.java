package net.bytebuddy.description.field;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations represent a list of field descriptions.
 *
 * @param <T> The type of field descriptions represented by this list.
 */
public interface FieldList<T extends FieldDescription> extends FilterableList<T, FieldList<T>> {

    /**
     * Transforms the list of field descriptions into a list of detached tokens. All types that are matched by the provided
     * target type matcher are substituted by {@link net.bytebuddy.dynamic.TargetType}.
     *
     * @param matcher A matcher that indicates type substitution.
     * @return The transformed token list.
     */
    ByteCodeElement.Token.TokenList<FieldDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher);

    /**
     * Returns this list of these field descriptions resolved to their defined shape.
     *
     * @return A list of fields in their defined shape.
     */
    FieldList<FieldDescription.InDefinedShape> asDefined();

    /**
     * An abstract base implementation of a {@link FieldList}.
     *
     * @param <S> The type of field descriptions represented by this list.
     */
    abstract class AbstractBase<S extends FieldDescription> extends FilterableList.AbstractBase<S, FieldList<S>> implements FieldList<S> {

        @Override
        public ByteCodeElement.Token.TokenList<FieldDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher) {
            List<FieldDescription.Token> tokens = new ArrayList<FieldDescription.Token>(size());
            for (FieldDescription fieldDescription : this) {
                tokens.add(fieldDescription.asToken(matcher));
            }
            return new ByteCodeElement.Token.TokenList<FieldDescription.Token>(tokens);
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> asDefined() {
            List<FieldDescription.InDefinedShape> declaredForms = new ArrayList<FieldDescription.InDefinedShape>(size());
            for (FieldDescription fieldDescription : this) {
                declaredForms.add(fieldDescription.asDefined());
            }
            return new Explicit<FieldDescription.InDefinedShape>(declaredForms);
        }

        @Override
        protected FieldList<S> wrap(List<S> values) {
            return new Explicit<S>(values);
        }
    }

    /**
     * An implementation of a field list for an array of loaded fields.
     */
    class ForLoadedFields extends AbstractBase<FieldDescription.InDefinedShape> {

        /**
         * The loaded fields this field list represents.
         */
        private final List<? extends Field> fields;

        /**
         * Creates a new immutable field list that represents an array of loaded field.
         *
         * @param field An array of fields to be represented by this field list.
         */
        public ForLoadedFields(Field... field) {
            this(Arrays.asList(field));
        }

        /**
         * Creates a new immutable field list that represents an array of loaded field.
         *
         * @param fields An array of fields to be represented by this field list.
         */
        public ForLoadedFields(List<? extends Field> fields) {
            this.fields = fields;
        }

        @Override
        public FieldDescription.InDefinedShape get(int index) {
            return new FieldDescription.ForLoadedField(fields.get(index));
        }

        @Override
        public int size() {
            return fields.size();
        }
    }

    /**
     * A wrapper implementation of a field list for a given list of field descriptions.
     *
     * @param <S> The type of field descriptions represented by this list.
     */
    class Explicit<S extends FieldDescription> extends AbstractBase<S> {

        /**
         * The list of field descriptions this list represents.
         */
        private final List<? extends S> fieldDescriptions;

        /**
         * Creates a new immutable wrapper field list.
         *
         * @param fieldDescription The list of fields to be represented by this field list.
         */
        @SuppressWarnings("unchecked")
        public Explicit(S... fieldDescription) {
            this(Arrays.asList(fieldDescription));
        }

        /**
         * Creates a new immutable wrapper field list.
         *
         * @param fieldDescriptions The list of fields to be represented by this field list.
         */
        public Explicit(List<? extends S> fieldDescriptions) {
            this.fieldDescriptions = fieldDescriptions;
        }

        @Override
        public S get(int index) {
            return fieldDescriptions.get(index);
        }

        @Override
        public int size() {
            return fieldDescriptions.size();
        }
    }

    /**
     * A list of field descriptions for a list of detached tokens. For the returned fields, each token is attached to its field representation.
     */
    class ForTokens extends AbstractBase<FieldDescription.InDefinedShape> {

        /**
         * The declaring type of the represented fields.
         */
        private final TypeDescription declaringType;

        /**
         * A list of the represented fields' tokens.
         */
        private final List<? extends FieldDescription.Token> tokens;

        /**
         * Creates a new field list from a list of field tokens.
         *
         * @param declaringType The declaring type of the represented fields.
         * @param token         A list of the represented fields' tokens.
         */
        public ForTokens(TypeDescription declaringType, FieldDescription.Token... token) {
            this(declaringType, Arrays.asList(token));
        }

        /**
         * Creates a new field list from a list of field tokens.
         *
         * @param declaringType The declaring type of the represented fields.
         * @param tokens        A list of the represented fields' tokens.
         */
        public ForTokens(TypeDescription declaringType, List<? extends FieldDescription.Token> tokens) {
            this.declaringType = declaringType;
            this.tokens = tokens;
        }

        @Override
        public FieldDescription.InDefinedShape get(int index) {
            return new FieldDescription.Latent(declaringType, tokens.get(index));
        }

        @Override
        public int size() {
            return tokens.size();
        }
    }

    /**
     * A list of field descriptions that yields {@link net.bytebuddy.description.field.FieldDescription.TypeSubstituting}.
     */
    class TypeSubstituting extends AbstractBase<FieldDescription.InGenericShape> {

        /**
         * The field's actual declaring type.
         */
        private final TypeDescription.Generic declaringType;

        /**
         * The field descriptions to be transformed.
         */
        private final List<? extends FieldDescription> fieldDescriptions;

        /**
         * The visitor to apply to a field description.
         */
        private final TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

        /**
         * Creates a new type substituting field list.
         *
         * @param declaringType     The field's actual declaring type.
         * @param fieldDescriptions The field descriptions to be transformed.
         * @param visitor           The visitor to apply to a field description.
         */
        public TypeSubstituting(TypeDescription.Generic declaringType,
                                List<? extends FieldDescription> fieldDescriptions,
                                TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
            this.declaringType = declaringType;
            this.fieldDescriptions = fieldDescriptions;
            this.visitor = visitor;
        }

        @Override
        public FieldDescription.InGenericShape get(int index) {
            return new FieldDescription.TypeSubstituting(declaringType, fieldDescriptions.get(index), visitor);
        }

        @Override
        public int size() {
            return fieldDescriptions.size();
        }
    }

    /**
     * An implementation of an empty field list.
     *
     * @param <S> The type of parameter descriptions represented by this list.
     */
    class Empty<S extends FieldDescription> extends FilterableList.Empty<S, FieldList<S>> implements FieldList<S> {

        @Override
        public ByteCodeElement.Token.TokenList<FieldDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher) {
            return new ByteCodeElement.Token.TokenList<FieldDescription.Token>();
        }

        @Override
        @SuppressWarnings("unchecked")
        public FieldList<FieldDescription.InDefinedShape> asDefined() {
            return (FieldList<FieldDescription.InDefinedShape>) this;
        }
    }
}
