package net.bytebuddy.implementation.bytecode.member;

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
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class HandleInvocationTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testInvocationDecreasingStack() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(void.class, Object.class);
        StackManipulation stackManipulation = new HandleInvocation(methodType);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "(Ljava/lang/Object;)V", false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testInvocationIncreasingStack() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(Object.class);
        StackManipulation stackManipulation = new HandleInvocation(methodType);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()Ljava/lang/Object;", false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(HandleInvocation.class).apply();
    }
}
