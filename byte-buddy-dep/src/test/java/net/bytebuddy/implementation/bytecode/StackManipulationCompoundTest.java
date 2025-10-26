package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class StackManipulationCompoundTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        when(first.apply(methodVisitor, implementationContext)).thenReturn(new StackManipulation.Size(2, 3));
        when(second.apply(methodVisitor, implementationContext)).thenReturn(new StackManipulation.Size(2, 3));
        StackManipulation.Size size = new StackManipulation.Compound(first, second).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(4));
        assertThat(size.getMaximalSize(), is(5));
        verify(first).apply(methodVisitor, implementationContext);
        verifyNoMoreInteractions(first);
        verify(second).apply(methodVisitor, implementationContext);
        verifyNoMoreInteractions(second);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }
}
