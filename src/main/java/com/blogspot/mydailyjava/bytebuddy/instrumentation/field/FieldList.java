package com.blogspot.mydailyjava.bytebuddy.instrumentation.field;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implementations represent a list of field descriptions.
 */
public interface FieldList extends List<FieldDescription> {

    /**
     * An implementation of a field list for an array of loaded fields.
     */
    static class ForLoadedField extends AbstractList<FieldDescription> implements FieldList {

        private final Field[] field;

        /**
         * Creates a new immutable field list that represents an array of loaded field.
         *
         * @param field An array of fields to be represented by this field list.
         */
        public ForLoadedField(Field[] field) {
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
        public FieldDescription named(String fieldName) {
            for (Field field : this.field) {
                if (field.getName().equals(fieldName)) {
                    return new FieldDescription.ForLoadedField(field);
                }
            }
            throw new IllegalArgumentException("Expected to find a field " + fieldName);
        }
    }

    /**
     * A wrapper implementation of a field list for a given list of field descriptions.
     */
    static class Explicit extends AbstractList<FieldDescription> implements FieldList {

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
        public FieldDescription named(String fieldName) {
            for (FieldDescription fieldDescription : fieldDescriptions) {
                if (fieldDescription.getInternalName().equals(fieldName)) {
                    return fieldDescription;
                }
            }
            throw new IllegalArgumentException("Expected to find a field " + fieldName);
        }
    }

    /**
     * An implementation of an empty field list.
     */
    static class Empty extends AbstractList<FieldDescription> implements FieldList {

        @Override
        public FieldDescription get(int index) {
            throw new NoSuchElementException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public FieldDescription named(String fieldName) {
            throw new IllegalArgumentException("Expected to find a field " + fieldName + " but found none");
        }
    }

    /**
     * Identifies a single field description in this list that is named {@code fieldName} and returns this field
     * description. If no such field is in the list, an exception is thrown.
     *
     * @param fieldName The internalName of the required field.
     * @return The field named {@code fieldName}.
     */
    FieldDescription named(String fieldName);
}
