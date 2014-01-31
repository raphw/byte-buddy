package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
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
public class MethodReturnTest {

    @Parameterized.Parameters
    public static Collection<Object[]> returns() {
        return Arrays.asList(new Object[][]{
                {void.class, Opcodes.RETURN, 0},
                {Object.class, Opcodes.ARETURN, 1},
                {Object[].class, Opcodes.ARETURN, 1},
                {long.class, Opcodes.LRETURN, 2},
                {double.class, Opcodes.DRETURN, 2},
                {float.class, Opcodes.FRETURN, 1},
                {int.class, Opcodes.IRETURN, 1},
                {char.class, Opcodes.IRETURN, 1},
                {short.class, Opcodes.IRETURN, 1},
                {byte.class, Opcodes.IRETURN, 1},
                {boolean.class, Opcodes.IRETURN, 1},
        });
    }

    private final Class<?> type;
    private final int opcode;
    private final int sizeChange;

    public MethodReturnTest(Class<?> type, int opcode, int sizeChange) {
        this.type = type;
        this.opcode = opcode;
        this.sizeChange = sizeChange;
    }

    private MethodVisitor methodVisitor;
    private TypeDescription typeDescription;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
        typeDescription = mock(TypeDescription.class);
        when(typeDescription.isPrimitive()).thenReturn(type.isPrimitive());
        when(typeDescription.represents(type)).thenReturn(true);
    }

    @Test
    public void testVoidReturn() throws Exception {
        Assignment assignment = MethodReturn.returning(typeDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-1 * sizeChange));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
