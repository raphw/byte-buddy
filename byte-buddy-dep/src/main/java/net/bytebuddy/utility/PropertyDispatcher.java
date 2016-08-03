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
            return RenderingDispatcher.CURRENT.toSourceString((boolean[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((byte[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((short[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((char[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((int[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((long[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((float[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((double[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((Class<?>) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((Class<?>[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((TypeDescription) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((TypeDescription[]) value);
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
            return RenderingDispatcher.CURRENT.toSourceString((Object[]) value);
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

    public enum RenderingDispatcher {

        FOR_LEGACY_VM ('[', ']'){
            @Override
            protected String toSourceString(boolean[] value) {
                return Arrays.toString(value);
            }

            @Override
            protected String toSourceString(byte[] value) {
                return Arrays.toString(value);
            }

            @Override
            protected String toSourceString(char value) {
                return Character.toString(value);
            }

            @Override
            protected String toSourceString(char[] value) {
                return Arrays.toString(value);
            }

            @Override
            protected String toSourceString(short[] value) {
                return Arrays.toString(value);
            }

            @Override
            protected String toSourceString(int[] value) {
                return Arrays.toString(value);
            }

            @Override
            protected String toSourceString(long value) {
                return Long.toString(value);
            }

            @Override
            protected String toSourceString(long[] value) {
                return Arrays.toString(value);
            }

            @Override
            protected String toSourceString(float value) {
                return Float.toString(value);
            }

            @Override
            protected String toSourceString(float[] value) {
                return Arrays.toString(value);
            }

            @Override
            protected String toSourceString(double value) {
                return Double.toString(value);
            }

            @Override
            protected String toSourceString(double[] value) {
                return Arrays.toString(value);
            }

            @Override
            protected String toSourceString(Class<?> type) {
                return type.toString();
            }

            @Override
            protected String toSourceString(Class<?>[] type) {
                return Arrays.toString(type);
            }

            @Override
            protected String toSourceString(TypeDescription typeDescription) {
                return typeDescription.toString();
            }

            @Override
            protected String toSourceString(TypeDescription[] typeDescription) {
                return Arrays.toString(typeDescription);
            }

            @Override
            protected String toSourceString(Object[] value) {
                return Arrays.toString(value);
            }
        },

        FOR_JAVA9_CAPABLE_VM('{', '}') {
            @Override
            protected String toSourceString(boolean[] value) {
                String[] rendered = new String[value.length];
                for (int index = 0; index < value.length; index++) {
                    rendered[index] = toSourceString(value[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(byte[] value) {
                String[] rendered = new String[value.length];
                for (int index = 0; index < value.length; index++) {
                    rendered[index] = toSourceString(value[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(char value) {
                StringBuilder stringBuilder = new StringBuilder().append('\'');
                if (value == '\'') {
                    stringBuilder.append("\\'");
                } else {
                    stringBuilder.append(value);
                }
                return stringBuilder.append('\'').toString();
            }

            @Override
            protected String toSourceString(char[] value) {
                String[] rendered = new String[value.length];
                for (int index = 0; index < value.length; index++) {
                    rendered[index] = toSourceString(value[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(short[] value) {
                String[] rendered = new String[value.length];
                for (int index = 0; index < value.length; index++) {
                    rendered[index] = toSourceString(value[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(int[] value) {
                String[] rendered = new String[value.length];
                for (int index = 0; index < value.length; index++) {
                    rendered[index] = toSourceString(value[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(long value) {
                return Math.abs(value) <= Integer.MAX_VALUE
                        ? Long.toString(value)
                        : Long.toString(value) + "L";
            }

            @Override
            protected String toSourceString(long[] value) {
                String[] rendered = new String[value.length];
                for (int index = 0; index < value.length; index++) {
                    rendered[index] = toSourceString(value[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(float value) {
                if (Math.abs(value) <= Float.MAX_VALUE /* Float.isFinite(value) */)
                    return Float.toString(value) + "f";
                else if (Float.isInfinite(value)) {
                    return value < 0.0f ? "-1.0f/0.0f" : "1.0f/0.0f";
                } else {
                    return "0.0f/0.0f";
                }
            }

            @Override
            protected String toSourceString(float[] value) {
                String[] rendered = new String[value.length];
                for (int index = 0; index < value.length; index++) {
                    rendered[index] = toSourceString(value[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(double value) {
                if (Math.abs(value) <= Double.MAX_VALUE /* Double.isFinite(value) */)
                    return Double.toString(value);
                else if (Double.isInfinite(value)) {
                    return value < 0.0d ? "-1.0/0.0" : "1.0/0.0";
                } else {
                    return "0.0/0.0";
                }
            }

            @Override
            protected String toSourceString(double[] value) {
                String[] rendered = new String[value.length];
                for (int index = 0; index < value.length; index++) {
                    rendered[index] = toSourceString(value[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(Class<?> type) {
                return toSourceString(new TypeDescription.ForLoadedType(type));
            }

            @Override
            protected String toSourceString(Class<?>[] type) {
                String[] rendered = new String[type.length];
                for (int index = 0; index < type.length; index++) {
                    rendered[index] = toSourceString(type[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(TypeDescription typeDescription) {
                return typeDescription.getActualName() + ".class";
            }

            @Override
            protected String toSourceString(TypeDescription[] typeDescription) {
                String[] rendered = new String[typeDescription.length];
                for (int index = 0; index < typeDescription.length; index++) {
                    rendered[index] = toSourceString(typeDescription[index]);
                }
                return asString(rendered);
            }

            @Override
            protected String toSourceString(Object[] value) {
                String[] rendered = new String[value.length];
                for (int index = 0; index < value.length; index++) {
                    rendered[index] = toSourceString(value[index]);
                }
                return asString(rendered);
            }

            private String asString(String[] rendered) {
                if (rendered.length == 0) {
                    return "{}";
                }
                StringBuilder stringBuilder = new StringBuilder().append('{');
                boolean first = true;
                for (String value : rendered) {
                    if (first) {
                        first = false;
                    } else {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(value);
                }
                return stringBuilder.append('}').toString();
            }
        };

        public static final RenderingDispatcher CURRENT = ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V6).isAtLeast(ClassFileVersion.JAVA_V9)
                ? FOR_JAVA9_CAPABLE_VM
                : FOR_LEGACY_VM;

        private final char openingBrace;

        private final char closingBrace;

        RenderingDispatcher(char openingBrace, char closingBrace) {
            this.openingBrace = openingBrace;
            this.closingBrace = closingBrace;
        }

        public char getOpeningBrace() {
            return openingBrace;
        }

        public char getClosingBrace() {
            return closingBrace;
        }

        protected String toSourceString(boolean value) {
            return Boolean.toString(value);
        }

        protected abstract String toSourceString(boolean[] value);

        protected String toSourceString(byte value) {
            return Byte.toString(value);
        }

        protected abstract String toSourceString(byte[] value);

        protected abstract String toSourceString(char value);

        protected abstract String toSourceString(char[] value);

        protected String toSourceString(short value) {
            return Short.toString(value);
        }

        protected abstract String toSourceString(short[] value);

        protected String toSourceString(int value) {
            return Integer.toString(value);
        }

        protected abstract String toSourceString(int[] value);

        protected abstract String toSourceString(long value);

        protected abstract String toSourceString(long[] value);

        protected abstract String toSourceString(float value);

        protected abstract String toSourceString(float[] value);

        protected abstract String toSourceString(double value);

        protected abstract String toSourceString(double[] value);

        protected abstract String toSourceString(Class<?> type);

        protected abstract String toSourceString(Class<?>[] type);

        protected abstract String toSourceString(TypeDescription typeDescription);

        protected abstract String toSourceString(TypeDescription[] typeDescription);

        protected String toSourceString(Object value) {
            return value.toString();
        }

        protected abstract String toSourceString(Object[] value);

        @Override
        public String toString() {
            return "PropertyDispatcher.RenderingDispatcher." + name();
        }
    }
}
