package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class VoidAwareAssignerTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription sourceTypeDescription, targetTypeDescription;
    @Mock
    private Assigner chainedAssigner;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testAssignVoidToVoid() throws Exception {
        when(sourceTypeDescription.represents(void.class)).thenReturn(true);
        when(targetTypeDescription.represents(void.class)).thenReturn(true);
        Assigner voidAwareAssigner = new VoidAwareAssigner(chainedAssigner, true);
        StackManipulation stackManipulation = voidAwareAssigner.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(chainedAssigner);
    }

    @Test
    public void testAssignNonVoidToNonVoid() throws Exception {
        Assigner voidAwareAssigner = new VoidAwareAssigner(chainedAssigner, true);
        StackManipulation chainedStackManipulation = mock(StackManipulation.class);
        when(chainedAssigner.assign(sourceTypeDescription, targetTypeDescription, false)).thenReturn(chainedStackManipulation);
        StackManipulation stackManipulation = voidAwareAssigner.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(stackManipulation, is(chainedStackManipulation));
        verify(chainedAssigner).assign(sourceTypeDescription, targetTypeDescription, false);
        verifyNoMoreInteractions(chainedAssigner);
    }
}
