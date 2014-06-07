package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassConstantReferenceTest {

    private static final String FOO = "Lfoo;";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Test
    public void testClassConstant() throws Exception {
        when(typeDescription.getDescriptor()).thenReturn(FOO);
        StackManipulation stackManipulation = ClassConstant.of(typeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(typeDescription).getDescriptor();
        verify(typeDescription, times(9)).represents(any(Class.class));
        verifyNoMoreInteractions(typeDescription);
        verify(methodVisitor).visitLdcInsn(Type.getType(FOO));
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(ClassConstant.of(typeDescription).hashCode(), is(ClassConstant.of(typeDescription).hashCode()));
        assertThat(ClassConstant.of(typeDescription), is(ClassConstant.of(typeDescription)));
        assertThat(ClassConstant.of(typeDescription).hashCode(), not(is(ClassConstant.of(mock(TypeDescription.class)).hashCode())));
        assertThat(ClassConstant.of(typeDescription), not(is(ClassConstant.of(mock(TypeDescription.class)))));
    }
}
