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

public class ImplementationCompoundComposableTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Implementation.Composable first, second, third, forth;

    @Mock
    private InstrumentedType instrumentedType;

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private ByteCodeAppender byteCodeAppender;

    private Implementation.Composable compound;

    @Before
    public void setUp() throws Exception {
        compound = new Implementation.Compound.Composable(first, second);
    }

    @Test
    public void testPrepare() throws Exception {
        when(second.andThen(third)).thenReturn(forth);
        when(first.prepare(instrumentedType)).thenReturn(instrumentedType);
        when(forth.prepare(instrumentedType)).thenReturn(instrumentedType);
        assertThat(compound.andThen(third).prepare(instrumentedType), is(instrumentedType));
        verify(first).prepare(instrumentedType);
        verify(forth).prepare(instrumentedType);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(forth);
        verify(second).andThen(third);
        verifyNoMoreInteractions(second);
        verifyNoMoreInteractions(third);
    }

    @Test
    public void testAppend() throws Exception {
        when(second.andThen(third)).thenReturn(forth);
        when(first.appender(implementationTarget)).thenReturn(byteCodeAppender);
        when(forth.appender(implementationTarget)).thenReturn(byteCodeAppender);
        assertThat(compound.andThen(third).appender(implementationTarget), notNullValue());
        verify(first).appender(implementationTarget);
        verify(forth).appender(implementationTarget);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(forth);
        verify(second).andThen(third);
        verifyNoMoreInteractions(second);
        verifyNoMoreInteractions(third);
    }
}
