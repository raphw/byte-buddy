package net.bytebuddy.instrumentation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations of this type explicitly initialize a type. Usually, such implementations inject context into an
 * instrumented type which cannot be defined by the means of the Java class file format.
 */
public interface TypeInitializer {

    /**
     * Callback that is invoked on the creation of an instrumented type. If the type initializer is alive, this
     * method should be implemented empty instead of throwing an exception.
     *
     * @param type The manifestation of the instrumented type.
     */
    void onLoad(Class<?> type);

    /**
     * Indicates if this initializer is alive and needs to be invoked.
     *
     * @return {@code true} if this initializer is alive.
     */
    boolean isAlive();

    /**
     * A type initializer that does not do anything.
     */
    static enum NoOp implements TypeInitializer {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void onLoad(Class<?> type) {
            /* do nothing */
        }

        @Override
        public boolean isAlive() {
            return false;
        }
    }

    /**
     * A type initializer for setting a value for a static field.
     *
     * @param <T> The type of the value that is set as a value to the field.
     */
    static class ForStaticField<T> implements TypeInitializer, Serializable {

        private static final Object STATIC_FIELD = null;

        /**
         * Creates a {@link net.bytebuddy.instrumentation.TypeInitializer} for given field name and value where the
         * field is accessible by reflection.
         *
         * @param fieldName The name of the field.
         * @param value     The value to set.
         * @return A corresponding {@link net.bytebuddy.instrumentation.TypeInitializer}.
         */
        public static TypeInitializer accessible(String fieldName, Object value) {
            return new ForStaticField<Object>(fieldName, value, false);
        }

        /**
         * Creates a {@link net.bytebuddy.instrumentation.TypeInitializer} for given field name and value where the
         * field is not accessible by reflection and needs to be prepared accordingly.
         *
         * @param fieldName The name of the field.
         * @param value     The value to set.
         * @return A corresponding {@link net.bytebuddy.instrumentation.TypeInitializer}.
         */
        public static TypeInitializer nonAccessible(String fieldName, Object value) {
            return new ForStaticField<Object>(fieldName, value, true);
        }

        private final String fieldName;
        private final T value;
        private final boolean makeAccessible;

        /**
         * Creates a new {@link net.bytebuddy.instrumentation.TypeInitializer} for setting a static field.
         *
         * @param fieldName      the name of the field.
         * @param value          The value to be set.
         * @param makeAccessible Whether the field should be made accessible.
         */
        protected ForStaticField(String fieldName, T value, boolean makeAccessible) {
            this.fieldName = fieldName;
            this.value = value;
            this.makeAccessible = makeAccessible;
        }

        @Override
        public void onLoad(Class<?> type) {
            try {
                Field field = type.getDeclaredField(fieldName);
                if (makeAccessible) {
                    field.setAccessible(true);
                }
                field.set(STATIC_FIELD, value);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(String.format("could not access field %s on %s", fieldName, type), e);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(String.format("There is no field %s defined for %s", fieldName, type), e);
            }
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ForStaticField that = (ForStaticField) o;
            return makeAccessible == that.makeAccessible
                    && fieldName.equals(that.fieldName)
                    && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            int result = fieldName.hashCode();
            result = 31 * result + value.hashCode();
            result = 31 * result + (makeAccessible ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TypeInitializer.ForStaticField{" +
                    "fieldName='" + fieldName + '\'' +
                    ", value=" + value +
                    ", makeAccessible=" + makeAccessible +
                    '}';
        }
    }

    /**
     * A compound type initializer that combines several type initializers.
     */
    static class Compound implements TypeInitializer, Serializable {

        private final TypeInitializer[] typeInitializer;

        /**
         * Creates a new compound type initializer.
         *
         * @param typeInitializer A number of type initializers in their invocation order.
         */
        public Compound(TypeInitializer... typeInitializer) {
            this.typeInitializer = typeInitializer;
        }

        /**
         * Creates a new compound type initializer.
         *
         * @param typeInitializers A number of type initializers in their invocation order.
         */
        public Compound(List<? extends TypeInitializer> typeInitializers) {
            this.typeInitializer = typeInitializers.toArray(new TypeInitializer[typeInitializers.size()]);
        }

        @Override
        public void onLoad(Class<?> type) {
            for (TypeInitializer typeInitializer : this.typeInitializer) {
                typeInitializer.onLoad(type);
            }
        }

        @Override
        public boolean isAlive() {
            for (TypeInitializer typeInitializer : this.typeInitializer) {
                if (typeInitializer.isAlive()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && Arrays.equals(typeInitializer, ((Compound) o).typeInitializer);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(typeInitializer);
        }

        @Override
        public String toString() {
            return "TypeInitializer.Compound{typeInitializer=" + Arrays.toString(typeInitializer) + '}';
        }
    }
}
