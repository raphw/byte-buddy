package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static net.bytebuddy.matcher.ElementMatchers.isBootstrapClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MultipleParentClassLoaderTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", SCHEME = "http://";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    @Mock
    private ClassLoader first, second;

    private URL fooUrl, barFirstUrl, barSecondUrl, quxUrl;

    @Before
    public void setUp() throws Exception {
        Method loadClass = ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class);
        loadClass.setAccessible(true);
        when(loadClass.invoke(first, FOO, false)).thenReturn(Foo.class);
        when(loadClass.invoke(first, BAR, false)).thenReturn(BarFirst.class);
        when(loadClass.invoke(first, QUX, false)).thenThrow(new ClassNotFoundException());
        when(loadClass.invoke(first, BAZ, false)).thenThrow(new ClassNotFoundException());
        when(loadClass.invoke(second, BAR, false)).thenReturn(BarSecond.class);
        when(loadClass.invoke(second, QUX, false)).thenReturn(Qux.class);
        when(loadClass.invoke(second, BAZ, false)).thenThrow(new ClassNotFoundException());
        fooUrl = new URL(SCHEME + FOO);
        barFirstUrl = new URL(SCHEME + BAR);
        barSecondUrl = new URL(SCHEME + BAZ);
        quxUrl = new URL(SCHEME + QUX);
        when(first.getResource(FOO)).thenReturn(fooUrl);
        when(first.getResource(BAR)).thenReturn(barFirstUrl);
        when(second.getResource(BAR)).thenReturn(barSecondUrl);
        when(second.getResource(QUX)).thenReturn(quxUrl);
        when(first.getResources(FOO)).thenReturn(new SingleElementEnumeration(fooUrl));
        when(first.getResources(BAR)).thenReturn(new SingleElementEnumeration(barFirstUrl));
        when(second.getResources(BAR)).thenReturn(new SingleElementEnumeration(barSecondUrl));
        when(second.getResources(QUX)).thenReturn(new SingleElementEnumeration(quxUrl));
    }

    @Test
    public void testSingleParentReturnsOriginal() throws Exception {
        assertThat(new MultipleParentClassLoader.Builder()
                .append(getClass().getClassLoader(), getClass().getClassLoader())
                .build(), is(ClassLoader.getSystemClassLoader()));
    }

    @Test
    public void testSingleParentReturnsOriginalChained() throws Exception {
        assertThat(new MultipleParentClassLoader.Builder()
                .append(ClassLoader.getSystemClassLoader())
                .append(ClassLoader.getSystemClassLoader())
                .build(), is(ClassLoader.getSystemClassLoader()));
    }

    @Test
    public void testClassLoaderFilter() throws Exception {
        assertThat(new MultipleParentClassLoader.Builder()
                .append(getClass().getClassLoader(), null)
                .filter(not(isBootstrapClassLoader()))
                .build(), is(getClass().getClassLoader()));
    }

    @Test
    public void testMultipleParentClassLoading() throws Exception {
        ClassLoader classLoader = new MultipleParentClassLoader.Builder().append(first, second, null).build();
        assertThat(classLoader.loadClass(FOO), CoreMatchers.<Class<?>>is(Foo.class));
        assertThat(classLoader.loadClass(BAR), CoreMatchers.<Class<?>>is(BarFirst.class));
        assertThat(classLoader.loadClass(QUX), CoreMatchers.<Class<?>>is(Qux.class));
        Method loadClass = ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class);
        loadClass.setAccessible(true);
        loadClass.invoke(verify(first), FOO, false);
        loadClass.invoke(verify(first), BAR, false);
        loadClass.invoke(verify(first), QUX, false);
        verifyNoMoreInteractions(first);
        loadClass.invoke(verify(second), QUX, false);
        verifyNoMoreInteractions(second);
    }

    @Test(expected = ClassNotFoundException.class)
    public void testMultipleParentClassLoadingNotFound() throws Exception {
        new MultipleParentClassLoader.Builder().append(first, second, null).build().loadClass(BAZ);
    }

    @Test
    @IntegrationRule.Enforce
    public void testMultipleParentURL() throws Exception {
        ClassLoader classLoader = new MultipleParentClassLoader.Builder().append(first, second, null).build();
        assertThat(classLoader.getResource(FOO), is(fooUrl));
        assertThat(classLoader.getResource(BAR), is(barFirstUrl));
        assertThat(classLoader.getResource(QUX), is(quxUrl));
        verify(first).getResource(FOO);
        verify(first).getResource(BAR);
        verify(first).getResource(QUX);
        verifyNoMoreInteractions(first);
        verify(second).getResource(QUX);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testMultipleParentURLNotFound() throws Exception {
        assertThat(new MultipleParentClassLoader.Builder().append(first, second, null).build().getResource(BAZ), nullValue(URL.class));
    }

    @Test
    @IntegrationRule.Enforce
    public void testMultipleParentEnumerationURL() throws Exception {
        ClassLoader classLoader = new MultipleParentClassLoader.Builder().append(first, second, null).build();
        Enumeration<URL> foo = classLoader.getResources(FOO);
        assertThat(foo.hasMoreElements(), is(true));
        assertThat(foo.nextElement(), is(fooUrl));
        assertThat(foo.hasMoreElements(), is(false));
        Enumeration<URL> bar = classLoader.getResources(BAR);
        assertThat(bar.hasMoreElements(), is(true));
        assertThat(bar.nextElement(), is(barFirstUrl));
        assertThat(bar.hasMoreElements(), is(true));
        assertThat(bar.nextElement(), is(barSecondUrl));
        assertThat(bar.hasMoreElements(), is(false));
        Enumeration<URL> qux = classLoader.getResources(QUX);
        assertThat(qux.hasMoreElements(), is(true));
        assertThat(qux.nextElement(), is(quxUrl));
        assertThat(qux.hasMoreElements(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testMultipleParentEnumerationNotFound() throws Exception {
        ClassLoader classLoader = new MultipleParentClassLoader.Builder().append(first, second, null).build();
        Enumeration<URL> enumeration = classLoader.getResources(BAZ);
        assertThat(enumeration.hasMoreElements(), is(false));
        enumeration.nextElement();
    }

    @Test(expected = IllegalStateException.class)
    public void testInactiveDispatcher() throws Exception {
        MultipleParentClassLoader.Dispatcher dispatcher = new MultipleParentClassLoader.Dispatcher.Erroneous(new Exception());
        dispatcher.loadClass(mock(ClassLoader.class), FOO, true);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MultipleParentClassLoader.class).applyBasic();
        final Iterator<Method> iterator = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(MultipleParentClassLoader.Dispatcher.Active.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(MultipleParentClassLoader.Dispatcher.Erroneous.class).apply();
        ObjectPropertyAssertion.of(MultipleParentClassLoader.CompoundEnumeration.class).applyBasic();
        ObjectPropertyAssertion.of(MultipleParentClassLoader.Builder.class).apply();
    }

    public static class Foo {
        /* empty */
    }

    public static class BarFirst {
        /* empty */
    }

    public static class BarSecond {
        /* empty */
    }

    public static class Qux {
        /* empty */
    }

    private static class SingleElementEnumeration implements Enumeration<URL> {

        private URL element;

        public SingleElementEnumeration(URL element) {
            this.element = element;
        }

        @Override
        public boolean hasMoreElements() {
            return element != null;
        }

        @Override
        public URL nextElement() {
            if (!hasMoreElements()) {
                throw new AssertionError();
            }
            try {
                return element;
            } finally {
                element = null;
            }
        }
    }
}
