package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class TypeInitializerCompoundTest {

    private static final Class<?> TYPE = Object.class;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeInitializer first, second;
    @Mock
    private InstrumentedType instrumentedType;
    @Mock
    private ByteCodeAppender byteCodeAppender;

    private TypeInitializer compound;

    @Before
    public void setUp() throws Exception {
        compound = new TypeInitializer.Compound(first, second);
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

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new TypeInitializer.Compound(first, second).hashCode(), is(new TypeInitializer.Compound(first, second).hashCode()));
        assertThat(new TypeInitializer.Compound(first, second), is(new TypeInitializer.Compound(first, second)));
        assertThat(new TypeInitializer.Compound(first, second).hashCode(), not(is(new TypeInitializer.Compound(second, first).hashCode())));
        assertThat(new TypeInitializer.Compound(first, second), not(is(new TypeInitializer.Compound(second, first))));
    }
}
