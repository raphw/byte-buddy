package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import org.junit.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SameThreadCoModifiableIterableTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Test
    public void testIteration() throws Exception {
        List<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(FOO, BAR));
        Iterable<String> iterable = new TypeWriter.SameThreadCoModifiableIterable<String>(list);
        Iterator<String> iterator = iterable.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(FOO));
        list.add(QUX);
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(BAR));
        list.add(BAZ);
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(QUX));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(BAZ));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCannotRemoveElement() throws Exception {
        List<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(FOO, BAR));
        Iterable<String> iterable = new TypeWriter.SameThreadCoModifiableIterable<String>(list);
        Iterator<String> iterator = iterable.iterator();
        iterator.remove();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIteratorOverflow() throws Exception {
        List<String> list = new ArrayList<String>();
        Iterable<String> iterable = new TypeWriter.SameThreadCoModifiableIterable<String>(list);
        Iterator<String> iterator = iterable.iterator();
        assertThat(iterator.hasNext(), is(false));
        iterator.next();
    }
}
