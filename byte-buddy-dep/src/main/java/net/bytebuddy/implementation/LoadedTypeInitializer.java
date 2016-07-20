package net.bytebuddy.implementation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.utility.privilege.SetAccessibleAction;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
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
     */
    class ForStaticField implements LoadedTypeInitializer, Serializable {

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
        private final Object value;

        /**
         * Creates a new {@link LoadedTypeInitializer} for setting a static field.
         *
         * @param fieldName the name of the field.
         * @param value     The value to be set.
         */
        protected ForStaticField(String fieldName, Object value) {
            this.fieldName = fieldName;
            this.value = value;
        }

        @Override
        public void onLoad(Class<?> type) {
            try {
                Field field = type.getDeclaredField(fieldName);
                if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
                    AccessController.doPrivileged(new SetAccessibleAction<Field>(field));
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
            return fieldName.equals(that.fieldName)
                    && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            int result = fieldName.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "LoadedTypeInitializer.ForStaticField{" +
                    "fieldName='" + fieldName + '\'' +
                    ", value=" + value +
                    '}';
        }
    }

    /**
     * A compound loaded type initializer that combines several type initializers.
     */
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Serialization is considered opt-in for a rare use case")
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
