package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.method.JavaMethod;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

public class MethodExtractionTest {

    @SuppressWarnings("unused")
    public static interface Foo {

        void foo();
    }

    @SuppressWarnings("unused")
    public static interface Bar extends Foo {

        void bar();
    }

    @SuppressWarnings("unused")
    public static class Baz {

        public void baz() {
            /* empty */
        }
    }

    @Test
    public void testSingleExtraction() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<JavaMethod> methods = methodExtraction.extract(Object.class).asList();
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Object.class)));
    }

    @Test
    public void testInheritedExtraction() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<JavaMethod> methods = methodExtraction.extract(Baz.class).asList();
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length + Baz.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Object.class)));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Baz.class)));
    }

    @Test
    public void getInterfaceOnlyExtraction() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<JavaMethod> methods = methodExtraction.extract(Foo.class).asList();
        assertThat(methods.size(), is(Foo.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Foo.class)));
    }

    @Test
    public void getSingleInterfaceExtraction() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<JavaMethod> methods = methodExtraction.extract(Object.class).appendInterface(Foo.class).asList();
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length + Foo.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Object.class)));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Foo.class)));
    }

    @Test
    public void getInheritedInterfaceExtraction() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<JavaMethod> methods = methodExtraction.extract(Object.class).appendInterface(Bar.class).asList();
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length + Foo.class.getDeclaredMethods().length + Bar.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Object.class)));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Foo.class)));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Bar.class)));
    }

    @Test
    public void getInheritedInterfaceExtractionWithMatcher() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.declaredIn(Foo.class));
        List<JavaMethod> methods = methodExtraction.extract(Object.class).appendInterface(Bar.class).asList();
        assertThat(methods.size(), is(Foo.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Foo.class)));
    }

    private static JavaMethod[] getDeclaredJavaMethods(Class<?> type) {
        JavaMethod[] javaMethods = new JavaMethod[type.getDeclaredMethods().length];
        int i = 0;
        for(Method method : type.getDeclaredMethods()) {
            javaMethods[i++] = new JavaMethod.ForMethod(method);
        }
        return javaMethods;
    }
}
