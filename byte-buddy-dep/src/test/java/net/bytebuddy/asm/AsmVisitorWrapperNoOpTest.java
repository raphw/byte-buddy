package net.bytebuddy.asm;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AsmVisitorWrapperNoOpTest {

    private static final int FOO = 42, IRRELEVANT = -1;

    @Test
    public void testWrapperChain() throws Exception {
        ClassVisitor classVisitor = mock(ClassVisitor.class);
        assertThat(AsmVisitorWrapper.NoOp.INSTANCE.wrap(mock(TypeDescription.class), classVisitor, IRRELEVANT, IRRELEVANT), is(classVisitor));
        verifyZeroInteractions(classVisitor);
    }

    @Test
    public void testReaderFlags() throws Exception {
        assertThat(AsmVisitorWrapper.NoOp.INSTANCE.mergeReader(FOO), is(FOO));
    }

    @Test
    public void testWriterFlags() throws Exception {
        assertThat(AsmVisitorWrapper.NoOp.INSTANCE.mergeWriter(FOO), is(FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AsmVisitorWrapper.NoOp.class).apply();
    }
}
