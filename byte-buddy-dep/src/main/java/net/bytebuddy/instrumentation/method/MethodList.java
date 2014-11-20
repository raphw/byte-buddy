package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementations represent a list of method descriptions.
 */
public interface MethodList extends FilterableList<MethodDescription, MethodList> {

    /**
     * Returns a new list that only includes the methods that are matched by the given method matcher.
     *
     * @param methodMatcher A filter applied to this list.
     * @return a new list where all methods match the given {@code methodMatcher}.
     */
    MethodList filter(MethodMatcher methodMatcher);

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
            methods = type.getDeclaredMethods();
            constructors = type.getDeclaredConstructors();
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
        public MethodList filter(MethodMatcher methodMatcher) {
            List<MethodDescription> result = new ArrayList<MethodDescription>(size());
            for (Method method : methods) {
                MethodDescription methodDescription = new MethodDescription.ForLoadedMethod(method);
                if (methodMatcher.matches(methodDescription)) {
                    result.add(methodDescription);
                }
            }
            for (Constructor<?> constructor : constructors) {
                MethodDescription methodDescription = new MethodDescription.ForLoadedConstructor(constructor);
                if (methodMatcher.matches(methodDescription)) {
                    result.add(methodDescription);
                }
            }
            return new Explicit(result);
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
        public MethodList filter(MethodMatcher methodMatcher) {
            List<MethodDescription> result = new ArrayList<MethodDescription>(methodDescriptions.size());
            for (MethodDescription methodDescription : methodDescriptions) {
                if (methodMatcher.matches(methodDescription)) {
                    result.add(methodDescription);
                }
            }
            return new Explicit(result);
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

        @Override
        public MethodList filter(MethodMatcher methodMatcher) {
            return this;
        }
    }
}
