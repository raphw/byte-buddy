package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class LoadedTypeInitializerCompoundTest {

    private static final Class<?> TYPE = Object.class;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private LoadedTypeInitializer first, second;

    @Mock
    private InstrumentedType instrumentedType;

    @Mock
    private ByteCodeAppender byteCodeAppender;

    private LoadedTypeInitializer compound;

    @Before
    public void setUp() throws Exception {
        compound = new LoadedTypeInitializer.Compound(first, second);
    }

    @Test
    public void testIsAlive() throws Exception {
        when(first.isAlive()).thenReturn(true);
        assertThat(compound.isAlive(), is(true));
        verify(first).isAlive();
        verifyNoMoreInteractions(first);
    }

    @Test
    public void testIsNotAlive() throws Exception {
        assertThat(compound.isAlive(), is(false));
        verify(first).isAlive();
        verify(second).isAlive();
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testApply() throws Exception {
        compound.onLoad(TYPE);
        verify(first).onLoad(TYPE);
        verify(second).onLoad(TYPE);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(second);
    }
}
