package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.method.matcher.MethodMatchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodListEmptyTest {

    private MethodList methodList;

    @Before
    public void setUp() throws Exception {
        methodList = new MethodList.Empty();
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertThat(methodList.size(), is(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testThrowsException() throws Exception {
        methodList.get(0);
    }

    @Test
    public void testIsIdenticalWhenFiltered() throws Exception {
        assertThat(methodList.filter(MethodMatchers.any()), is(methodList));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testNoElements() throws Exception {
        methodList.get(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnlyElement() throws Exception {
        methodList.getOnly();
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(methodList.subList(0, 0), is(methodList));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSubListOutOfBounds() throws Exception {
        methodList.subList(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListIllegal() throws Exception {
        methodList.subList(1, 0);
    }
}
