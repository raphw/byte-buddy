package net.bytebuddy.dynamic.scaffold;

import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class TypeWriterDefaultForInliningWithFullProcessingInitializationHandlerAppendingFrameWriterNoOpTest {

    @Test
    public void testFrame() throws Exception {
        TypeWriter.Default.ForInlining.WithFullProcessing.InitializationHandler.Appending.FrameWriter.NoOp.INSTANCE.onFrame(0, 0);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        TypeWriter.Default.ForInlining.WithFullProcessing.InitializationHandler.Appending.FrameWriter.NoOp.INSTANCE.emitFrame(methodVisitor);
        verifyZeroInteractions(methodVisitor);
    }
}
