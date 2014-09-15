package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.utility.HashCodeEqualsTester;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class InstrumentationCompoundTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation first, second;
    @Mock
    private InstrumentedType instrumentedType;
    @Mock
    private Instrumentation.Target instrumentationTarget;
    @Mock
    private ByteCodeAppender byteCodeAppender;

    private Instrumentation compound;

    @Before
    public void setUp() throws Exception {
        compound = new Instrumentation.Compound(first, second);
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
        when(first.appender(instrumentationTarget)).thenReturn(byteCodeAppender);
        when(second.appender(instrumentationTarget)).thenReturn(byteCodeAppender);
        assertThat(compound.appender(instrumentationTarget), notNullValue());
        verify(first).appender(instrumentationTarget);
        verify(second).appender(instrumentationTarget);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(Instrumentation.Compound.class).apply();
    }
}
