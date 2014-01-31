package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
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
public class PrimitiveUnboxingDelegateDirectTest {

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
    private final Class<?> wrapperType;
    private final String unboxingMethodName;
    private final String unboxingMethodDescriptor;
    private final int sizeChange;

    public PrimitiveUnboxingDelegateDirectTest(Class<?> primitiveType,
                                               Class<?> wrapperType,
                                               String unboxingMethodName,
                                               String unboxingMethodDescriptor,
                                               int sizeChange) {
        this.primitiveType = primitiveType;
        this.wrapperType = wrapperType;
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
        this.sizeChange = sizeChange;
    }

    private TypeDescription primitiveTypeDescription;
    private TypeDescription wrapperTypeDescription;
    private Assigner chainedAssigner;
    private Assignment assignment;
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        primitiveTypeDescription = mock(TypeDescription.class);
        when(primitiveTypeDescription.isPrimitive()).thenReturn(true);
        when(primitiveTypeDescription.represents(primitiveType)).thenReturn(true);
        when(primitiveTypeDescription.getInternalName()).thenReturn(Type.getInternalName(primitiveType));
        wrapperTypeDescription = mock(TypeDescription.class);
        when(wrapperTypeDescription.isPrimitive()).thenReturn(false);
        when(wrapperTypeDescription.represents(wrapperType)).thenReturn(true);
        when(wrapperTypeDescription.getInternalName()).thenReturn(Type.getInternalName(wrapperType));
        chainedAssigner = mock(Assigner.class);
        assignment = mock(Assignment.class);
        when(chainedAssigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean())).thenReturn(assignment);
        when(assignment.isValid()).thenReturn(true);
        when(assignment.apply(any(MethodVisitor.class))).thenReturn(TypeSize.ZERO.toIncreasingSize());
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testTrivialBoxing() throws Exception {
        Assignment assignment = PrimitiveUnboxingDelegate.forReferenceType(wrapperTypeDescription)
                .assignUnboxedTo(primitiveTypeDescription, chainedAssigner, false);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(sizeChange));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                wrapperTypeDescription.getInternalName(),
                unboxingMethodName,
                unboxingMethodDescriptor);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(chainedAssigner);
    }

    @Test
    public void testImplicitBoxing() throws Exception {
        TypeDescription referenceTypeDescription = mock(TypeDescription.class);
        Assignment primitiveAssignment = PrimitiveUnboxingDelegate.forReferenceType(referenceTypeDescription)
                .assignUnboxedTo(primitiveTypeDescription, chainedAssigner, true);
        assertThat(primitiveAssignment.isValid(), is(true));
        Assignment.Size size = primitiveAssignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(sizeChange));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                wrapperTypeDescription.getInternalName(),
                unboxingMethodName,
                unboxingMethodDescriptor);
        verifyNoMoreInteractions(methodVisitor);
        verify(chainedAssigner).assign(referenceTypeDescription, new TypeDescription.ForLoadedType(wrapperType), true);
        verifyNoMoreInteractions(chainedAssigner);
        verify(assignment, atLeast(1)).isValid();
        verify(assignment).apply(methodVisitor);
        verifyNoMoreInteractions(assignment);
    }
}
