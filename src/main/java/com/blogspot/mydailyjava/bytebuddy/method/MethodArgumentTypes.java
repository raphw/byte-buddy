package com.blogspot.mydailyjava.bytebuddy.method;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MethodArgumentTypes implements Iterable<String> {

    private static final char OBJECT = 'L', ARRAY = '[', OBJECT_TYPE_DELIMITER = ';', ARGUMENTS_DELIMITER = ')';

    private class MethodArgumentTypeNameIterator implements Iterator<String> {

        private int descIndex = 0;

        @Override
        public boolean hasNext() {
            return descIndex != desc.length();
        }

        @Override
        public String next() {
            int nextIndex = findNextIndex();
            try {
                return desc.substring(descIndex, nextIndex);
            } finally {
                descIndex = nextIndex;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private int findNextIndex() {
            int nextIndex;
            switch (desc.charAt(descIndex)) {
                case OBJECT:
                    nextIndex = endOfObject(desc, descIndex);
                    break;
                case ARRAY:
                    nextIndex = endOfArray(desc, descIndex);
                    break;
                /*
                case FLOAT:
                case LONG:
                case DOUBLE:
                case INT:
                case BOOLEAN:
                case CHAR:
                case SHORT:
                case BYTE:
                */
                default:
                    nextIndex = descIndex + 1;
            }
            return nextIndex;
        }

        private int endOfObject(String value, int index) {
            return value.indexOf(OBJECT_TYPE_DELIMITER, index + 1) + 1;
        }

        private int endOfArray(String value, int index) {
            switch (value.charAt(index + 1)) {
                case OBJECT:
                    return endOfObject(value, index + 1);
                case ARRAY:
                    return endOfArray(value, index + 1);
                /*
                case FLOAT:
                case LONG:
                case DOUBLE:
                case INT:
                case BOOLEAN:
                case CHAR:
                case SHORT:
                case BYTE:
                */
                default:
                    return index + 1;
            }
        }

    }

    public static List<String> listNAmes(String desc) {
        List<String> names = new ArrayList<String>();
        for (String typeName : new MethodArgumentTypes(desc)) {
            names.add(typeName);
        }
        return names;
    }

    private final String desc;

    public MethodArgumentTypes(String desc) {
        this.desc = desc.substring(1, desc.lastIndexOf(ARGUMENTS_DELIMITER));
    }

    @Override
    public Iterator<String> iterator() {
        return new MethodArgumentTypeNameIterator();
    }
}
