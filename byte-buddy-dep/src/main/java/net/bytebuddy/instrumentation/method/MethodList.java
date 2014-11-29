package net.bytebuddy.instrumentation.method;

import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Implementations represent a list of method descriptions.
 */
public interface MethodList extends FilterableList<MethodDescription, MethodList> {

    /**
     * A method list implementation that returns all loaded byte code methods (methods and constructors) that
     * are declared for a given type.
     */
    static class ForLoadedType extends AbstractBase<MethodDescription, MethodList> implements MethodList {

        /**
         * The loaded methods that are represented by this method list.
         */
        private final Method[] methods;

        /**
         * The loaded constructors that are represented by this method list.
         */
        private final Constructor<?>[] constructors;

        /**
         * Creates a new list for a loaded type. Method descriptions are created on demand.
         *
         * @param type The type to be represented by this method list.
         */
        public ForLoadedType(Class<?> type) {
            constructors = type.getDeclaredConstructors();
            methods = type.getDeclaredMethods();
        }

        /**
         *
         * @param methods
         * @param constructors
         */
        public ForLoadedType(Constructor<?>[] constructors, Method[] methods) {
            this.constructors = constructors;
            this.methods = methods;
        }

        @Override
        public MethodDescription get(int index) {
            if (index < constructors.length) {
                return new MethodDescription.ForLoadedConstructor(constructors[index]);
            } else {
                return new MethodDescription.ForLoadedMethod(methods[index - constructors.length]);
            }
        }

        @Override
        public int size() {
            return constructors.length + methods.length;
        }

        @Override
        protected MethodList wrap(List<MethodDescription> values) {
            return new Explicit(values);
        }
    }

    /**
     * A method list that is a wrapper for a given list of method descriptions.
     */
    static class Explicit extends AbstractBase<MethodDescription, MethodList> implements MethodList {

        /**
         * The list of methods that is represented by this method list.
         */
        private final List<? extends MethodDescription> methodDescriptions;

        /**
         * Creates a new wrapper for a given list of methods.
         *
         * @param methodDescriptions The underlying list of methods used for this method list.
         */
        public Explicit(List<? extends MethodDescription> methodDescriptions) {
            this.methodDescriptions = Collections.unmodifiableList(methodDescriptions);
        }

        @Override
        public MethodDescription get(int index) {
            return methodDescriptions.get(index);
        }

        @Override
        public int size() {
            return methodDescriptions.size();
        }

        @Override
        protected MethodList wrap(List<MethodDescription> values) {
            return new Explicit(values);
        }
    }

    /**
     * An implementation of an empty method list.
     */
    static class Empty extends FilterableList.Empty<MethodDescription, MethodList> implements MethodList {
    }
}
