package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations of this type explicitly initialize a type. Usually, such implementations inject context into an
 * instrumented type which cannot be defined by the means of the Java class file format.
 */
public interface TypeInitializer {

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

    /**
     * A type initializer that does not do anything.
     */
    static enum NoOp implements TypeInitializer, Serializable {
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
}
