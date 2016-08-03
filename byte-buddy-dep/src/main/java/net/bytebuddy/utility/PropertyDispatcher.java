package net.bytebuddy.utility;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;

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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((boolean[]) first, (boolean[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            boolean[] castValue = (boolean[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((byte[]) first, (byte[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            byte[] castValue = (byte[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((short[]) first, (short[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            short[] castValue = (short[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((char[]) first, (char[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            char[] castValue = (char[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((int[]) first, (int[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            int[] castValue = (int[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((long[]) first, (long[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            long[] castValue = (long[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((float[]) first, (float[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            float[] castValue = (float[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((double[]) first, (double[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            double[] castValue = (double[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
        }
    },

    /**
     * A property dispatcher for a {@code Class} value. This value requires special handling since Java 9.
     */
    TYPE_LOADED {
        @Override
        public String toString(Object value) {
            return TypeRenderer.CURRENT.render(value);
        }

        @Override
        public int hashCode(Object value) {
            return value.hashCode();
        }

        @Override
        protected boolean doEquals(Object first, Object second) {
            return first.equals(second);
        }

        @Override
        public <T> T conditionalClone(T value) {
            return value;
        }
    },

    /**
     * A property dispatcher for a {@code Class[]} value. This value requires special handling since Java 9.
     */
    TYPE_LOADED_ARRAY {
        @Override
        public String toString(Object value) {
            return TypeRenderer.CURRENT.render((Object[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((Object[]) value);
        }

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((Object[]) first, (Object[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            Object[] castValue = (Object[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
        }
    },

    /**
     * A property dispatcher for a {@code TypeDescription} value. This value requires special handling since Java 9.
     */
    TYPE_DESCRIBED {
        @Override
        public String toString(Object value) {
            return TypeRenderer.CURRENT.render(value);
        }

        @Override
        public int hashCode(Object value) {
            return value.hashCode();
        }

        @Override
        protected boolean doEquals(Object first, Object second) {
            return first.equals(second);
        }

        @Override
        public <T> T conditionalClone(T value) {
            return value;
        }
    },

    /**
     * A property dispatcher for a {@code TypeDescription[]} value. This value requires special handling since Java 9.
     */
    TYPE_DESCRIBED_ARRAY {
        @Override
        public String toString(Object value) {
            return TypeRenderer.CURRENT.render((Object[]) value);
        }

        @Override
        public int hashCode(Object value) {
            return Arrays.hashCode((Object[]) value);
        }

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((Object[]) first, (Object[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            Object[] castValue = (Object[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return Arrays.equals((Object[]) first, (Object[]) second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            Object[] castValue = (Object[]) value;
            return castValue.length == 0 ? value : (T) castValue.clone();
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

        @Override
        protected boolean doEquals(Object first, Object second) {
            return first.equals(second);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T conditionalClone(T value) {
            return value;
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
        } else if (type == Class.class) {
            return TYPE_LOADED;
        } else if (type == Class[].class) {
            return TYPE_LOADED_ARRAY;
        } else if (TypeDescription.class.isAssignableFrom(type)) {
            return TYPE_DESCRIBED;
        } else if (TypeDescription[].class.isAssignableFrom(type)) {
            return TYPE_DESCRIBED_ARRAY;
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

    /**
     * Compares if two values are equal.
     *
     * @param first  The first value which must be of the type of this property dispatcher and not {@code null}.
     * @param second Another value which might or might not be of the type of this property dispatcher.
     * @return {@code true} if both values are equal.
     */
    public boolean equals(Object first, Object second) {
        return second != null && (first == second || PropertyDispatcher.of(second.getClass()) == this && doEquals(first, second));
    }

    /**
     * Compares two values that are both non-null and of the same type as the array.
     *
     * @param first  The first value of this property dispatcher's type.
     * @param second The second value of this property dispatcher's type.
     * @return {@code true} if both values are equal.
     */
    protected abstract boolean doEquals(Object first, Object second);

    /**
     * Creates a shallow copy of an array but returns non-array types as such.
     *
     * @param value The value to attempt to clone.
     * @param <T>   The type of the value.
     * @return A shallow copy of an array or the input value if it does not represent an array.
     */
    public abstract <T> T conditionalClone(T value);

    @Override
    public String toString() {
        return "PropertyDispatcher." + name();
    }

    /**
     * A delegate for rendering a {@link Class} or {@link TypeDescription}. Starting with Java 9, such values are enclosed
     * in curly braces and are rendered as class literals to better match the source code.
     */
    public enum TypeRenderer {

        /**
         * A type renderer for a legacy VM prior to Java 8.
         */
        FOR_LEGACY_VM('[', ']') {
            @Override
            public String render(Object type) {
                if (!(type instanceof Class || type instanceof TypeDescription)) {
                    throw new IllegalArgumentException("Unexpected type description: " + type);
                }
                return type.toString();
            }
        },

        /**
         * A type renderer for a VM of at least Java version 9.
         */
        FOR_JAVA9_CAPABLE_VM('{', '}') {
            @Override
            public String render(Object type) {
                String name;
                if (type instanceof Class) {
                    name = ((Class<?>) type).getName();
                } else if (type instanceof TypeDescription) {
                    name = ((TypeDescription) type).getName();
                } else {
                    throw new IllegalArgumentException("Unexpected type description: " + type);
                }
                return name + JAVA9_NAME_SUFFIX;
            }
        };

        /**
         * The class constant suffix for types from Java 9 and later.
         */
        private static final String JAVA9_NAME_SUFFIX = ".class";

        /**
         * The type renderer to be used on the current VM.
         */
        public static final TypeRenderer CURRENT = make();

        /**
         * Finds the type renderer for the current VM.
         *
         * @return The type renderer to be used on the current VM.
         */
        private static TypeRenderer make() {
            return ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V6).isAtLeast(ClassFileVersion.JAVA_V9)
                    ? FOR_JAVA9_CAPABLE_VM
                    : FOR_LEGACY_VM;
        }

        /**
         * The opening brace character.
         */
        private final char open;

        /**
         * The closing brace character.
         */
        private final char close;

        /**
         * Creates a new type renderer.
         *
         * @param open  The opening brace character.
         * @param close The closing brace character.
         */
        TypeRenderer(char open, char close) {
            this.open = open;
            this.close = close;
        }

        /**
         * Renders a {@link Class} or {@link TypeDescription} constant.
         *
         * @param type The type to be rendered.
         * @return The rendered string.
         */
        public abstract String render(Object type);

        /**
         * Renders an array of {@link Class} or {@link TypeDescription} constants.
         *
         * @param type The types to be rendered.
         * @return The rendered string.
         */
        public String render(Object[] type) {
            StringBuilder stringBuilder = new StringBuilder().append(open);
            boolean initial = true;
            for (Object aType : type) {
                stringBuilder.append(render(aType));
                if (initial) {
                    initial = false;
                } else {
                    stringBuilder.append(", ");
                }
            }
            return stringBuilder.append(close).toString();
        }

        /**
         * Returns the opening brace.
         * @return The opening brace.
         */
        public char getOpen() {
            return open;
        }

        /**
         * Returns the closing brace.
         * @return The closing brace.
         */
        public char getClose() {
            return close;
        }

        @Override
        public String toString() {
            return "PropertyDispatcher.TypeRenderer." + name();
        }
    }
}
