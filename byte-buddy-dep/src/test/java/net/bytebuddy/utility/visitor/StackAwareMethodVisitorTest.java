package net.bytebuddy.utility.visitor;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class StackAwareMethodVisitorTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodVisitor methodVisitor;

    @Test
    public void testDrainSingleSize() throws Exception {
        StackAwareMethodVisitor methodVisitor = new StackAwareMethodVisitor(this.methodVisitor, methodDescription);
        methodVisitor.visitLdcInsn(1);
        methodVisitor.drainStack();
        verify(this.methodVisitor).visitLdcInsn(1);
        verify(this.methodVisitor).visitInsn(Opcodes.POP);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testDrainDoubleSize() throws Exception {
        StackAwareMethodVisitor methodVisitor = new StackAwareMethodVisitor(this.methodVisitor, methodDescription);
        methodVisitor.visitLdcInsn(1L);
        methodVisitor.drainStack();
        verify(this.methodVisitor).visitLdcInsn(1L);
        verify(this.methodVisitor).visitInsn(Opcodes.POP2);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testDrainOrder() throws Exception {
        StackAwareMethodVisitor methodVisitor = new StackAwareMethodVisitor(this.methodVisitor, methodDescription);
        methodVisitor.visitLdcInsn(1);
        methodVisitor.visitLdcInsn(1L);
        methodVisitor.drainStack();
        InOrder inOrder = inOrder(this.methodVisitor);
        inOrder.verify(this.methodVisitor).visitLdcInsn(1);
        inOrder.verify(this.methodVisitor).visitLdcInsn(1L);
        inOrder.verify(this.methodVisitor).visitInsn(Opcodes.POP2);
        inOrder.verify(this.methodVisitor).visitInsn(Opcodes.POP);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testDrainRetainTopSingle() throws Exception {
        when(methodDescription.getStackSize()).thenReturn(42);
        StackAwareMethodVisitor methodVisitor = new StackAwareMethodVisitor(this.methodVisitor, methodDescription);
        methodVisitor.visitLdcInsn(1L);
        methodVisitor.visitLdcInsn(1);
        assertThat(methodVisitor.drainStack(Opcodes.ISTORE, Opcodes.ILOAD, StackSize.SINGLE), is(43));
        InOrder inOrder = inOrder(this.methodVisitor);
        inOrder.verify(this.methodVisitor).visitLdcInsn(1L);
        inOrder.verify(this.methodVisitor).visitLdcInsn(1);
        inOrder.verify(this.methodVisitor).visitVarInsn(Opcodes.ISTORE, 42);
        inOrder.verify(this.methodVisitor).visitInsn(Opcodes.POP2);
        inOrder.verify(this.methodVisitor).visitVarInsn(Opcodes.ILOAD, 42);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testDrainRetainTopDouble() throws Exception {
        when(methodDescription.getStackSize()).thenReturn(42);
        StackAwareMethodVisitor methodVisitor = new StackAwareMethodVisitor(this.methodVisitor, methodDescription);
        methodVisitor.visitLdcInsn(1);
        methodVisitor.visitLdcInsn(1L);
        assertThat(methodVisitor.drainStack(Opcodes.LSTORE, Opcodes.LLOAD, StackSize.DOUBLE), is(44));
        InOrder inOrder = inOrder(this.methodVisitor);
        inOrder.verify(this.methodVisitor).visitLdcInsn(1);
        inOrder.verify(this.methodVisitor).visitLdcInsn(1L);
        inOrder.verify(this.methodVisitor).visitVarInsn(Opcodes.LSTORE, 42);
        inOrder.verify(this.methodVisitor).visitInsn(Opcodes.POP);
        inOrder.verify(this.methodVisitor).visitVarInsn(Opcodes.LLOAD, 42);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testDrainFreeListOnly() throws Exception {
        StackAwareMethodVisitor methodVisitor = new StackAwareMethodVisitor(this.methodVisitor, methodDescription);
        methodVisitor.visitLdcInsn(1);
        methodVisitor.visitVarInsn(Opcodes.ISTORE, 41);
        methodVisitor.visitLdcInsn(1);
        assertThat(methodVisitor.drainStack(Opcodes.ISTORE, Opcodes.ILOAD, StackSize.SINGLE), is(0));
        InOrder inOrder = inOrder(this.methodVisitor);
        inOrder.verify(this.methodVisitor).visitLdcInsn(1);
        inOrder.verify(this.methodVisitor).visitVarInsn(Opcodes.ISTORE, 41);
        inOrder.verify(this.methodVisitor).visitLdcInsn(1);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testDrainFreeList() throws Exception {
        StackAwareMethodVisitor methodVisitor = new StackAwareMethodVisitor(this.methodVisitor, methodDescription);
        methodVisitor.visitLdcInsn(1);
        methodVisitor.visitVarInsn(Opcodes.ISTORE, 41);
        methodVisitor.visitLdcInsn(1);
        methodVisitor.visitLdcInsn(1);
        assertThat(methodVisitor.drainStack(Opcodes.ISTORE, Opcodes.ILOAD, StackSize.SINGLE), is(43));
        InOrder inOrder = inOrder(this.methodVisitor);
        inOrder.verify(this.methodVisitor).visitLdcInsn(1);
        inOrder.verify(this.methodVisitor).visitVarInsn(Opcodes.ISTORE, 41);
        inOrder.verify(this.methodVisitor, times(2)).visitLdcInsn(1);
        inOrder.verify(this.methodVisitor).visitVarInsn(Opcodes.ISTORE, 42);
        inOrder.verify(this.methodVisitor).visitInsn(Opcodes.POP);
        inOrder.verify(this.methodVisitor).visitVarInsn(Opcodes.ILOAD, 42);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testManualRegistration() throws Exception {
        StackAwareMethodVisitor methodVisitor = new StackAwareMethodVisitor(this.methodVisitor, methodDescription);
        Label label = new Label();
        methodVisitor.register(label, Arrays.asList(StackSize.DOUBLE, StackSize.SINGLE));
        methodVisitor.visitLabel(label);
        methodVisitor.drainStack();
        InOrder inOrder = inOrder(this.methodVisitor);
        inOrder.verify(this.methodVisitor).visitLabel(label);
        inOrder.verify(this.methodVisitor).visitInsn(Opcodes.POP);
        inOrder.verify(this.methodVisitor).visitInsn(Opcodes.POP2);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testStackCanUnderflow() throws Exception {
        StackAwareMethodVisitor methodVisitor = new StackAwareMethodVisitor(this.methodVisitor, methodDescription);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.drainStack();
        verify(this.methodVisitor).visitInsn(Opcodes.POP);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(StackAwareMethodVisitor.class).applyBasic();
    }
}