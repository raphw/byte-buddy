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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveTypeAwareAssignerImplicitUnboxingTest {

    private final Class<?> sourceType;

    private final Class<?> wrapperType;

    private final Class<?> targetType;

    private final boolean assignable;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription sourceTypeDescription, targetTypeDescription;

    @Mock
    private Assigner chainedAssigner;

    @Mock
    private StackManipulation chainedStackManipulation;

    private Assigner primitiveAssigner;

    public PrimitiveTypeAwareAssignerImplicitUnboxingTest(Class<?> sourceType,
                                                          Class<?> wrapperType,
                                                          Class<?> targetType,
                                                          boolean assignable) {
        this.sourceType = sourceType;
        this.wrapperType = wrapperType;
        this.targetType = targetType;
        this.assignable = assignable;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
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

    @Before
    public void setUp() throws Exception {
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        when(sourceTypeDescription.isPrimitive()).thenReturn(false);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
        when(targetTypeDescription.isPrimitive()).thenReturn(true);
        when(chainedStackManipulation.isValid()).thenReturn(true);
        when(chainedAssigner.assign(any(TypeDescription.class), any(TypeDescription.class), any(Assigner.Typing.class)))
                .thenReturn(chainedStackManipulation);
        primitiveAssigner = new PrimitiveTypeAwareAssigner(chainedAssigner);
    }

    @Test
    public void testImplicitUnboxingAssignment() {
        StackManipulation stackManipulation = primitiveAssigner.assign(sourceTypeDescription, targetTypeDescription, Assigner.Typing.DYNAMIC);
        assertThat(stackManipulation.isValid(), is(assignable));
        verify(chainedStackManipulation).isValid();
        verifyNoMoreInteractions(chainedStackManipulation);
        verify(sourceTypeDescription, atLeast(0)).represents(any(Class.class));
        verify(sourceTypeDescription, atLeast(1)).isPrimitive();
        verifyNoMoreInteractions(sourceTypeDescription);
        verify(targetTypeDescription, atLeast(0)).represents(any(Class.class));
        verify(targetTypeDescription, atLeast(1)).isPrimitive();
        verifyNoMoreInteractions(targetTypeDescription);
        verify(chainedAssigner).assign(sourceTypeDescription, new TypeDescription.ForLoadedType(wrapperType), Assigner.Typing.DYNAMIC);
        verifyNoMoreInteractions(chainedAssigner);
    }
}
