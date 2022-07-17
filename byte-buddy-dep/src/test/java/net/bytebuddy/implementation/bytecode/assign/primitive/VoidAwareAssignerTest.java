package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class VoidAwareAssignerTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic sourceTypeDescription, targetTypeDescription;

    @Mock
    private Assigner chainedAssigner;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(implementationContext);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testAssignVoidToVoid() throws Exception {
        when(sourceTypeDescription.represents(void.class)).thenReturn(true);
        when(targetTypeDescription.represents(void.class)).thenReturn(true);
        Assigner voidAwareAssigner = new VoidAwareAssigner(chainedAssigner);
        StackManipulation stackManipulation = voidAwareAssigner.assign(sourceTypeDescription, targetTypeDescription, Assigner.Typing.STATIC);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyNoMoreInteractions(chainedAssigner);
    }

    @Test
    public void testAssignNonVoidToNonVoid() throws Exception {
        Assigner voidAwareAssigner = new VoidAwareAssigner(chainedAssigner);
        StackManipulation chainedStackManipulation = mock(StackManipulation.class);
        when(chainedAssigner.assign(sourceTypeDescription, targetTypeDescription, Assigner.Typing.STATIC)).thenReturn(chainedStackManipulation);
        StackManipulation stackManipulation = voidAwareAssigner.assign(sourceTypeDescription, targetTypeDescription, Assigner.Typing.STATIC);
        assertThat(stackManipulation, is(chainedStackManipulation));
        verify(chainedAssigner).assign(sourceTypeDescription, targetTypeDescription, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(chainedAssigner);
    }
}
