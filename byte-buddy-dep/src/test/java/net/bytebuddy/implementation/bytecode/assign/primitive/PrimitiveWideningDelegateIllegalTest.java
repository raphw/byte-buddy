package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveWideningDelegateIllegalTest {

    private final TypeDescription sourceTypeDescription;

    private final TypeDescription targetTypeDescription;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public PrimitiveWideningDelegateIllegalTest(Class<?> sourceType, Class<?> targetType) {
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
                {byte.class, char.class},
                {short.class, boolean.class},
                {short.class, byte.class},
                {short.class, char.class},
                {char.class, boolean.class},
                {char.class, byte.class},
                {char.class, short.class},
                {int.class, boolean.class},
                {int.class, byte.class},
                {int.class, short.class},
                {long.class, boolean.class},
                {long.class, byte.class},
                {long.class, short.class},
                {long.class, char.class},
                {long.class, int.class},
                {float.class, boolean.class},
                {float.class, byte.class},
                {float.class, short.class},
                {float.class, char.class},
                {float.class, int.class},
                {float.class, long.class},
                {double.class, boolean.class},
                {double.class, byte.class},
                {double.class, short.class},
                {double.class, char.class},
                {double.class, int.class},
                {double.class, long.class},
                {double.class, float.class},
        });
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBoolean() throws Exception {
        StackManipulation stackManipulation = PrimitiveWideningDelegate.forPrimitive(sourceTypeDescription).widenTo(targetTypeDescription);
        assertThat(stackManipulation.isValid(), is(false));
        stackManipulation.apply(methodVisitor, implementationContext);
    }
}
