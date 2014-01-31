package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class PrimitiveWideningDelegateNontrivialTest {

    @Parameterized.Parameters
    public static Collection<Object[]> wideningAssignments() {
        return Arrays.asList(new Object[][]{
                {byte.class, long.class, 1, Opcodes.I2L},
                {byte.class, float.class, 0, Opcodes.I2F},
                {byte.class, double.class, 1, Opcodes.I2L},
                {short.class, long.class, 1, Opcodes.I2L},
                {short.class, float.class, 0, Opcodes.I2F},
                {short.class, double.class, 1, Opcodes.I2D},
                {char.class, long.class, 1, Opcodes.I2L},
                {char.class, float.class, 0, Opcodes.I2F},
                {char.class, double.class, 1, Opcodes.I2D},
                {int.class, long.class, 1, Opcodes.I2L},
                {int.class, float.class, 0, Opcodes.I2F},
                {int.class, double.class, 1, Opcodes.I2D},
                {long.class, float.class, -1, Opcodes.L2F},
                {long.class, double.class, 0, Opcodes.L2D},
                {float.class, double.class, 1, Opcodes.F2D}
        });
    }

    private final Class<?> sourceType;
    private final Class<?> targetType;
    private final int sizeChange;
    private final int opcode;

    public PrimitiveWideningDelegateNontrivialTest(Class<?> sourceType,
                                                   Class<?> targetType,
                                                   int sizeChange,
                                                   int opcode) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.sizeChange = sizeChange;
        this.opcode = opcode;
    }

    private MethodVisitor methodVisitor;
    private TypeDescription sourceTypeDescription;
    private TypeDescription targetTypeDescription;

    @Before
    public void setUp() throws Exception {
        sourceTypeDescription = mock(TypeDescription.class);
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        targetTypeDescription = mock(TypeDescription.class);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testWideningConversion() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(sourceTypeDescription).widenTo(targetTypeDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(Math.max(0, sizeChange)));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
