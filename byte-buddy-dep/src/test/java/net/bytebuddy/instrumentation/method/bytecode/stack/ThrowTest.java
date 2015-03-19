package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ThrowTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @Test
    public void testApplication() throws Exception {
        StackManipulation.Size size = Throw.INSTANCE.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(Opcodes.ATHROW);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testValidity() throws Exception {
        assertThat(Throw.INSTANCE.isValid(), is(true));
    }
}
