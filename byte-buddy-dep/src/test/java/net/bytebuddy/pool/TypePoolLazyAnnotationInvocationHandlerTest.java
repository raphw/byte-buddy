package net.bytebuddy.pool;

import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypePoolLazyAnnotationInvocationHandlerTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypePool.LazyTypeDescription.AnnotationValue<?, ?> annotationValue, otherAnnotationValue;

    @Test(expected = AnnotationTypeMismatchException.class)
    public void testAnnotationTypeMismatchException() throws Exception {
        when(annotationValue.load(getClass().getClassLoader())).thenReturn(new Object());
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = IncompleteAnnotationException.class)
    public void testIncompleteAnnotationException() throws Exception {
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = EnumConstantNotPresentException.class)
    public void testEnumConstantNotPresentException() throws Exception {
        when(annotationValue.load(getClass().getClassLoader()))
                .thenThrow(new EnumConstantNotPresentException(Bar.class, FOO));
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = ClassNotFoundException.class)
    public void testClassNotFoundExceptionIsWrapped() throws Exception {
        when(annotationValue.load(getClass().getClassLoader())).thenThrow(new ClassNotFoundException());
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = RuntimeException.class)
    public void testOtherExceptionPassThrough() throws Exception {
        when(annotationValue.load(getClass().getClassLoader())).thenThrow(new RuntimeException());
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test
    public void testEqualsToDirectIsTrue() throws Throwable {
        when(annotationValue.load(getClass().getClassLoader())).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(FOO)}),
                is((Object) true));
    }

    @Test
    public void testEqualsToIndirectIsTrue() throws Throwable {
        when(annotationValue.load(getClass().getClassLoader())).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, new ExplicitFoo(FOO))}),
                is((Object) true));
    }

    @Test
    public void testEqualsToOtherHandlerIsTrue() throws Throwable {
        when(annotationValue.load(getClass().getClassLoader())).thenReturn(FOO);
        InvocationHandler invocationHandler = new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue));
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, invocationHandler)}),
                is((Object) true));
    }

    @Test
    public void testEqualsToDirectIsFalse() throws Throwable {
        when(annotationValue.load(getClass().getClassLoader())).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(BAR)}),
                is((Object) false));
    }

    @Test
    public void testEqualsToIndirectIsFalse() throws Throwable {
        when(annotationValue.load(getClass().getClassLoader())).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, new ExplicitFoo(BAR))}),
                is((Object) false));
    }

    @Test
    public void testEqualsToOtherHandlerIsFalse() throws Throwable {
        when(annotationValue.load(getClass().getClassLoader())).thenReturn(FOO);
        when(otherAnnotationValue.load(getClass().getClassLoader())).thenReturn(BAR);
        InvocationHandler invocationHandler = new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, otherAnnotationValue));
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, invocationHandler)}),
                is((Object) false));
    }

    @Test
    public void testEqualsToObjectIsFalse() throws Throwable {
        when(annotationValue.load(getClass().getClassLoader())).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new Other()}),
                is((Object) false));
    }

    @Test
    public void testEqualsToInvocationExceptionIsFalse() throws Throwable {
        when(annotationValue.load(getClass().getClassLoader())).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new FooWithException()}),
                is((Object) false));
    }

    public static enum Bar {
        VALUE
    }

    public static @interface Foo {
        String foo();
    }

    public static @interface DefaultFoo {
        String foo() default FOO;
    }

    private static class FooWithException implements Foo {
        @Override
        public String foo() {
            throw new RuntimeException();
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Foo.class;
        }
    }

    private static class ExplicitFoo implements Foo, InvocationHandler {

        private final String value;

        private ExplicitFoo(String value) {
            this.value = value;
        }

        @Override
        public String foo() {
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Foo.class;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(this, args);
        }
    }

    private static class Other implements Annotation {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Annotation.class;
        }
    }
}
