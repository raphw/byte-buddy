package net.bytebuddy.utility;

import java.util.Arrays;

public enum PropertyDispatcher {

    BOOLEAN_ARRAY {
        @Override
        public String toString(Object value) {
            return Arrays.toString((boolean[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((boolean[]) value);
        }
    },

    BYTE_ARRAY {
        @Override
        public String toString(Object value) {
            return Arrays.toString((byte[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((byte[]) value);
        }
    },

    SHORT_ARRAY {
        @Override
        public String toString(Object value) {
            return Arrays.toString((short[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((short[]) value);
        }
    },

    CHARACTER_ARRAY {
        @Override
        public String toString(Object value) {
            return Arrays.toString((char[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((char[]) value);
        }
    },

    INTEGER_ARRAY {
        @Override
        public String toString(Object value) {
            return Arrays.toString((int[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((int[]) value);
        }
    },

    LONG_ARRAY {
        @Override
        public String toString(Object value) {
            return Arrays.toString((long[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((long[]) value);
        }
    },

    FLOAT_ARRAY {
        @Override
        public String toString(Object value) {
            return Arrays.toString((float[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((float[]) value);
        }
    },

    DOUBLE_ARRAY {
        @Override
        public String toString(Object value) {
            return Arrays.toString((double[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((double[]) value);
        }
    },

    REFERENCE_ARRAY {
        @Override
        public String toString(Object value) {
            return Arrays.toString((Object[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((Object[]) value);
        }
    },

    NON_ARRAY {
        @Override
        public String toString(Object value) {
            return value.toString();
        }

        @Override
        public int hashCode(Object value) {
            return value.hashCode();
        }
    };

    public static PropertyDispatcher of(Class<?> type) {
        if (type == boolean[].class) {
            return BOOLEAN_ARRAY;
        } else if (type == byte[].class) {
            return BYTE_ARRAY;
        } else if (type == short[].class) {
            return SHORT_ARRAY;
        } else if (type == char[].class) {
            return CHARACTER_ARRAY;
        } else if (type == int[].class) {
            return INTEGER_ARRAY;
        } else if (type == long[].class) {
            return LONG_ARRAY;
        } else if (type == float[].class) {
            return FLOAT_ARRAY;
        } else if (type == double[].class) {
            return DOUBLE_ARRAY;
        } else if (Object[].class.isAssignableFrom(type)) {
            return REFERENCE_ARRAY;
        } else {
            return NON_ARRAY;
        }
    }

    public abstract String toString(Object value);

    public abstract int hashCode(Object value);
}
