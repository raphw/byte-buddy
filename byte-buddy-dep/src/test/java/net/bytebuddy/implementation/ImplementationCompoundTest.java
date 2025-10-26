package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ImplementationCompoundTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Implementation first, second;

    @Mock
    private InstrumentedType instrumentedType;

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private ByteCodeAppender byteCodeAppender;

    private Implementation compound;

    @Before
    public void setUp() throws Exception {
        compound = new Implementation.Compound(first, second);
    }

    @Test
    public void testPrepare() throws Exception {
        when(first.prepare(instrumentedType)).thenReturn(instrumentedType);
        when(second.prepare(instrumentedType)).thenReturn(instrumentedType);
        assertThat(compound.prepare(instrumentedType), is(instrumentedType));
        verify(first).prepare(instrumentedType);
        verify(second).prepare(instrumentedType);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testAppend() throws Exception {
        when(first.appender(implementationTarget)).thenReturn(byteCodeAppender);
        when(second.appender(implementationTarget)).thenReturn(byteCodeAppender);
        assertThat(compound.appender(implementationTarget), notNullValue());
        verify(first).appender(implementationTarget);
        verify(second).appender(implementationTarget);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(second);
    }
}
