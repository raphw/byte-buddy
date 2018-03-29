package net.bytebuddy.dynamic.scaffold;

import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.mockito.Mockito.*;

public class TypeWriterDefaultForInliningInitializationHandlerAppendingFrameWriterExpandingTest {

    @Test
    public void testFrame() throws Exception {
        TypeWriter.Default.ForInlining.InitializationHandler.Appending.FrameWriter.Expanding.INSTANCE.onFrame(0, 0);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        TypeWriter.Default.ForInlining.InitializationHandler.Appending.FrameWriter.Expanding.INSTANCE.emitFrame(methodVisitor);
        verify(methodVisitor).visitFrame(Opcodes.F_NEW, 0, new Object[0], 0, new Object[0]);
        verifyNoMoreInteractions(methodVisitor);
    }
}
