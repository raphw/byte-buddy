package net.bytebuddy.utility.visitor;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalVariableAwareMethodVisitorTest {

    private LocalVariableAwareMethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(methodDescription.getStackSize()).thenReturn(1);
        methodVisitor = new LocalVariableAwareMethodVisitor(null, methodDescription);
    }

    @Test
    public void testSingleSize() {
        assertThat(methodVisitor.getFreeOffset(), equalTo(1));
        methodVisitor.visitVarInsn(Opcodes.ASTORE, 1);
        assertThat(methodVisitor.getFreeOffset(), equalTo(2));
    }

    @Test
    public void testDoubleSize() {
        assertThat(methodVisitor.getFreeOffset(), equalTo(1));
        methodVisitor.visitVarInsn(Opcodes.LSTORE, 1);
        assertThat(methodVisitor.getFreeOffset(), equalTo(3));
    }
}