package net.bytebuddy.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A canonical representation of a Java method that is invoked via reflection which might not be available
 * on every Java virtual machine.
 */
public interface JavaMethod {

    /**
     * Checks if this method is invokable on the current version of the Java virtual machine.
     *
     * @return {@code true} if this method is invokable.
     */
    boolean isInvokable();

    /**
     * Invokes this method.
     *
     * @param instance The instance on which the method is to be invoked.
     * @param argument The arguments for this method.
     * @return The return value of the invoked method.
     */
    Object invoke(Object instance, Object... argument);

    /**
     * Invokes a static method.
     *
     * @param argument The arguments for the method.
     * @return The return value.
     */
    Object invokeStatic(Object... argument);

    /**
     * Represents a method that cannot be invoked.
     */
    enum ForUnavailableMethod implements JavaMethod {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean isInvokable() {
            return false;
        }

        @Override
        public Object invoke(Object instance, Object... argument) {
            throw new IllegalStateException("Java language feature is not available for current virtual machine");
        }

        @Override
        public Object invokeStatic(Object... argument) {
            return invoke(null, argument);
        }


        @Override
        public String toString() {
            return "JavaMethod.ForUnavailableMethod." + name();
        }
    }

    /**
     * Represents a method that can be invoked.
     */
    class ForLoadedMethod implements JavaMethod {

        /**
         * The method to invoke.
         */
        private final Method method;

        /**
         * Creates a new representation for a loaded method.
         *
         * @param method The method to invoke.
         */
        public ForLoadedMethod(Method method) {
            this.method = method;
        }

        @Override
        public boolean isInvokable() {
            return true;
        }

        @Override
        public Object invoke(Object instance, Object... argument) {
            try {
                return method.invoke(instance, argument);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot invoke dynamically-linked method", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Exception when invoking method", e.getCause());
            }
        }

        @Override
        public Object invokeStatic(Object... argument) {
            return invoke(null, argument);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && method.equals(((ForLoadedMethod) other).method);
        }

        @Override
        public int hashCode() {
            return method.hashCode();
        }

        @Override
        public String toString() {
            return "JavaMethod.ForLoadedMethod{" +
                    "method=" + method +
                    '}';
        }
    }
}
