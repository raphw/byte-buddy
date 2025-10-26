package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class PrimitiveNarrowingDelegateIllegalTest {

    private final TypeDescription sourceTypeDescription;

    private final TypeDescription targetTypeDescription;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public PrimitiveNarrowingDelegateIllegalTest(Class<?> sourceType, Class<?> targetType) {
        sourceTypeDescription = mock(TypeDescription.class);
        when(sourceTypeDescription.isPrimitive()).thenReturn(true);
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        targetTypeDescription = mock(TypeDescription.class);
        when(targetTypeDescription.isPrimitive()).thenReturn(true);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, byte.class},
                {boolean.class, short.class},
                {boolean.class, char.class},
                {boolean.class, int.class},
                {boolean.class, long.class},
                {boolean.class, float.class},
                {boolean.class, double.class},
                {byte.class, boolean.class},
                {byte.class, short.class},
                {byte.class, int.class},
                {byte.class, long.class},
                {byte.class, float.class},
                {byte.class, double.class},
                {short.class, boolean.class},
                {short.class, int.class},
                {short.class, long.class},
                {short.class, float.class},
                {short.class, double.class},
                {char.class, boolean.class},
                {char.class, int.class},
                {char.class, long.class},
                {char.class, float.class},
                {char.class, double.class},
                {int.class, boolean.class},
                {int.class, long.class},
                {int.class, float.class},
                {int.class, double.class},
                {long.class, boolean.class},
                {long.class, float.class},
                {long.class, double.class},
                {float.class, boolean.class},
                {float.class, double.class},
                {double.class, boolean.class},
        });
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBoolean() throws Exception {
        StackManipulation stackManipulation = PrimitiveNarrowingDelegate.forPrimitive(sourceTypeDescription).narrowTo(targetTypeDescription);
        assertThat(stackManipulation.isValid(), is(false));
        stackManipulation.apply(methodVisitor, implementationContext);
    }
}
