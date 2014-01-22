package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class MethodArgumentTest {

    @Parameterized.Parameters
    public static Collection<Object[]> assignments() {
        return Arrays.asList(new Object[][]{
                {Object.class, Opcodes.ALOAD, 1},
                {Object[].class, Opcodes.AALOAD, 1},
                {int.class, Opcodes.ILOAD, 1},
                {long.class, Opcodes.LLOAD, 2},
                {double.class, Opcodes.DLOAD, 2},
                {float.class, Opcodes.FLOAD, 1},
        });
    }

    private final Class<?> type;
    private final int opcode;
    private final int size;

    public MethodArgumentTest(Class<?> type, int opcode, int size) {
        this.type = type;
        this.opcode = opcode;
        this.size = size;
    }

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testObject() throws Exception {
        Assignment assignment = MethodArgument.forType(type).loadFromIndex(4);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(this.size));
        assertThat(size.getMaximalSize(), is(this.size));
        verify(methodVisitor).visitVarInsn(opcode, 4);
        verifyNoMoreInteractions(methodVisitor);
    }
}
