package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SerializedConstantTest {

    private static final String FOO = "foo";

    @Test
    public void testNullValue() throws Exception {
        assertThat(SerializedConstant.of(null), is((StackManipulation) NullConstant.INSTANCE));
    }

    @Test
    public void testSerialization() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        SerializedConstant.of(FOO).apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitLdcInsn(contains(FOO));
        verifyZeroInteractions(implementationContext);
    }
}
