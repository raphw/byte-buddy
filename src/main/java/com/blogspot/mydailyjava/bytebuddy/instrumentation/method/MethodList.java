package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Implementations represent a list of method descriptions.
 */
public interface MethodList extends List<MethodDescription> {

    /**
     * A method list implementation that returns all loaded byte code methods (methods and constructors) that
     * are declared for a given type.
     */
    static class ForLoadedType extends AbstractList<MethodDescription> implements MethodList {

        private final Class<?> type;

        /**
         * Creates a new list for a loaded type. Method descriptions are created on demand.
         *
         * @param type The type of interest.
         */
        public ForLoadedType(Class<?> type) {
            this.type = type;
        }

        @Override
        public MethodDescription get(int index) {
            if (index < type.getDeclaredConstructors().length) {
                return new MethodDescription.ForConstructor(type.getDeclaredConstructors()[index]);
            } else {
                return new MethodDescription.ForMethod(type.getDeclaredMethods()[type.getDeclaredConstructors().length + index]);
            }
        }

        @Override
        public int size() {
            return type.getDeclaredMethods().length + type.getDeclaredConstructors().length;
        }

        @Override
        public MethodList filter(MethodMatcher methodMatcher) {
            List<MethodDescription> result = new ArrayList<MethodDescription>(size());
            for (Method method : type.getDeclaredMethods()) {
                MethodDescription methodDescription = new MethodDescription.ForMethod(method);
                if (methodMatcher.matches(methodDescription)) {
                    result.add(methodDescription);
                }
            }
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                MethodDescription methodDescription = new MethodDescription.ForConstructor(constructor);
                if (methodMatcher.matches(methodDescription)) {
                    result.add(methodDescription);
                }
            }
            return new Explicit(result);
        }

        @Override
        public MethodDescription getOnly() {
            if (size() == 1) {
                return get(0);
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public MethodList subList(int fromIndex, int toIndex) {
            return new Explicit(super.subList(fromIndex, toIndex));
        }
    }

    static class Explicit extends AbstractList<MethodDescription> implements MethodList {

        private final List<? extends MethodDescription> methodDescriptions;

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
        public MethodDescription getOnly() {
            if (methodDescriptions.size() == 1) {
                return methodDescriptions.get(0);
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public MethodList subList(int fromIndex, int toIndex) {
            return new Explicit(super.subList(fromIndex, toIndex));
        }
    }

    public class Empty extends AbstractList<MethodDescription> implements MethodList {

        @Override
        public MethodDescription get(int index) {
            throw new NoSuchElementException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public MethodList filter(MethodMatcher methodMatcher) {
            return this;
        }

        @Override
        public MethodDescription getOnly() {
            throw new IllegalStateException();
        }

        @Override
        public MethodList subList(int fromIndex, int toIndex) {
            if (fromIndex == toIndex && toIndex == 0) {
                return this;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    MethodList filter(MethodMatcher methodMatcher);

    @Override
    MethodList subList(int fromIndex, int toIndex);

    MethodDescription getOnly();
}
