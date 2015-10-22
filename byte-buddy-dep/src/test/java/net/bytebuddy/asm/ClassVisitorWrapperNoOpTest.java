package net.bytebuddy.asm;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ClassVisitorWrapperNoOpTest {

    private static final int FOO = 42;

    @Test
    public void testWrapperChain() throws Exception {
        ClassVisitor classVisitor = mock(ClassVisitor.class);
        assertThat(ClassVisitorWrapper.NoOp.INSTANCE.wrap(classVisitor), is(classVisitor));
        verifyZeroInteractions(classVisitor);
    }

    @Test
    public void testReaderFlags() throws Exception {
        assertThat(ClassVisitorWrapper.NoOp.INSTANCE.mergeReader(FOO), is(FOO));
    }

    @Test
    public void testWriterFlags() throws Exception {
        assertThat(ClassVisitorWrapper.NoOp.INSTANCE.mergeWriter(FOO), is(FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassVisitorWrapper.NoOp.class).apply();
    }
}
