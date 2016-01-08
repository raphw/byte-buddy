package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveTypeAwareAssignerUnboxingTest {

    private final Class<?> sourceType;

    private final Class<?> targetType;

    private final boolean assignable;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic sourceTypeDescription, targetTypeDescription;

    @Mock
    private Assigner chainedAssigner;

    private Assigner primitiveAssigner;

    public PrimitiveTypeAwareAssignerUnboxingTest(Class<?> sourceType,
                                                  Class<?> targetType,
                                                  boolean assignable) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.assignable = assignable;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Boolean.class, boolean.class, true},
                {Byte.class, byte.class, true},
                {Short.class, short.class, true},
                {Character.class, char.class, true},
                {Integer.class, int.class, true},
                {Long.class, long.class, true},
                {Float.class, float.class, true},
                {Double.class, double.class, true}
        });
    }

    @Before
    public void setUp() throws Exception {
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        when(sourceTypeDescription.isPrimitive()).thenReturn(false);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
        when(targetTypeDescription.isPrimitive()).thenReturn(true);
        primitiveAssigner = new PrimitiveTypeAwareAssigner(chainedAssigner);
    }

    @Test
    public void testUnboxingAssignment() throws Exception {
        StackManipulation stackManipulation = primitiveAssigner.assign(sourceTypeDescription, targetTypeDescription, Assigner.Typing.STATIC);
        assertThat(stackManipulation.isValid(), is(assignable));
        verify(sourceTypeDescription, atLeast(0)).represents(any(Class.class));
        verify(sourceTypeDescription).represents(sourceType);
        verify(sourceTypeDescription, atLeast(1)).isPrimitive();
        verifyNoMoreInteractions(sourceTypeDescription);
        verify(targetTypeDescription, atLeast(0)).represents(any(Class.class));
        verify(targetTypeDescription).represents(targetType);
        verify(targetTypeDescription, atLeast(1)).isPrimitive();
        verifyNoMoreInteractions(targetTypeDescription);
        verifyZeroInteractions(chainedAssigner);
    }
}
