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

@RunWith(Parameterized.class)
public class PrimitiveUnboxingDelegateTest {

    @Parameterized.Parameters
    public static Collection<Object[]> unboxingAssignments() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Boolean.class, "booleanValue", "()Z", 0},
                {byte.class, Byte.class, "byteValue", "()B", 0},
                {short.class, Short.class, "shortValue", "()S", 0},
                {char.class, Character.class, "charValue", "()C", 0},
                {int.class, Integer.class, "intValue", "()I", 0},
                {long.class, Long.class, "longValue", "()J", 1},
                {float.class, Float.class, "floatValue", "()F", 0},
                {double.class, Double.class, "doubleValue", "()D", 1},
        });
    }

    private final Class<?> primitiveType;
    private final Class<?> referenceType;
    private final String unboxingMethodName;
    private final String unboxingMethodDescriptor;
    private final int sizeChange;

    public PrimitiveUnboxingDelegateTest(Class<?> primitiveType,
                                         Class<?> referenceType,
                                         String unboxingMethodName,
                                         String unboxingMethodDescriptor,
                                         int sizeChange) {
        this.primitiveType = primitiveType;
        this.referenceType = referenceType;
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
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
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(sizeChange));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEDYNAMIC, Type.getInternalName(referenceType), unboxingMethodName, unboxingMethodDescriptor);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(chainedAssigner);
    }

    @Test
    public void testImplicitBoxing() throws Exception {
        Assignment assignment = PrimitiveUnboxingDelegate.forReferenceType(Object.class).assignUnboxedTo(primitiveType, chainedAssigner, true);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(sizeChange));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEDYNAMIC, Type.getInternalName(referenceType), unboxingMethodName, unboxingMethodDescriptor);
        verifyNoMoreInteractions(methodVisitor);
        verify(chainedAssigner).assign(Object.class, referenceType, true);
        verifyNoMoreInteractions(chainedAssigner);
    }
}
