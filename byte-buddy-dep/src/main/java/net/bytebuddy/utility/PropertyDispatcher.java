package net.bytebuddy.utility;

import java.util.Arrays;

/**
 * A dispatcher for invoking {@link Object#toString()} and {@link Object#hashCode()} methods that are sensitive to
 * array values which need redirection to different specialized methods defined by {@link java.util.Arrays}.
 */
public enum PropertyDispatcher {

    /**
     * A property dispatcher for a {@code boolean[]} array.
     */
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

    /**
     * A property dispatcher for a {@code byte[]} array.
     */
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

    /**
     * A property dispatcher for a {@code short[]} array.
     */
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

    /**
     * A property dispatcher for a {@code char[]} array.
     */
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

    /**
     * A property dispatcher for a {@code int[]} array.
     */
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

    /**
     * A property dispatcher for a {@code long[]} array.
     */
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

    /**
     * A property dispatcher for a {@code float[]} array.
     */
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

    /**
     * A property dispatcher for a {@code double[]} array.
     */
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

    /**
     * A property dispatcher for any {@code Object[]} array.
     */
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

    /**
     * A property dispatcher for a non-array.
     */
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

    /**
     * Finds a property dispatcher for a given type.
     *
     * @param type The type for which a property dispatcher should be found.
     * @return A suitable property dispatcher.
     */
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

    /**
     * Computes a string representation for the given type.
     *
     * @param value The value onto which a specific {@code toString} method should be invoked.
     * @return The created string.
     */
    public abstract String toString(Object value);

    /**
     * Computes a hash code for the given type.
     *
     * @param value The value onto which a specific {@code hashCode} method should be invoked.
     * @return The created hash code.
     */
    public abstract int hashCode(Object value);
}
