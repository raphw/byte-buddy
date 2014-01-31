package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveTypeAwareAssignerBoxingTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Boolean.class, true},
                {byte.class, Byte.class, true},
                {short.class, Short.class, true},
                {char.class, Character.class, true},
                {int.class, Integer.class, true},
                {long.class, Long.class, true},
                {float.class, Float.class, true},
                {double.class, Double.class, true}
        });
    }

    private final Class<?> sourceType;
    private final Class<?> targetType;
    private final boolean assignable;

    public PrimitiveTypeAwareAssignerBoxingTest(Class<?> sourceType,
                                                Class<?> targetType,
                                                boolean assignable) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.assignable = assignable;
    }

    private TypeDescription sourceTypeDescription;
    private TypeDescription targetTypeDescription;
    private Assigner chainedAssigner;
    private Assignment chainedAssignment;
    private Assigner primitiveAssigner;

    @Before
    public void setUp() throws Exception {
        sourceTypeDescription = mock(TypeDescription.class);
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        when(sourceTypeDescription.isPrimitive()).thenReturn(true);
        targetTypeDescription = mock(TypeDescription.class);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
        when(targetTypeDescription.isPrimitive()).thenReturn(false);
        chainedAssigner = mock(Assigner.class);
        chainedAssignment = mock(Assignment.class);
        when(chainedAssignment.isValid()).thenReturn(true);
        when(chainedAssigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean())).thenReturn(chainedAssignment);
        primitiveAssigner = new PrimitiveTypeAwareAssigner(chainedAssigner);
    }

    @Test
    public void testBoxingAssignment() {
        Assignment assignment = primitiveAssigner.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(assignment.isValid(), is(assignable));
        verify(chainedAssignment).isValid();
        verifyNoMoreInteractions(chainedAssignment);
        verify(sourceTypeDescription, atLeast(0)).represents(any(Class.class));
        verify(sourceTypeDescription).represents(sourceType);
        verify(sourceTypeDescription, atLeast(1)).isPrimitive();
        verifyNoMoreInteractions(sourceTypeDescription);
        verify(targetTypeDescription, atLeast(1)).isPrimitive();
        verifyNoMoreInteractions(targetTypeDescription);
        verify(chainedAssigner).assign(new TypeDescription.ForLoadedType(targetType), targetTypeDescription, false);
        verifyNoMoreInteractions(chainedAssigner);
    }
}
