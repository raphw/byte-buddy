package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public interface MethodList extends List<MethodDescription> {

    static class ForLoadedType extends AbstractList<MethodDescription> implements MethodList {

        private final Class<?> type;

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
    }

    MethodList filter(MethodMatcher methodMatcher);

    MethodDescription getOnly();
}
