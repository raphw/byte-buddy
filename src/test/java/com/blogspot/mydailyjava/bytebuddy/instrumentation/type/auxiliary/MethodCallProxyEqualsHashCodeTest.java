package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodCallProxyEqualsHashCodeTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription first, second;

    @Test
    public void testEqualsHashCode() throws Exception {
        assertThat(new MethodCallProxy(first).hashCode(), is(new MethodCallProxy(first).hashCode()));
        assertThat(new MethodCallProxy(first), is(new MethodCallProxy(first)));
        assertThat(new MethodCallProxy(first).hashCode(), not(is(new MethodCallProxy(second).hashCode())));
        assertThat(new MethodCallProxy(first), not(is(new MethodCallProxy(second))));
    }
}
