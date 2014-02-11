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
    }
}
