package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.NoSuchElementException;

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

    @Test
    public void testIsIdenticalWhenFiltered() throws Exception {
        assertThat(methodList.filter(MethodMatchers.any()), is(methodList));
    }

    @Test(expected = NoSuchElementException.class)
    public void testNoElements() throws Exception {
        methodList.get(0);
    }
}
