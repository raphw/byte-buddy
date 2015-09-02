package net.bytebuddy.implementation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations of this interface explicitly initialize a loaded type. Usually, such implementations inject runtime
 * context into an instrumented type which cannot be defined by the means of the Java class file format.
 */
public interface LoadedTypeInitializer {

    /**
     * Callback that is invoked on the creation of an instrumented type. If the loaded type initializer is alive, this
     * method should be implemented empty instead of throwing an exception.
     *
     * @param type The manifestation of the instrumented type.
     */
    void onLoad(Class<?> type);

    /**
     * Indicates if this initializer is alive and needs to be invoked. This is only meant as a mark. A loaded type
     * initializer that is not alive might still be called and must therefore not throw an exception but rather
     * provide an empty implementation.
     *
     * @return {@code true} if this initializer is alive.
     */
    boolean isAlive();

    /**
     * A loaded type initializer that does not do anything.
     */
    enum NoOp implements LoadedTypeInitializer {

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

        @Override
        public String toString() {
            return "LoadedTypeInitializer.NoOp." + name();
        }
    }

    /**
     * A type initializer for setting a value for a static field.
     *
     * @param <T> The type of the value that is set as a value to the field.
     */
    class ForStaticField<T> implements LoadedTypeInitializer, Serializable {
        /**
         * This class's serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * A value for accessing a static field.
         */
        private static final Object STATIC_FIELD = null;

        /**
         * The name of the field.
         */
        private final String fieldName;

        /**
         * The value of the field.
         */
        private final T value;

        /**
         * Determines if the field needs to be made accessible for setting it.
         */
        private final boolean makeAccessible;

        /**
         * Creates a new {@link LoadedTypeInitializer} for setting a static field.
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

        /**
         * Creates a {@link LoadedTypeInitializer} for given field name and value where the
         * field is accessible by reflection.
         *
         * @param fieldName The name of the field.
         * @param value     The value to set.
         * @return A corresponding {@link LoadedTypeInitializer}.
         */
        public static LoadedTypeInitializer accessible(String fieldName, Object value) {
            return new ForStaticField<Object>(fieldName, value, false);
        }

        /**
         * Creates a {@link LoadedTypeInitializer} for given field name and value where the
         * field is not accessible by reflection and needs to be prepared accordingly.
         *
         * @param fieldName The name of the field.
         * @param value     The value to set.
         * @return A corresponding {@link LoadedTypeInitializer}.
         */
        public static LoadedTypeInitializer nonAccessible(String fieldName, Object value) {
            return new ForStaticField<Object>(fieldName, value, true);
        }

        @Override
        public void onLoad(Class<?> type) {
            try {
                Field field = type.getDeclaredField(fieldName);
                if (makeAccessible) {
                    AccessController.doPrivileged(new FieldAccessibilityAction(field));
                }
                field.set(STATIC_FIELD, value);
            } catch (IllegalAccessException exception) {
                throw new IllegalArgumentException("Cannot access " + fieldName + " from " + type, exception);
            } catch (NoSuchFieldException exception) {
                throw new IllegalStateException("There is no field " + fieldName + " defined on " + type, exception);
            }
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ForStaticField that = (ForStaticField) other;
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
            return "LoadedTypeInitializer.ForStaticField{" +
                    "fieldName='" + fieldName + '\'' +
                    ", value=" + value +
                    ", makeAccessible=" + makeAccessible +
                    '}';
        }

        /**
         * Sets a field accessible.
         */
        protected static class FieldAccessibilityAction implements PrivilegedAction<Void> {

            /**
             * Indicates that this action returns nothing.
             */
            private static final Void NOTHING = null;

            /**
             * The field to make accessible.
             */
            private final Field field;

            /**
             * Creates a new field accessibility action.
             * @param field The field to make accessible.
             */
            protected FieldAccessibilityAction(Field field) {
                this.field = field;
            }

            @Override
            public Void run() {
                field.setAccessible(true);
                return NOTHING;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                FieldAccessibilityAction that = (FieldAccessibilityAction) other;
                return field.equals(that.field);
            }

            @Override
            public int hashCode() {
                return field.hashCode();
            }

            @Override
            public String toString() {
                return "LoadedTypeInitializer.ForStaticField.FieldAccessibilityAction{" +
                        "field=" + field +
                        '}';
            }
        }
    }

    /**
     * A compound loaded type initializer that combines several type initializers.
     */
    class Compound implements LoadedTypeInitializer, Serializable {

        /**
         * This class's serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * The loaded type initializers that are represented by this compound type initializer.
         */
        private final LoadedTypeInitializer[] loadedTypeInitializer;

        /**
         * Creates a new compound loaded type initializer.
         *
         * @param loadedTypeInitializer A number of loaded type initializers in their invocation order.
         */
        public Compound(LoadedTypeInitializer... loadedTypeInitializer) {
            this.loadedTypeInitializer = loadedTypeInitializer;
        }

        /**
         * Creates a new compound loaded type initializer.
         *
         * @param loadedTypeInitializers A number of loaded type initializers in their invocation order.
         */
        public Compound(List<? extends LoadedTypeInitializer> loadedTypeInitializers) {
            this.loadedTypeInitializer = loadedTypeInitializers.toArray(new LoadedTypeInitializer[loadedTypeInitializers.size()]);
        }

        @Override
        public void onLoad(Class<?> type) {
            for (LoadedTypeInitializer loadedTypeInitializer : this.loadedTypeInitializer) {
                loadedTypeInitializer.onLoad(type);
            }
        }

        @Override
        public boolean isAlive() {
            for (LoadedTypeInitializer loadedTypeInitializer : this.loadedTypeInitializer) {
                if (loadedTypeInitializer.isAlive()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(loadedTypeInitializer, ((Compound) other).loadedTypeInitializer);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(loadedTypeInitializer);
        }

        @Override
        public String toString() {
            return "LoadedTypeInitializer.Compound{loadedTypeInitializer=" + Arrays.toString(loadedTypeInitializer) + '}';
        }
    }
}
