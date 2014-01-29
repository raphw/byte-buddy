package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;

public class MethodExtractionTest {

    private static final String TO_STRING_METHOD_NAME = "toString";

    @Test
    public void testNoOpExtraction() throws Exception {
        assertThat(MethodExtraction.matching(any()).asList().size(), is(0));
    }

    @Test
    public void testMethodExtraction() throws Exception {
        List<MethodDescription> methodDescriptions = MethodExtraction.matching(isMethod())
                .appendUniqueDescriptorsFrom(Object.class).asList();
        assertThat(methodDescriptions.size(), is(getDeclaredJavaMethods(Object.class).length));
        assertThat(methodDescriptions, hasItems(getDeclaredJavaMethods(Object.class)));
    }

    @Test
    public void testConstructorExtraction() throws Exception {
        List<MethodDescription> methodDescriptions = MethodExtraction.matching(isConstructor())
                .appendUniqueDescriptorsFrom(Object.class).asList();
        assertThat(methodDescriptions.size(), is(getDeclaredJavaConstructors(Object.class).length));
        assertThat(methodDescriptions, hasItems(getDeclaredJavaConstructors(Object.class)));
    }

    @Test
    public void testEmptyExtraction() throws Exception {
        List<MethodDescription> methodDescriptions = MethodExtraction.matching(none())
                .appendUniqueDescriptorsFrom(Object.class).asList();
        assertThat(methodDescriptions.size(), is(0));
    }

    @Test
    public void testSpecificExtraction() throws Exception {
        List<MethodDescription> methodDescriptions = MethodExtraction.matching(named(TO_STRING_METHOD_NAME))
                .appendUniqueDescriptorsFrom(Object.class).asList();
        assertThat(methodDescriptions.size(), is(1));
        assertThat(methodDescriptions.get(0), equalTo((MethodDescription) new MethodDescription.ForMethod(
                Object.class.getDeclaredMethod(TO_STRING_METHOD_NAME))));
    }

    @SuppressWarnings("unused")
    public static class Foo {

        public Foo(Object o) {
            /* empty */
        }

        public void foo() {
            /* empty */
        }

    }

    @SuppressWarnings("unused")
    public static class Bar extends Foo {

        public Bar() {
            super(null);
        }

        public void bar() {
            /* empty */
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    @Test
    public void testSubclassExtraction() throws Exception {
        List<MethodDescription> methodDescriptions = MethodExtraction.matching(any())
                .appendUniqueDescriptorsFrom(Bar.class).asList();
        MethodDescription[] expectedUserTypeMethods = getDeclaredJavaMethods(Foo.class, Bar.class);
        MethodDescription[] expectedObjectTypeMethods = getDeclaredJavaMethods(not(named(TO_STRING_METHOD_NAME)), Object.class);
        MethodDescription[] expectedConstructors = getDeclaredJavaConstructors(Bar.class);
        assertThat(methodDescriptions.size(), is(expectedConstructors.length
                + expectedUserTypeMethods.length
                + expectedObjectTypeMethods.length));
        assertThat(methodDescriptions, hasItems(expectedConstructors));
        assertThat(methodDescriptions, hasItems(expectedUserTypeMethods));
        assertThat(methodDescriptions, hasItems(expectedObjectTypeMethods));
    }

    @Test
    public void testFiltering() throws Exception {
        List<MethodDescription> methodDescriptions = MethodExtraction.matching(any())
                .appendUniqueDescriptorsFrom(Object.class).filter(named(TO_STRING_METHOD_NAME)).asList();
        assertThat(methodDescriptions.size(), is(1));
        assertThat(methodDescriptions.get(0), equalTo((MethodDescription) new MethodDescription.ForMethod(
                Object.class.getDeclaredMethod(TO_STRING_METHOD_NAME))));
    }

    @Test
    public void testAppending() throws Exception {
        List<MethodDescription> methodDescriptions = MethodExtraction.matching(not(named(TO_STRING_METHOD_NAME)))
                .appendUniqueDescriptorsFrom(Foo.class).appendUniqueDescriptorsFrom(Bar.class).asList();
        MethodDescription[] expectedMethods = getDeclaredJavaMethods(not(named(TO_STRING_METHOD_NAME)), Foo.class, Bar.class, Object.class);
        MethodDescription[] expectedConstructors = getDeclaredJavaConstructors(Foo.class, Bar.class);
        assertThat(methodDescriptions.size(), is(expectedMethods.length + expectedConstructors.length));
        assertThat(methodDescriptions, hasItems(expectedMethods));
        assertThat(methodDescriptions, hasItems(expectedConstructors));
    }

    private static MethodDescription[] getDeclaredJavaMethods(Class<?>... types) {
        return getDeclaredJavaMethods(any(), types);
    }

    private static MethodDescription[] getDeclaredJavaMethods(MethodMatcher methodMatcher, Class<?>... types) {
        List<MethodDescription> methodDescriptions = new LinkedList<MethodDescription>();
        for (Class<?> type : types) {
            for (Method method : type.getDeclaredMethods()) {
                MethodDescription methodDescription = new MethodDescription.ForMethod(method);
                if (methodMatcher.matches(methodDescription)) {
                    methodDescriptions.add(methodDescription);
                }
            }
        }
        return methodDescriptions.toArray(new MethodDescription[methodDescriptions.size()]);
    }

    private static MethodDescription[] getDeclaredJavaConstructors(Class<?>... types) {
        return getDeclaredJavaConstructors(any(), types);
    }

    private static MethodDescription[] getDeclaredJavaConstructors(MethodMatcher methodMatcher, Class<?>... types) {
        List<MethodDescription> methodDescriptions = new LinkedList<MethodDescription>();
        for (Class<?> type : types) {
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                MethodDescription methodDescription = new MethodDescription.ForConstructor(constructor);
                if (methodMatcher.matches(methodDescription)) {
                    methodDescriptions.add(methodDescription);
                }
            }
        }
        return methodDescriptions.toArray(new MethodDescription[methodDescriptions.size()]);
    }
}
