package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(Parameterized.class)
public class PrimitiveUnboxingDelegateWideningTest {

    @Parameterized.Parameters
    public static Collection<Object[]> unboxingAssignments() {
        return Arrays.asList(new Object[][]{
                {long.class, Integer.class, "intValue", "()I", Opcodes.I2L, 1},
                {double.class, Integer.class, "intValue", "()I", Opcodes.I2D, 1},
                {float.class, Integer.class, "intValue", "()I", Opcodes.I2F, 0},
                {long.class, Short.class, "shortValue", "()S", Opcodes.I2L, 1},
                {double.class, Short.class, "shortValue", "()S", Opcodes.I2D, 1},
                {float.class, Short.class, "shortValue", "()S", Opcodes.I2F, 0},
        });
    }

    private final Class<?> primitiveType;
    private final Class<?> referenceType;
    private final String unboxingMethodName;
    private final String unboxingMethodDescriptor;
    private final int wideningOpcode;
    private final int sizeChange;

    public PrimitiveUnboxingDelegateWideningTest(Class<?> primitiveType,
                                                 Class<?> referenceType,
                                                 String unboxingMethodName,
                                                 String unboxingMethodDescriptor,
                                                 int wideningOpcode,
                                                 int sizeChange) {
        this.primitiveType = primitiveType;
        this.referenceType = referenceType;
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
        this.wideningOpcode = wideningOpcode;
        this.sizeChange = sizeChange;
    }

    private Assigner chainedAssigner;
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        chainedAssigner = mock(Assigner.class);
        when(chainedAssigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testTrivialBoxing() throws Exception {
        Assignment assignment = PrimitiveUnboxingDelegate.forReferenceType(referenceType).assignUnboxedTo(primitiveType, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(sizeChange));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEDYNAMIC, Type.getInternalName(referenceType), unboxingMethodName, unboxingMethodDescriptor);
        verify(methodVisitor).visitInsn(wideningOpcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(chainedAssigner);
    }
}
