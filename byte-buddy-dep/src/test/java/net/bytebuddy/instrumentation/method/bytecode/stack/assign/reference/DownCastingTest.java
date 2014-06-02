package net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DownCastingTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoTest = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Test
    public void testCasting() throws Exception {
        when(typeDescription.getInternalName()).thenReturn(FOO);
        StackManipulation.Size size = new DownCasting(typeDescription).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitTypeInsn(Opcodes.CHECKCAST, FOO);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveCastingThrowsException() throws Exception {
        when(typeDescription.isPrimitive()).thenReturn(true);
        new DownCasting(typeDescription);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        when(typeDescription.getInternalName()).thenReturn(FOO);
        TypeDescription otherEqual = mock(TypeDescription.class);
        when(otherEqual.getInternalName()).thenReturn(FOO);
        assertThat(new DownCasting(typeDescription).hashCode(), is(new DownCasting(otherEqual).hashCode()));
        assertThat(new DownCasting(typeDescription), is(new DownCasting(otherEqual)));
        TypeDescription otherNonEqual = mock(TypeDescription.class);
        when(otherNonEqual.getInternalName()).thenReturn(BAR);
        assertThat(new DownCasting(typeDescription).hashCode(), not(is(new DownCasting(otherNonEqual).hashCode())));
        assertThat(new DownCasting(typeDescription), not(is(new DownCasting(otherNonEqual))));
    }
}
