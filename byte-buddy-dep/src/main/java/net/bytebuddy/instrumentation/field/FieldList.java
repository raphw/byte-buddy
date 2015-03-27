package net.bytebuddy.instrumentation.field;

import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Implementations represent a list of field descriptions.
 */
public interface FieldList extends FilterableList<FieldDescription, FieldList> {

    /**
     * An implementation of a field list for an array of loaded fields.
     */
    class ForLoadedField extends AbstractBase<FieldDescription, FieldList> implements FieldList {

        /**
         * The loaded fields this field list represents.
         */
        private final Field[] field;

        /**
         * Creates a new immutable field list that represents an array of loaded field.
         *
         * @param field An array of fields to be represented by this field list.
         */
        public ForLoadedField(Field... field) {
            this.field = field;
        }

        @Override
        public FieldDescription get(int index) {
            return new FieldDescription.ForLoadedField(field[index]);
        }

        @Override
        public int size() {
            return field.length;
        }

        @Override
        protected FieldList wrap(List<FieldDescription> values) {
            return new Explicit(values);
        }
    }

    /**
     * A wrapper implementation of a field list for a given list of field descriptions.
     */
    class Explicit extends AbstractBase<FieldDescription, FieldList> implements FieldList {

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

        @Override
        protected FieldList wrap(List<FieldDescription> values) {
            return new Explicit(values);
        }
    }

    /**
     * An implementation of an empty field list.
     */
    class Empty extends FilterableList.Empty<FieldDescription, FieldList> implements FieldList {

    }
}
