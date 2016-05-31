package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaConstant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class JavaConstantValueTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private JavaConstant javaConstant;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testMethodHandle() throws Exception {
        when(javaConstant.asConstantPoolValue()).thenReturn(FOO);
        StackManipulation stackManipulation = new JavaConstantValue(javaConstant);
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(javaConstant).asConstantPoolValue();
        verifyNoMoreInteractions(javaConstant);
        verify(methodVisitor).visitLdcInsn(FOO);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(JavaConstantValue.class).apply();
    }
}
