package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ByteCodeAppenderCompoundTest {

    private static final int MINIMUM = 3, MAXIMUM = 5;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ByteCodeAppender first, second;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    private ByteCodeAppender compound;

    @Before
    public void setUp() throws Exception {
        compound = new ByteCodeAppender.Compound(first, second);
    }

    @Test
    public void testApplication() throws Exception {
        when(first.apply(methodVisitor, implementationContext, methodDescription)).thenReturn(new ByteCodeAppender.Size(MINIMUM, MAXIMUM));
        when(second.apply(methodVisitor, implementationContext, methodDescription)).thenReturn(new ByteCodeAppender.Size(MAXIMUM, MINIMUM));
        ByteCodeAppender.Size size = compound.apply(methodVisitor, implementationContext, methodDescription);
        assertThat(size.getLocalVariableSize(), is(MAXIMUM));
        assertThat(size.getOperandStackSize(), is(MAXIMUM));
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }
}
