package net.bytebuddy.instrumentation.method;

import net.bytebuddy.matcher.ElementMatchers;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodListForLoadedTypeTest {

    private MethodList methodList;

    @Before
    public void setUp() throws Exception {
        methodList = new MethodList.ForLoadedType(Object.class);
    }

    @Test
    public void testMethodList() throws Exception {
        assertThat(methodList.size(), is(Object.class.getDeclaredMethods().length + Object.class.getDeclaredConstructors().length));
        for (Method method : Object.class.getDeclaredMethods()) {
            assertThat(methodList.filter(ElementMatchers.is(method)).size(), is(1));
        }
        for (Constructor<?> constructor : Object.class.getDeclaredConstructors()) {
            assertThat(methodList.filter(ElementMatchers.is(constructor)).size(), is(1));
        }
    }

    @Test
    public void testMethodListFilter() throws Exception {
        methodList = methodList.filter(isMethod());
        assertThat(methodList.size(), is(Object.class.getDeclaredMethods().length));
        for (Method method : Object.class.getDeclaredMethods()) {
            MethodList methodList = this.methodList.filter(ElementMatchers.is(method));
            assertThat(methodList.size(), is(1));
            assertThat(methodList.getOnly(), CoreMatchers.<MethodDescription>equalTo(new MethodDescription.ForLoadedMethod(method)));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOnly() throws Exception {
        methodList.getOnly();
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(methodList.subList(0, 1).size(), is(1));
    }
}
