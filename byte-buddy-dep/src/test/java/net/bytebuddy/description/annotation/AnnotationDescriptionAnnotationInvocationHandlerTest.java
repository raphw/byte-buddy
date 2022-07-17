package net.bytebuddy.description.annotation;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;

import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AnnotationDescriptionAnnotationInvocationHandlerTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private AnnotationValue<?, ?> annotationValue, otherAnnotationValue, freeAnnotationValue;

    @Mock
    private AnnotationValue.Loaded<?> loadedAnnotationValue, otherLoadedAnnotationValue;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(annotationValue.filter(Mockito.any(MethodDescription.InDefinedShape.class))).thenReturn((AnnotationValue) annotationValue);
        when(annotationValue.load(getClass().getClassLoader()))
                .thenReturn((AnnotationValue.Loaded) loadedAnnotationValue);
        when(otherAnnotationValue.filter(Mockito.any(MethodDescription.InDefinedShape.class))).thenReturn((AnnotationValue) otherAnnotationValue);
        when(otherAnnotationValue.load(getClass().getClassLoader()))
                .thenReturn((AnnotationValue.Loaded) otherLoadedAnnotationValue);
        when(freeAnnotationValue.filter(Mockito.any(MethodDescription.InDefinedShape.class))).thenReturn((AnnotationValue) freeAnnotationValue);
    }

    @Test(expected = IncompleteAnnotationException.class)
    @SuppressWarnings("unchecked")
    public void testIncompleteAnnotationException() throws Throwable {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenReturn((AnnotationValue.Loaded)
                new AnnotationValue.ForMissingValue.Loaded<Void>(Foo.class, "foo"));
        Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue)))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = EnumConstantNotPresentException.class)
    @SuppressWarnings("unchecked")
    public void testEnumConstantNotPresentException() throws Throwable {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenReturn((AnnotationValue.Loaded)
                new AnnotationValue.ForEnumerationDescription.WithUnknownConstant.Loaded(Bar.class, FOO));
        Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue)))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = IncompatibleClassChangeError.class)
    @SuppressWarnings("unchecked")
    public void testEnumTypeIncompatible() throws Throwable {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenReturn((AnnotationValue.Loaded)
                new AnnotationValue.ForEnumerationDescription.Loaded.WithIncompatibleRuntimeType(Foo.class));
        Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue)))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = IncompatibleClassChangeError.class)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeIncompatible() throws Throwable {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenReturn((AnnotationValue.Loaded)
                new AnnotationValue.ForEnumerationDescription.Loaded.WithIncompatibleRuntimeType(Foo.class));
        Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue)))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test(expected = RuntimeException.class)
    public void testOtherExceptionIsTransparent() throws Throwable {
        when(freeAnnotationValue.load(getClass().getClassLoader())).thenThrow(new RuntimeException());
        Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                        Foo.class,
                        Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, freeAnnotationValue)))
                .invoke(new Object(), Foo.class.getDeclaredMethod("foo"), new Object[0]);
    }

    @Test
    public void testEqualsToDirectIsTrue() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.RESOLVED);
        when(loadedAnnotationValue.represents(FOO)).thenReturn(true);
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(FOO)}),
                is((Object) true));
    }

    @Test
    public void testEqualsToUnresolvedIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.UNRESOLVED);
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(FOO)}),
                is((Object) false));
        verify(loadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToUndefinedIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.UNDEFINED);
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(FOO)}),
                is((Object) false));
        verify(loadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToIndirectIsTrue() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.RESOLVED);
        when(loadedAnnotationValue.represents(FOO)).thenReturn(true);
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, new ExplicitFoo(FOO))}),
                is((Object) true));
        verify(loadedAnnotationValue).represents(FOO);
    }

    @Test
    public void testEqualsToOtherHandlerIsTrue() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)));
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, invocationHandler)}),
                is((Object) true));
        verify(loadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToDirectIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.RESOLVED);
        when(loadedAnnotationValue.represents(BAR)).thenReturn(false);
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new ExplicitFoo(BAR)}),
                is((Object) false));
        verify(loadedAnnotationValue).represents(BAR);
    }

    @Test
    public void testEqualsToIndirectIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.RESOLVED);
        when(loadedAnnotationValue.represents(BAR)).thenReturn(false);
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, new ExplicitFoo(BAR))}),
                is((Object) false));
        verify(loadedAnnotationValue).represents(BAR);
    }

    @Test
    public void testEqualsToOtherHandlerIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        when(otherLoadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.RESOLVED);
        when(otherLoadedAnnotationValue.resolve()).thenReturn(BAR);
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                Foo.class,
                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, otherAnnotationValue)));
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class),
                                new Object[]{Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Foo.class}, invocationHandler)}),
                is((Object) false));
        verify(loadedAnnotationValue, never()).resolve();
        verify(otherLoadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToObjectIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new Other()}),
                is((Object) false));
        verify(loadedAnnotationValue, never()).resolve();
    }

    @Test
    public void testEqualsToInvocationExceptionIsFalse() throws Throwable {
        when(loadedAnnotationValue.getState()).thenReturn(AnnotationValue.State.RESOLVED);
        when(loadedAnnotationValue.resolve()).thenReturn(FOO);
        assertThat(Proxy.getInvocationHandler(AnnotationDescription.AnnotationInvocationHandler.of(getClass().getClassLoader(),
                                Foo.class,
                                Collections.<String, AnnotationValue<?, ?>>singletonMap(FOO, annotationValue)))
                        .invoke(new Object(), Object.class.getDeclaredMethod("equals", Object.class), new Object[]{new FooWithException()}),
                is((Object) false));
        verify(loadedAnnotationValue, never()).represents(any(String.class));
    }

    public enum Bar {
        VALUE
    }

    public @interface Foo {

        String foo();
    }

    public @interface DefaultFoo {

        String foo() default FOO;
    }

    private static class FooWithException implements Foo {

        public String foo() {
            throw new RuntimeException();
        }

        public Class<? extends Annotation> annotationType() {
            return Foo.class;
        }
    }

    private static class ExplicitFoo implements Foo, InvocationHandler {

        private final String value;

        private ExplicitFoo(String value) {
            this.value = value;
        }

        public String foo() {
            return value;
        }

        public Class<? extends Annotation> annotationType() {
            return Foo.class;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(this, args);
        }
    }

    private static class Other implements Annotation {

        public Class<? extends Annotation> annotationType() {
            return Annotation.class;
        }
    }
}
