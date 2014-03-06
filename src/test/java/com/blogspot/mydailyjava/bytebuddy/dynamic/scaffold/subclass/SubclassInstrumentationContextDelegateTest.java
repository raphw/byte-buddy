package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.when;

public class SubclassInstrumentationContextDelegateTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;
    @Mock
    private MethodDescription firstMethod, secondMethod;
    @Mock
    private TypeDescription firstMethodReturnType, secondMethodReturnType;
    @Mock
    private TypeList firstMethodParameters, secondMethodParameters;

    private SubclassInstrumentationContextDelegate delegate;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        delegate = new SubclassInstrumentationContextDelegate(instrumentedType, FOO);
        when(firstMethod.getReturnType()).thenReturn(firstMethodReturnType);
        when(firstMethod.getParameterTypes()).thenReturn(firstMethodParameters);
        when(secondMethod.getReturnType()).thenReturn(secondMethodReturnType);
        when(secondMethod.getParameterTypes()).thenReturn(secondMethodParameters);
        when(secondMethod.isStatic()).thenReturn(true);
    }

    @Test
    public void testIteratorIsEmpty() throws Exception {
        assertThat(delegate.getProxiedMethods().iterator().hasNext(), is(false));
    }

    @Test
    public void testProxyMethod() throws Exception {
        MethodDescription firstProxyMethod = delegate.requireProxyMethodFor(firstMethod);
        assertThat(firstProxyMethod.isStatic(), is(false));
        assertThat(firstProxyMethod, not(is(firstMethod)));
        MethodDescription secondProxyMethod = delegate.requireProxyMethodFor(secondMethod);
        assertThat(secondProxyMethod.isStatic(), is(true));
        assertThat(secondProxyMethod, not(is(secondMethod)));
        assertThat(delegate.requireProxyMethodFor(firstMethod), is(firstProxyMethod));
        Iterator<MethodDescription> iterator = delegate.getProxiedMethods().iterator();
        assertThat(iterator.hasNext(), is(true));
        MethodDescription next = iterator.next();
        assertThat(next, is(firstProxyMethod));
        assertThat(delegate.target(next), notNullValue());
        assertThat(iterator.hasNext(), is(true));
        next = iterator.next();
        assertThat(next, is(secondProxyMethod));
        assertThat(delegate.target(next), notNullValue());
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsExceptionForUnknownMethod() throws Exception {
        delegate.target(firstMethod);
    }
}
