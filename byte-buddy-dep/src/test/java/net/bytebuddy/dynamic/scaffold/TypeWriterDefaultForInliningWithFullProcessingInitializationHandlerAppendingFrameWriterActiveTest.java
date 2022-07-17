package net.bytebuddy.dynamic.scaffold;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.mockito.Mockito.inOrder;

public class TypeWriterDefaultForInliningWithFullProcessingInitializationHandlerAppendingFrameWriterActiveTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    private TypeWriter.Default.ForInlining.WithFullProcessing.InitializationHandler.Appending.FrameWriter frameWriter =
            new TypeWriter.Default.ForInlining.WithFullProcessing.InitializationHandler.Appending.FrameWriter.Active();

    @Test
    public void testSameFrame() throws Exception {
        frameWriter.emitFrame(methodVisitor);
        InOrder order = inOrder(methodVisitor);
        order.verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
        order.verify(methodVisitor).visitInsn(Opcodes.NOP);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testSameFrame1() throws Exception {
        frameWriter.onFrame(Opcodes.F_SAME1, 0);
        frameWriter.emitFrame(methodVisitor);
        InOrder order = inOrder(methodVisitor);
        order.verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
        order.verify(methodVisitor).visitInsn(Opcodes.NOP);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testAppendChop() throws Exception {
        frameWriter.onFrame(Opcodes.F_APPEND, 2);
        frameWriter.onFrame(Opcodes.F_CHOP, 1);
        frameWriter.emitFrame(methodVisitor);
        InOrder order = inOrder(methodVisitor);
        order.verify(methodVisitor).visitFrame(Opcodes.F_CHOP, 1, new Object[0], 0, new Object[0]);
        order.verify(methodVisitor).visitInsn(Opcodes.NOP);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testFull() throws Exception {
        frameWriter.onFrame(Opcodes.F_FULL, 5);
        frameWriter.onFrame(Opcodes.F_CHOP, 1);
        frameWriter.emitFrame(methodVisitor);
        InOrder order = inOrder(methodVisitor);
        order.verify(methodVisitor).visitFrame(Opcodes.F_FULL, 0, new Object[0], 0, new Object[0]);
        order.verify(methodVisitor).visitInsn(Opcodes.NOP);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testNew() throws Exception {
        frameWriter.onFrame(Opcodes.F_NEW, 5);
        frameWriter.onFrame(Opcodes.F_CHOP, 1);
        frameWriter.emitFrame(methodVisitor);
        InOrder order = inOrder(methodVisitor);
        order.verify(methodVisitor).visitFrame(Opcodes.F_FULL, 0, new Object[0], 0, new Object[0]);
        order.verify(methodVisitor).visitInsn(Opcodes.NOP);
        order.verifyNoMoreInteractions();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnexpected() throws Exception {
        frameWriter.onFrame(-2, 0);
    }
}
