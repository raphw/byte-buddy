package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;

import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class SubclassInstrumentationContextDelegateTest {

    private static final int PROXY_MODIFIER = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL;

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType, firstAlteration, secondAlteration;
    @Mock
    private MethodList firstAlterationList, firstAlterationListFiltered, secondAlterationList, secondAlterationListFiltered;
    @Mock
    private MethodDescription firstMethod, firstMethodProxy, secondMethod, secondMethodProxy;
    @Mock
    private TypeDescription firstMethodReturnType, secondMethodReturnType;
    @Mock
    private TypeList firstMethodParameters, secondMethodParameters;

    private SubclassInstrumentationContextDelegate delegate;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        delegate = new SubclassInstrumentationContextDelegate(instrumentedType, FOO);
        when(instrumentedType.withMethod(any(String.class), any(TypeDescription.class), any(List.class), anyInt()))
                .thenReturn(firstAlteration);
        when(firstAlteration.getDeclaredMethods()).thenReturn(firstAlterationList);
        when(firstAlterationList.filter(any(MethodMatcher.class))).thenReturn(firstAlterationListFiltered);
        when(firstAlterationListFiltered.getOnly()).thenReturn(firstMethodProxy);
        when(firstAlteration.withMethod(any(String.class), any(TypeDescription.class), any(List.class), anyInt()))
                .thenReturn(secondAlteration);
        when(secondAlteration.getDeclaredMethods()).thenReturn(secondAlterationList);
        when(secondAlterationList.filter(any(MethodMatcher.class))).thenReturn(secondAlterationListFiltered);
        when(secondAlterationListFiltered.getOnly()).thenReturn(secondMethodProxy);
        when(firstMethod.getReturnType()).thenReturn(firstMethodReturnType);
        when(firstMethod.getParameterTypes()).thenReturn(firstMethodParameters);
        when(secondMethod.getReturnType()).thenReturn(secondMethodReturnType);
        when(secondMethod.getParameterTypes()).thenReturn(secondMethodParameters);
        when(secondMethod.isStatic()).thenReturn(true);
    }

    @Test
    public void testReturnsInstrumentedType() throws Exception {
        assertThat(delegate.getInstrumentedType(), is(instrumentedType));
    }

    @Test
    public void testIteratorIsEmpty() throws Exception {
        assertThat(delegate.getProxiedMethods().iterator().hasNext(), is(false));
    }

    @Test
    public void testProxyMethod() throws Exception {
        assertThat(delegate.requireProxyMethodFor(firstMethod), is(firstMethodProxy));
        assertThat(delegate.requireProxyMethodFor(secondMethod), is(secondMethodProxy));
        assertThat(delegate.requireProxyMethodFor(firstMethod), is(firstMethodProxy));
        Iterator<MethodDescription> proxyMethodIterator = delegate.getProxiedMethods().iterator();
        assertThat(proxyMethodIterator.hasNext(), is(true));
        assertThat(proxyMethodIterator.next(), is(firstMethodProxy));
        assertThat(proxyMethodIterator.hasNext(), is(true));
        assertThat(proxyMethodIterator.next(), is(secondMethodProxy));
        assertThat(proxyMethodIterator.hasNext(), is(false));
        verify(instrumentedType).withMethod(any(String.class),
                eq(firstMethodReturnType),
                eq(firstMethodParameters),
                eq(PROXY_MODIFIER));
        verifyNoMoreInteractions(instrumentedType);
        verify(firstAlteration).withMethod(any(String.class),
                eq(secondMethodReturnType),
                eq(secondMethodParameters),
                eq(PROXY_MODIFIER | Opcodes.ACC_STATIC));
        verify(firstAlteration).getDeclaredMethods();
        verifyNoMoreInteractions(firstAlteration);
        verify(secondAlteration).getDeclaredMethods();
        assertThat(delegate.getInstrumentedType(), is(secondAlteration));
        verifyNoMoreInteractions(secondAlteration);
        assertThat(delegate.target(firstMethodProxy).getAttributeAppender(),
                is((MethodAttributeAppender) MethodAttributeAppender.NoOp.INSTANCE));
        assertThat(delegate.target(firstMethodProxy).getByteCodeAppender().appendsCode(), is(true));
        assertThat(delegate.target(firstMethodProxy).isDefineMethod(), is(true));
        assertThat(delegate.target(secondMethodProxy).getAttributeAppender(),
                is((MethodAttributeAppender) MethodAttributeAppender.NoOp.INSTANCE));
        assertThat(delegate.target(secondMethodProxy).getByteCodeAppender().appendsCode(), is(true));
        assertThat(delegate.target(secondMethodProxy).isDefineMethod(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsExceptionForUnknownMethod() throws Exception {
        delegate.target(firstMethodProxy);
    }
}
