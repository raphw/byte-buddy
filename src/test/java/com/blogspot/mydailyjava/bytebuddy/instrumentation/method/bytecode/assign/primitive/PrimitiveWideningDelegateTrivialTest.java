package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveWideningDelegateTrivialTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, boolean.class},
                {byte.class, byte.class},
                {byte.class, short.class},
                {byte.class, int.class},
                {short.class, short.class},
                {short.class, int.class},
                {char.class, int.class},
                {char.class, char.class},
                {int.class, int.class},
                {long.class, long.class},
                {float.class, float.class},
                {double.class, double.class}
        });
    }

    private final Class<?> sourceType;
    private final Class<?> targetType;

    public PrimitiveWideningDelegateTrivialTest(Class<?> sourceType, Class<?> targetType) {
        this.sourceType = sourceType;
        this.targetType = targetType;
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
    public void testNoOpAssignment() throws Exception {
        Assignment assignment = PrimitiveWideningDelegate.forPrimitive(sourceTypeDescription).widenTo(targetTypeDescription);
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }
}
