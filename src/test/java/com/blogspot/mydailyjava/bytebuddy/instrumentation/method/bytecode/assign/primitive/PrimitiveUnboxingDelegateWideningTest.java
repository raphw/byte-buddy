package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveUnboxingDelegateWideningTest {

    @Parameterized.Parameters
    public static Collection<Object[]> wideningAssignments() {
        return Arrays.asList(new Object[][]{
                {Short.class, long.class, "shortValue", "()S", Opcodes.I2L, 1, 1},
                {Short.class, float.class, "shortValue", "()S", Opcodes.I2F, 0, 0},
                {Short.class, double.class, "shortValue", "()S", Opcodes.I2D, 1, 1},
                {Integer.class, long.class, "intValue", "()I", Opcodes.I2L, 1, 1},
                {Integer.class, float.class, "intValue", "()I", Opcodes.I2F, 0, 0},
                {Integer.class, double.class, "intValue", "()I", Opcodes.I2D, 1, 1},
                {Long.class, float.class, "longValue", "()J", Opcodes.L2F, 0, 1},
                {Long.class, double.class, "longValue", "()J", Opcodes.L2D, 1, 1},
                {Float.class, double.class, "floatValue", "()F", Opcodes.F2D, 1, 1},
        });
    }

    private final Class<?> primitiveType;
    private final Class<?> referenceType;
    private final String unboxingMethodName;
    private final String unboxingMethodDescriptor;
    private final int wideningOpcode;
    private final int sizeChange;
    private final int interimMaximum;

    public PrimitiveUnboxingDelegateWideningTest(Class<?> referenceType,
                                                 Class<?> primitiveType,

                                                 String unboxingMethodName,
                                                 String unboxingMethodDescriptor,
                                                 int wideningOpcode,
                                                 int sizeChange,
                                                 int interimMaximum) {
        this.primitiveType = primitiveType;
        this.referenceType = referenceType;
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
        this.wideningOpcode = wideningOpcode;
        this.sizeChange = sizeChange;
        this.interimMaximum = interimMaximum;
    }

    private TypeDescription referenceTypeDescription;
    private TypeDescription primitiveTypeDescription;
    private Assigner chainedAssigner;
    private Assignment chainedAssignment;
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        referenceTypeDescription = mock(TypeDescription.class);
        when(referenceTypeDescription.represents(referenceType)).thenReturn(true);
        primitiveTypeDescription = mock(TypeDescription.class);
        when(primitiveTypeDescription.isPrimitive()).thenReturn(true);
        when(primitiveTypeDescription.represents(primitiveType)).thenReturn(true);
        chainedAssigner = mock(Assigner.class);
        chainedAssignment = mock(Assignment.class);
        when(chainedAssigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean())).thenReturn(chainedAssignment);
        when(chainedAssignment.isValid()).thenReturn(true);
        when(chainedAssignment.apply(any(MethodVisitor.class))).thenReturn(TypeSize.ZERO.toIncreasingSize());
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testTrivialBoxing() throws Exception {
        Assignment assignment = PrimitiveUnboxingDelegate.forReferenceType(referenceTypeDescription)
                .assignUnboxedTo(primitiveTypeDescription, chainedAssigner, false);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(interimMaximum));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(referenceType),
                unboxingMethodName,
                unboxingMethodDescriptor);
        verify(methodVisitor).visitInsn(wideningOpcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(chainedAssigner);
    }
}
