package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypePoolLazyMethodListTest {

    private MethodList methodList;

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
        methodList = typePool.describe(Sample.class.getName()).resolve().getDeclaredMethods();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Test
    public void testFieldList() throws Exception {
        assertThat(methodList.size(), is(3));
        assertThat(methodList.get(0), is((MethodDescription) new MethodDescription.ForLoadedConstructor(Sample.class.getDeclaredConstructor())));
        assertThat(methodList.get(1), is((MethodDescription) new MethodDescription.ForLoadedMethod(Sample.class.getDeclaredMethod("first"))));
        assertThat(methodList.get(2), is((MethodDescription) new MethodDescription.ForLoadedMethod(Sample.class.getDeclaredMethod("second", String.class))));
    }

    @Test
    public void testMethodListFilter() throws Exception {
        @SuppressWarnings("unchecked")
        ElementMatcher<? super MethodDescription> methodMatcher = mock(ElementMatcher.class);
        when(methodMatcher.matches(methodList.get(0))).thenReturn(true);
        methodList = methodList.filter(methodMatcher);
        assertThat(methodList.size(), is(1));
        assertThat(methodList.getOnly(), is(methodList.get(0)));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOnly() throws Exception {
        methodList.getOnly();
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(methodList.subList(0, 1), is((MethodList) new MethodList.Explicit(Arrays.asList(methodList.get(0)))));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSubListOutOfBounds() throws Exception {
        methodList.subList(0, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListIllegal() throws Exception {
        methodList.subList(1, 0);
    }

    public static abstract class Sample {

        abstract int first();

        abstract void second(String argument);
    }
}
