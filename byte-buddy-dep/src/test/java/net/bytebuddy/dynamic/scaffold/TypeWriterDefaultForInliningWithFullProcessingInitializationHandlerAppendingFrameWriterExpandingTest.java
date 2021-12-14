package net.bytebuddy.dynamic.scaffold;

import org.junit.Test;
import org.mockito.InOrder;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class TypeWriterDefaultForInliningWithFullProcessingInitializationHandlerAppendingFrameWriterExpandingTest {

    @Test
    public void testFrame() throws Exception {
        TypeWriter.Default.ForInlining.WithFullProcessing.InitializationHandler.Appending.FrameWriter.Expanding.INSTANCE.onFrame(0, 0);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        TypeWriter.Default.ForInlining.WithFullProcessing.InitializationHandler.Appending.FrameWriter.Expanding.INSTANCE.emitFrame(methodVisitor);
        InOrder order = inOrder(methodVisitor);
        order.verify(methodVisitor).visitFrame(Opcodes.F_NEW, 0, new Object[0], 0, new Object[0]);
        order.verify(methodVisitor).visitInsn(Opcodes.NOP);
        order.verifyNoMoreInteractions();
    }
}
