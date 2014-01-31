package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodListExplicitTest {

    private MethodDescription firstMethodDescription;
    private MethodDescription secondMethodDescription;
    private MethodList methodList;

    @Before
    public void setUp() throws Exception {
        firstMethodDescription = mock(MethodDescription.class);
        secondMethodDescription = mock(MethodDescription.class);
        methodList = new MethodList.Explicit(Arrays.asList(firstMethodDescription, secondMethodDescription));
    }

    @Test
    public void testMethodList() throws Exception {
        assertThat(methodList.size(), is(2));
        assertThat(methodList.get(0), is(firstMethodDescription));
        assertThat(methodList.get(1), is(secondMethodDescription));
    }

    @Test
    public void testMethodListFilter() throws Exception {
        MethodMatcher methodMatcher = mock(MethodMatcher.class);
        when(methodMatcher.matches(firstMethodDescription)).thenReturn(true);
        methodList = methodList.filter(methodMatcher);
        assertThat(methodList.size(), is(1));
        assertThat(methodList.get(0), is(firstMethodDescription));
    }
}
