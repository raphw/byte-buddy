package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class StackManipulationCompoundTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation first, second;

    @Test
    public void testIsValid() throws Exception {
        when(first.isValid()).thenReturn(true);
        when(second.isValid()).thenReturn(true);
        assertThat(new StackManipulation.Compound(first, second).isValid(), is(true));
        verify(first).isValid();
        verifyNoMoreInteractions(first);
        verify(second).isValid();
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testIsInvalid() throws Exception {
        when(first.isValid()).thenReturn(true);
        when(second.isValid()).thenReturn(false);
        assertThat(new StackManipulation.Compound(first, second).isValid(), is(false));
        verify(first).isValid();
        verifyNoMoreInteractions(first);
        verify(second).isValid();
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testApplication() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        when(first.apply(methodVisitor, instrumentationContext)).thenReturn(new StackManipulation.Size(2, 3));
        when(second.apply(methodVisitor, instrumentationContext)).thenReturn(new StackManipulation.Size(2, 3));
        StackManipulation.Size size = new StackManipulation.Compound(first, second).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(4));
        assertThat(size.getMaximalSize(), is(5));
        verify(first).apply(methodVisitor, instrumentationContext);
        verifyNoMoreInteractions(first);
        verify(second).apply(methodVisitor, instrumentationContext);
        verifyNoMoreInteractions(second);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(StackManipulation.Compound.class).apply();
    }
}
