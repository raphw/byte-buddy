package com.blogspot.mydailyjava.bytebuddy.instrumentation.field;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.List;
import java.util.NoSuchElementException;

public interface FieldList extends List<FieldDescription> {

    static class ForLoadedField extends AbstractList<FieldDescription> implements FieldList {

        private final Field[] field;

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
            throw new IllegalArgumentException();
        }
    }

    static class Explicit extends AbstractList<FieldDescription> implements FieldList {

        private final List<? extends FieldDescription> fieldDescriptions;

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
            throw new IllegalArgumentException();
        }
    }

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
            throw new IllegalStateException();
        }
    }

    FieldDescription named(String fieldName);
}
