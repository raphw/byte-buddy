package net.bytebuddy.instrumentation.method;

import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ParameterListForLoadedExecutableTest {

    private static final String FOO = "foo";

    private ParameterList parameterList;

    @Before
    public void setUp() throws Exception {
        parameterList = ParameterList.ForLoadedExecutable.of(Foo.class.getDeclaredMethod(FOO, Void.class, Void.class));
    }

    @Test
    public void testMethodList() throws Exception {
        assertThat(parameterList.size(), is(2));
        assertThat(parameterList.get(0).getIndex(), is(0));
        assertThat(parameterList.get(0).getOffset(), is(1));
        assertThat(parameterList.get(0).getTypeDescription().represents(Void.class), is(true));
        assertThat(parameterList.get(1).getIndex(), is(1));
        assertThat(parameterList.get(1).getOffset(), is(2));
        assertThat(parameterList.get(1).getTypeDescription().represents(Void.class), is(true));
    }

    @Test
    public void testMethodListFilter() throws Exception {
        parameterList = parameterList.filter(isAnnotatedWith(Bar.class));
        assertThat(parameterList.size(), is(1));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOnly() throws Exception {
        parameterList.getOnly();
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(parameterList.subList(0, 1).size(), is(1));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Bar {
        /* empty */
    }

    private static class Foo {

        private void foo(@Bar Void first, Void second) {
            /* empty */
        }
    }
}
