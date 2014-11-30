package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
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
        Assigner voidAwareAssigner = new VoidAwareAssigner(chainedAssigner);
        StackManipulation stackManipulation = voidAwareAssigner.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(chainedAssigner);
    }

    @Test
    public void testAssignNonVoidToNonVoid() throws Exception {
        Assigner voidAwareAssigner = new VoidAwareAssigner(chainedAssigner);
        StackManipulation chainedStackManipulation = mock(StackManipulation.class);
        when(chainedAssigner.assign(sourceTypeDescription, targetTypeDescription, false)).thenReturn(chainedStackManipulation);
        StackManipulation stackManipulation = voidAwareAssigner.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(stackManipulation, is(chainedStackManipulation));
        verify(chainedAssigner).assign(sourceTypeDescription, targetTypeDescription, false);
        verifyNoMoreInteractions(chainedAssigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(VoidAwareAssigner.class).apply();
    }

    @Test
    public void testValueRemoval() throws Exception {


    }
}
