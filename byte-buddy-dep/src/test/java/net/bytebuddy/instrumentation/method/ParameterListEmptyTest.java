package net.bytebuddy.instrumentation.method;

import org.junit.Before;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ParameterListEmptyTest {

    private ParameterList parameterList;

    @Before
    public void setUp() throws Exception {
        parameterList = new ParameterList.Empty();
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertThat(parameterList.size(), is(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testThrowsException() throws Exception {
        parameterList.get(0);
    }

    @Test
    public void testIsIdenticalWhenFiltered() throws Exception {
        assertThat(parameterList.filter(any()), is(parameterList));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testNoElements() throws Exception {
        parameterList.get(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnlyElement() throws Exception {
        parameterList.getOnly();
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(parameterList.subList(0, 0), is(parameterList));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSubListOutOfBounds() throws Exception {
        parameterList.subList(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListIllegal() throws Exception {
        parameterList.subList(1, 0);
    }

    @Test
    public void testMetaData() throws Exception {
        assertThat(parameterList.hasExplicitMetaData(), is(true));
    }
}