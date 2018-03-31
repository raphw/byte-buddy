package net.bytebuddy.asm;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AsmVisitorWrapperNoOpTest {

    private static final int FOO = 42, IGNORED = -1;

    @Test
    public void testWrapperChain() throws Exception {
        ClassVisitor classVisitor = mock(ClassVisitor.class);
        assertThat(AsmVisitorWrapper.NoOp.INSTANCE.wrap(mock(TypeDescription.class),
                classVisitor,
                mock(Implementation.Context.class),
                mock(TypePool.class),
                new FieldList.Empty<FieldDescription.InDefinedShape>(),
                new MethodList.Empty<MethodDescription>(),
                IGNORED,
                IGNORED), is(classVisitor));
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
}
