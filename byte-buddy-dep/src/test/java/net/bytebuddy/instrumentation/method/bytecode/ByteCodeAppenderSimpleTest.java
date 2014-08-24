package net.bytebuddy.instrumentation.method.bytecode;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ByteCodeAppenderSimpleTest {

    private static final int STACK_SIZE = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation first, second;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @Mock
    private MethodDescription methodDescription;

    @Before
    public void setUp() throws Exception {
        when(first.apply(methodVisitor, instrumentationContext)).thenReturn(new StackManipulation.Size(0, 0));
        when(second.apply(methodVisitor, instrumentationContext)).thenReturn(new StackManipulation.Size(0, 0));
        when(methodDescription.getStackSize()).thenReturn(STACK_SIZE);
    }

    @Test
    public void testApplication() throws Exception {
        ByteCodeAppender byteCodeAppender = new ByteCodeAppender.Simple(first, second);
        assertThat(byteCodeAppender.appendsCode(), is(true));
        ByteCodeAppender.Size size = byteCodeAppender.apply(methodVisitor, instrumentationContext, methodDescription);
        assertThat(size.getLocalVariableSize(), is(STACK_SIZE));
        assertThat(size.getOperandStackSize(), is(0));
        verify(first).apply(methodVisitor, instrumentationContext);
        verifyNoMoreInteractions(first);
        verify(second).apply(methodVisitor, instrumentationContext);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new ByteCodeAppender.Simple(first, second).hashCode(), is(new ByteCodeAppender.Simple(first, second).hashCode()));
        assertThat(new ByteCodeAppender.Simple(first, second), is(new ByteCodeAppender.Simple(first, second)));
        assertThat(new ByteCodeAppender.Simple(first, second).hashCode(), not(is(new ByteCodeAppender.Simple(first).hashCode())));
        assertThat(new ByteCodeAppender.Simple(first, second), not(is(new ByteCodeAppender.Simple(first))));
    }
}
