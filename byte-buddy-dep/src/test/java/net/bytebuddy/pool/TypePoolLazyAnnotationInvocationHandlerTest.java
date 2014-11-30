package net.bytebuddy.pool;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
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
import static org.mockito.Mockito.*;

public class TypePoolLazyAnnotationInvocationHandlerTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypePool.LazyTypeDescription.AnnotationValue<?, ?> annotationValue, otherAnnotationValue, freeAnnotationValue;

    @Mock
    private TypePool.LazyTypeDescription.AnnotationValue.Loaded<?> loadedAnnotationValue, otherLoadedAnnotationValue;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(annotationValue.load(getClass().getClassLoader()))
                .thenReturn((TypePool.LazyTypeDescription.AnnotationValue.Loaded) loadedAnnotationValue);
        when(otherAnnotationValue.load(getClass().getClassLoader()))
                .thenReturn((TypePool.LazyTypeDescription.AnnotationValue.Loaded) otherLoadedAnnotationValue);
    }

    @Test(expected = ClassNotFoundException.class)
    public void testClassNotFoundExceptionIsTransparent() throws Exception {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenThrow(new ClassNotFoundException());
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = AnnotationTypeMismatchException.class)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeMismatchException() throws Exception {
        when(loadedAnnotationValue.resolve()).thenReturn(new Object());
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = IncompleteAnnotationException.class)
    @SuppressWarnings("unchecked")
    public void testIncompleteAnnotationException() throws Exception {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenReturn((TypePool.LazyTypeDescription.AnnotationValue.Loaded)
                TypePool.LazyTypeDescription.AnnotationInvocationHandler.DefaultValue.of(Object.class.getMethod("toString")));
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = EnumConstantNotPresentException.class)
    @SuppressWarnings("unchecked")
    public void testEnumConstantNotPresentException() throws Exception {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenReturn((TypePool.LazyTypeDescription.AnnotationValue.Loaded)
                new TypePool.LazyTypeDescription.AnnotationValue.ForEnumeration.UnknownRuntimeEnumeration(Bar.class, FOO));
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = IncompatibleClassChangeError.class)
    @SuppressWarnings("unchecked")
    public void testEnumTypeIncompatible() throws Exception {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenReturn((TypePool.LazyTypeDescription.AnnotationValue.Loaded)
                new TypePool.LazyTypeDescription.AnnotationValue.ForEnumeration.IncompatibleRuntimeType(Foo.class));
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = IncompatibleClassChangeError.class)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeIncompatible() throws Exception {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenReturn((TypePool.LazyTypeDescription.AnnotationValue.Loaded)
                new TypePool.LazyTypeDescription.AnnotationValue.ForAnnotation.IncompatibleRuntimeType(Foo.class));
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = RuntimeException.class)
    public void testOtherExceptionIsTransparent() throws Exception {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenThrow(new RuntimeException());
        new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test
    public void testEqualsToDirectIsTrue() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(FOO)}),
                is((Object) true));
    }

    @Test
    public void testEqualsToUnresolvedIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.NON_RESOLVED);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(FOO)}),
                is((Object) false));
        verify(loadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToUndefinedIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.NON_DEFINED);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(FOO)}),
                is((Object) false));
        verify(loadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToIndirectIsTrue() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, new ExplicitFoo(FOO))}),
                is((Object) true));
        verify(loadedAnnotationValue).resolve();
    }

    @Test
    public void testEqualsToOtherHandlerIsTrue() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        InvocationHandler invocationHandler = new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue));
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, invocationHandler)}),
                is((Object) true));
        verify(loadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToDirectIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(BAR)}),
                is((Object) false));
        verify(loadedAnnotationValue).resolve();
    }

    @Test
    public void testEqualsToIndirectIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, new ExplicitFoo(BAR))}),
                is((Object) false));
        verify(loadedAnnotationValue).resolve();
    }

    @Test
    public void testEqualsToOtherHandlerIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        when(otherLoadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.RESOLVED);
        when(otherLoadedAnnotationValue.resolve()).thenReturn(BAR);
        InvocationHandler invocationHandler = new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, otherAnnotationValue));
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, invocationHandler)}),
                is((Object) false));
        verify(loadedAnnotationValue, never()).resolve();
        verify(otherLoadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToObjectIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new Other()}),
                is((Object) false));
        verify(loadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToInvocationExceptionIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(TypePool.LazyTypeDescription.AnnotationValue.Loaded.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        assertThat(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, TypePool.LazyTypeDescription.AnnotationValue<?, ?>>singletonMap(FOO, annotationValue))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new FooWithException()}),
                is((Object) false));
        verify(loadedAnnotationValue).resolve();
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
