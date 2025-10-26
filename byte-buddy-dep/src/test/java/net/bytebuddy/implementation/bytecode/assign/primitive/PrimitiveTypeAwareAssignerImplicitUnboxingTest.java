package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class PrimitiveTypeAwareAssignerImplicitUnboxingTest {

    private final Class<?> sourceType;

    private final Class<?> wrapperType;

    private final Class<?> targetType;

    private final boolean assignable;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic source, target;

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
        when(source.represents(sourceType)).thenReturn(true);
        when(source.isPrimitive()).thenReturn(false);
        when(source.asGenericType()).thenReturn(source);
        when(target.represents(targetType)).thenReturn(true);
        when(target.isPrimitive()).thenReturn(true);
        when(chainedStackManipulation.isValid()).thenReturn(true);
        when(chainedAssigner.assign(any(TypeDescription.Generic.class), any(TypeDescription.Generic.class), any(Assigner.Typing.class)))
                .thenReturn(chainedStackManipulation);
        primitiveAssigner = new PrimitiveTypeAwareAssigner(chainedAssigner);
    }

    @Test
    public void testImplicitUnboxingAssignment() {
        StackManipulation stackManipulation = primitiveAssigner.assign(source, target, Assigner.Typing.DYNAMIC);
        assertThat(stackManipulation.isValid(), is(assignable));
        verify(chainedStackManipulation).isValid();
        verifyNoMoreInteractions(chainedStackManipulation);
        verify(source, atLeast(0)).represents(any(Class.class));
        verify(source, atLeast(1)).isPrimitive();
        verify(source).asGenericType();
        verifyNoMoreInteractions(source);
        verify(target, atLeast(0)).represents(any(Class.class));
        verify(target, atLeast(1)).isPrimitive();
        verifyNoMoreInteractions(target);
        verify(chainedAssigner).assign(source, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(wrapperType), Assigner.Typing.DYNAMIC);
        verifyNoMoreInteractions(chainedAssigner);
    }
}
