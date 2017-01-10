package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;


public class TypeWriterDefaultForInliningInitializationHandlerAppendingFrameWriterActiveTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    private TypeWriter.Default.ForInlining.InitializationHandler.Appending.FrameWriter frameWriter = new TypeWriter.Default.ForInlining.InitializationHandler.Appending.FrameWriter.Active();

    @Test
    public void testNoFrame() throws Exception {
        frameWriter.emitFrame(methodVisitor);
        verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testSameFrame() throws Exception {
        frameWriter.onFrame(Opcodes.F_SAME, 0);
        frameWriter.emitFrame(methodVisitor);
        verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testSameFrame1() throws Exception {
        frameWriter.onFrame(Opcodes.F_SAME1, 0);
        frameWriter.emitFrame(methodVisitor);
        verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testAppendChop() throws Exception {
        frameWriter.onFrame(Opcodes.F_APPEND, 2);
        frameWriter.onFrame(Opcodes.F_CHOP, 1);
        frameWriter.emitFrame(methodVisitor);
        verify(methodVisitor).visitFrame(Opcodes.F_CHOP, 1, new Object[0], 0, new Object[0]);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testFull() throws Exception {
        frameWriter.onFrame(Opcodes.F_FULL, 5);
        frameWriter.onFrame(Opcodes.F_CHOP, 1);
        frameWriter.emitFrame(methodVisitor);
        verify(methodVisitor).visitFrame(Opcodes.F_FULL, 0, new Object[0], 0, new Object[0]);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testNew() throws Exception {
        frameWriter.onFrame(Opcodes.F_NEW, 5);
        frameWriter.onFrame(Opcodes.F_CHOP, 1);
        frameWriter.emitFrame(methodVisitor);
        verify(methodVisitor).visitFrame(Opcodes.F_FULL, 0, new Object[0], 0, new Object[0]);
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testUnexpected() throws Exception {
        frameWriter.onFrame(-2, 0);
    }
}
