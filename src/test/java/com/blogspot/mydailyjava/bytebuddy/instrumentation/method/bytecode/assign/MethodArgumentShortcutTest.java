package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.MoreOpcodes;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class MethodArgumentShortcutTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Object.class, 0, MoreOpcodes.ALOAD_0, 1},
                {Object.class, 1, MoreOpcodes.ALOAD_1, 1},
                {Object.class, 2, MoreOpcodes.ALOAD_2, 1},
                {Object.class, 3, MoreOpcodes.ALOAD_3, 1},
                {int.class, 0, MoreOpcodes.ILOAD_0, 1},
                {int.class, 1, MoreOpcodes.ILOAD_1, 1},
                {int.class, 2, MoreOpcodes.ILOAD_2, 1},
                {int.class, 3, MoreOpcodes.ILOAD_3, 1},
                {long.class, 0, MoreOpcodes.LLOAD_0, 2},
                {long.class, 1, MoreOpcodes.LLOAD_1, 2},
                {long.class, 2, MoreOpcodes.LLOAD_2, 2},
                {long.class, 3, MoreOpcodes.LLOAD_3, 2},
                {double.class, 0, MoreOpcodes.DLOAD_0, 2},
                {double.class, 1, MoreOpcodes.DLOAD_1, 2},
                {double.class, 2, MoreOpcodes.DLOAD_2, 2},
                {double.class, 3, MoreOpcodes.DLOAD_3, 2},
                {float.class, 0, MoreOpcodes.FLOAD_0, 1},
                {float.class, 1, MoreOpcodes.FLOAD_1, 1},
                {float.class, 2, MoreOpcodes.FLOAD_2, 1},
                {float.class, 3, MoreOpcodes.FLOAD_3, 1},
        });
    }

    private final TypeDescription typeDescription;
    private final int index;
    private final int opcode;
    private final int size;

    public MethodArgumentShortcutTest(Class<?> type, int index, int opcode, int size) {
        this.typeDescription = mock(TypeDescription.class);
        when(typeDescription.isPrimitive()).thenReturn(type.isPrimitive());
        when(typeDescription.represents(type)).thenReturn(true);
        this.index = index;
        this.opcode = opcode;
        this.size = size;
    }

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testShortCutReference() throws Exception {
        Assignment assignment = MethodArgument.forType(typeDescription).loadFromIndex(index);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(this.size));
        assertThat(size.getMaximalSize(), is(this.size));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
