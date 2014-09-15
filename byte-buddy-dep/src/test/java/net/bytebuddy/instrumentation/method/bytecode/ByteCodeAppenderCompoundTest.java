package net.bytebuddy.instrumentation.method.bytecode;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.utility.HashCodeEqualsTester;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ByteCodeAppenderCompoundTest {

    private static final int MINIMUM = 3, MAXIMUM = 5;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ByteCodeAppender first, second;
    @Mock
    private MethodDescription methodDescription;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    private ByteCodeAppender compound;

    @Before
    public void setUp() throws Exception {
        compound = new ByteCodeAppender.Compound(first, second);
    }

    @Test
    public void testAppendsCode() throws Exception {
        when(first.appendsCode()).thenReturn(true);
        assertThat(compound.appendsCode(), is(true));
    }

    @Test
    public void testDoesNotAppendCode() throws Exception {
        assertThat(compound.appendsCode(), is(false));
    }

    @Test
    public void testApplication() throws Exception {
        when(first.apply(methodVisitor, instrumentationContext, methodDescription)).thenReturn(new ByteCodeAppender.Size(MINIMUM, MAXIMUM));
        when(second.apply(methodVisitor, instrumentationContext, methodDescription)).thenReturn(new ByteCodeAppender.Size(MAXIMUM, MINIMUM));
        ByteCodeAppender.Size size = compound.apply(methodVisitor, instrumentationContext, methodDescription);
        assertThat(size.getLocalVariableSize(), is(MAXIMUM));
        assertThat(size.getOperandStackSize(), is(MAXIMUM));
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(ByteCodeAppender.Compound.class).apply();
    }
}
