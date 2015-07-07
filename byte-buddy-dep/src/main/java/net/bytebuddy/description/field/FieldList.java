package net.bytebuddy.description.field;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementations represent a list of field descriptions.
 */
public interface FieldList extends FilterableList<FieldDescription, FieldList> {

    ByteCodeElement.Token.TokenList<FieldDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> targetTypeMatcher);

    abstract class AbstractBase extends FilterableList.AbstractBase<FieldDescription, FieldList> implements FieldList {

        @Override
        public ByteCodeElement.Token.TokenList<FieldDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> targetTypeMatcher) {
            List<FieldDescription.Token> tokens = new ArrayList<FieldDescription.Token>(size());
            for (FieldDescription fieldDescription : this) {
                tokens.add(fieldDescription.asToken(targetTypeMatcher));
            }
            return new ByteCodeElement.Token.TokenList<FieldDescription.Token>(tokens);
        }

        @Override
        protected FieldList wrap(List<FieldDescription> values) {
            return new Explicit(values);
        }
    }

    /**
     * An implementation of a field list for an array of loaded fields.
     */
    class ForLoadedField extends AbstractBase {

        /**
         * The loaded fields this field list represents.
         */
        private final List<? extends Field> fields;

        /**
         * Creates a new immutable field list that represents an array of loaded field.
         *
         * @param field An array of fields to be represented by this field list.
         */
        public ForLoadedField(Field... field) {
            this(Arrays.asList(field));
        }

        /**
         * Creates a new immutable field list that represents an array of loaded field.
         *
         * @param fields An array of fields to be represented by this field list.
         */
        public ForLoadedField(List<? extends Field> fields) {
            this.fields = fields;
        }

        @Override
        public FieldDescription get(int index) {
            return new FieldDescription.ForLoadedField(fields.get(index));
        }

        @Override
        public int size() {
            return fields.size();
        }
    }

    /**
     * A wrapper implementation of a field list for a given list of field descriptions.
     */
    class Explicit extends AbstractBase {

        /**
         * The list of field descriptions this list represents.
         */
        private final List<? extends FieldDescription> fieldDescriptions;

        /**
         * Creates a new immutable wrapper field list.
         *
         * @param fieldDescriptions The list of fields to be represented by this field list.
         */
        public Explicit(List<? extends FieldDescription> fieldDescriptions) {
            this.fieldDescriptions = fieldDescriptions;
        }

        @Override
        public FieldDescription get(int index) {
            return fieldDescriptions.get(index);
        }

        @Override
        public int size() {
            return fieldDescriptions.size();
        }
    }

    class ForTokens extends AbstractBase {

        private final TypeDescription declaringType;

        private final List<? extends FieldDescription.Token> tokens;

        public ForTokens(TypeDescription declaringType, List<? extends FieldDescription.Token> tokens) {
            this.declaringType = declaringType;
            this.tokens = tokens;
        }

        @Override
        public FieldDescription get(int index) {
            return new FieldDescription.Latent(declaringType, tokens.get(index));
        }

        @Override
        public int size() {
            return tokens.size();
        }
    }

    /**
     * An implementation of an empty field list.
     */
    class Empty extends FilterableList.Empty<FieldDescription, FieldList> implements FieldList {

        @Override
        public ByteCodeElement.Token.TokenList<FieldDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> targetTypeMatcher) {
            return new ByteCodeElement.Token.TokenList<FieldDescription.Token>(Collections.<FieldDescription.Token>emptyList());
        }
    }
}
