package net.bytebuddy.matcher;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FilterableListEmptyTest {

    @SuppressWarnings("unchecked")
    private FilterableList empty = new FilterableList.Empty();

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGet() throws Exception {
        empty.get(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOnly() throws Exception {
        empty.getOnly();
    }

    @Test
    public void testSize() throws Exception {
        assertThat(empty.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFilter() throws Exception {
        assertThat(empty.filter(mock(ElementMatcher.class)), is(empty));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSubListZero() throws Exception {
        assertThat(empty.subList(0, 0), is(empty));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    @SuppressWarnings("unchecked")
    public void testSubListOverflow() throws Exception {
        empty.subList(1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testSubListBounds() throws Exception {
        empty.subList(1, 0);
    }
}
