package net.bytebuddy.matcher;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractFilterableListTest<T, S extends FilterableList<T, S>, U> {

    protected abstract U getFirst() throws Exception;

    protected abstract U getSecond() throws Exception;

    protected abstract S asList(List<U> elements);

    protected abstract T asElement(U element);

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testGetOnlyTwoElementList() throws Exception {
        asList(Arrays.asList(getFirst(), getSecond())).getOnly();
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testGetOnlyEmptyList() throws Exception {
        asList(Collections.<U>emptyList()).getOnly();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOnlySingleList() throws Exception {
        assertThat(asList(Collections.singletonList(getFirst())).getOnly(), is(asElement(getFirst())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFilter() throws Exception {
        assertThat(asList(Arrays.asList(getFirst(), getSecond())).filter(ElementMatchers.is(asElement(getFirst()))).getOnly(), is(asElement(getFirst())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSubList() throws Exception {
        assertThat(asList(Arrays.asList(getFirst(), getSecond())).subList(0, 1).getOnly(), is(asElement(getFirst())));
    }
}