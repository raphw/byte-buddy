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
public class PrimitiveBoxingDelegateTest {

    private static final String VALUE_OF = "valueOf";

    @Parameterized.Parameters
    public static Collection<Object[]> boxingAssignments() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Boolean.class, "(Z)Ljava/lang/Boolean;", 0},
                {byte.class, Byte.class, "(B)Ljava/lang/Byte;", 0},
                {short.class, Short.class, "(S)Ljava/lang/Short;", 0},
                {char.class, Character.class, "(C)Ljava/lang/Character;", 0},
                {int.class, Integer.class, "(I)Ljava/lang/Integer;", 0},
                {long.class, Long.class, "(J)Ljava/lang/Long;", -1},
                {float.class, Float.class, "(F)Ljava/lang/Float;", 0},
                {double.class, Double.class, "(D)Ljava/lang/Double;", -1},
        });
    }

    private final Class<?> primitiveType;
    private final Class<?> referenceType;
    private final String boxingMethodDescriptor;
    private final int sizeChange;

    public PrimitiveBoxingDelegateTest(Class<?> primitiveType,
                                       Class<?> referenceType,
                                       String boxingMethodDescriptor,
                                       int sizeChange) {
        this.primitiveType = primitiveType;
        this.referenceType = referenceType;
        this.boxingMethodDescriptor = boxingMethodDescriptor;
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
    public void testBoxing() throws Exception {
        Assignment assignment = PrimitiveBoxingDelegate.forPrimitive(primitiveType).assignBoxedTo(Void.class, chainedAssigner, false);
        assertThat(assignment.isAssignable(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(0));
        verify(chainedAssigner).assign(referenceType, Void.class, false);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(referenceType), VALUE_OF, boxingMethodDescriptor);
        verifyNoMoreInteractions(methodVisitor);
    }
}
