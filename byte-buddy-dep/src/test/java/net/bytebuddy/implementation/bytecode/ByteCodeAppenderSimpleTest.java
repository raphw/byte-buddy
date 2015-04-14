package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
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
    private Implementation.Context implementationContext;

    @Mock
    private MethodDescription methodDescription;

    @Before
    public void setUp() throws Exception {
        when(first.apply(methodVisitor, implementationContext)).thenReturn(new StackManipulation.Size(0, 0));
        when(second.apply(methodVisitor, implementationContext)).thenReturn(new StackManipulation.Size(0, 0));
        when(methodDescription.getStackSize()).thenReturn(STACK_SIZE);
    }

    @Test
    public void testApplication() throws Exception {
        ByteCodeAppender byteCodeAppender = new ByteCodeAppender.Simple(first, second);
        ByteCodeAppender.Size size = byteCodeAppender.apply(methodVisitor, implementationContext, methodDescription);
        assertThat(size.getLocalVariableSize(), is(STACK_SIZE));
        assertThat(size.getOperandStackSize(), is(0));
        verify(first).apply(methodVisitor, implementationContext);
        verifyNoMoreInteractions(first);
        verify(second).apply(methodVisitor, implementationContext);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteCodeAppender.Simple.class).apply();
    }
}
