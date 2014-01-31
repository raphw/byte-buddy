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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveTypeAwareAssignerImplicitUnboxingTest {

    @Parameterized.Parameters
    public static Collection<Object[]> implicitUnboxingAssignments() {
        return Arrays.asList(new Object[][]{
                {Object.class, Boolean.class, boolean.class, true},
                {Object.class, Byte.class, byte.class, true},
                {Object.class, Short.class, short.class, true},
                {Object.class, Character.class, char.class, true},
                {Object.class, Integer.class, int.class, true},
                {Object.class, Long.class, long.class, true},
                {Object.class, Float.class, float.class, true},
                {Object.class, Double.class, double.class, true}
        });
    }

    private final Class<?> sourceType;
    private final Class<?> wrapperType;
    private final Class<?> targetType;
    private final boolean assignable;

    public PrimitiveTypeAwareAssignerImplicitUnboxingTest(Class<?> sourceType,
                                                          Class<?> wrapperType,
                                                          Class<?> targetType,
                                                          boolean assignable) {
        this.sourceType = sourceType;
        this.wrapperType = wrapperType;
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
        when(sourceTypeDescription.isPrimitive()).thenReturn(false);
        targetTypeDescription = mock(TypeDescription.class);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
        when(targetTypeDescription.isPrimitive()).thenReturn(true);
        chainedAssigner = mock(Assigner.class);
        chainedAssignment = mock(Assignment.class);
        when(chainedAssignment.isValid()).thenReturn(true);
        when(chainedAssigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean())).thenReturn(chainedAssignment);
        primitiveAssigner = new PrimitiveTypeAwareAssigner(chainedAssigner);
    }

    @Test
    public void testImplicitUnboxingAssignment() {
        Assignment assignment = primitiveAssigner.assign(sourceTypeDescription, targetTypeDescription, true);
        assertThat(assignment.isValid(), is(assignable));
        verify(chainedAssignment).isValid();
        verifyNoMoreInteractions(chainedAssignment);
        verify(sourceTypeDescription, atLeast(0)).represents(any(Class.class));
        verify(sourceTypeDescription, atLeast(1)).isPrimitive();
        verifyNoMoreInteractions(sourceTypeDescription);
        verify(targetTypeDescription, atLeast(0)).represents(any(Class.class));
        verify(targetTypeDescription, atLeast(1)).isPrimitive();
        verifyNoMoreInteractions(targetTypeDescription);
        verify(chainedAssigner).assign(sourceTypeDescription, new TypeDescription.ForLoadedType(wrapperType), true);
        verifyNoMoreInteractions(chainedAssigner);
    }
}
