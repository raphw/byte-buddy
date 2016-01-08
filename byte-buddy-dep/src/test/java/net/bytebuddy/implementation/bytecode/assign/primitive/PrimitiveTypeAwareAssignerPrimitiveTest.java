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
public class PrimitiveTypeAwareAssignerPrimitiveTest {

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

    public PrimitiveTypeAwareAssignerPrimitiveTest(Class<?> sourceType,
                                                   Class<?> targetType,
                                                   boolean assignable) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.assignable = assignable;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, boolean.class, true},
                {boolean.class, byte.class, false},
                {boolean.class, short.class, false},
                {boolean.class, char.class, false},
                {boolean.class, int.class, false},
                {boolean.class, long.class, false},
                {boolean.class, float.class, false},
                {boolean.class, double.class, false},

                {byte.class, boolean.class, false},
                {byte.class, byte.class, true},
                {byte.class, short.class, true},
                {byte.class, char.class, false},
                {byte.class, int.class, true},
                {byte.class, long.class, true},
                {byte.class, float.class, true},
                {byte.class, double.class, true},

                {short.class, boolean.class, false},
                {short.class, byte.class, false},
                {short.class, short.class, true},
                {short.class, char.class, false},
                {short.class, int.class, true},
                {short.class, long.class, true},
                {short.class, float.class, true},
                {short.class, double.class, true},

                {char.class, boolean.class, false},
                {char.class, byte.class, false},
                {char.class, short.class, false},
                {char.class, char.class, true},
                {char.class, int.class, true},
                {char.class, long.class, true},
                {char.class, float.class, true},
                {char.class, double.class, true},

                {int.class, boolean.class, false},
                {int.class, byte.class, false},
                {int.class, short.class, false},
                {int.class, char.class, false},
                {int.class, int.class, true},
                {int.class, long.class, true},
                {int.class, float.class, true},
                {int.class, double.class, true},

                {long.class, boolean.class, false},
                {long.class, byte.class, false},
                {long.class, short.class, false},
                {long.class, char.class, false},
                {long.class, int.class, false},
                {long.class, long.class, true},
                {long.class, float.class, true},
                {long.class, double.class, true},

                {float.class, boolean.class, false},
                {float.class, byte.class, false},
                {float.class, short.class, false},
                {float.class, char.class, false},
                {float.class, int.class, false},
                {float.class, long.class, false},
                {float.class, float.class, true},
                {float.class, double.class, true},

                {double.class, boolean.class, false},
                {double.class, byte.class, false},
                {double.class, short.class, false},
                {double.class, char.class, false},
                {double.class, int.class, false},
                {double.class, long.class, false},
                {double.class, float.class, false},
                {double.class, double.class, true},
        });
    }

    @Before
    public void setUp() throws Exception {
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        when(sourceTypeDescription.isPrimitive()).thenReturn(true);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
        when(targetTypeDescription.isPrimitive()).thenReturn(true);
        primitiveAssigner = new PrimitiveTypeAwareAssigner(chainedAssigner);
    }

    @Test
    public void testPrimitiveToPrimitiveAssignment() throws Exception {
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
