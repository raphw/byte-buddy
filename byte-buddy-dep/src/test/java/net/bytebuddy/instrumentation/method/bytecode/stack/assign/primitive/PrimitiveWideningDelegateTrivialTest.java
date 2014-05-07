package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveWideningDelegateTrivialTest {

    private final Class<?> sourceType;
    private final Class<?> targetType;
    @Rule
    public TestRule mockitoRule = new MockitoRule(this);
    @Mock
    private TypeDescription sourceTypeDescription, targetTypeDescription;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    public PrimitiveWideningDelegateTrivialTest(Class<?> sourceType, Class<?> targetType) {
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

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

    @Before
    public void setUp() throws Exception {
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testNoOpAssignment() throws Exception {
        StackManipulation stackManipulation = PrimitiveWideningDelegate.forPrimitive(sourceTypeDescription).widenTo(targetTypeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(sourceTypeDescription, atLeast(1)).represents(sourceType);
        verify(targetTypeDescription, atLeast(1)).represents(sourceType);
    }
}
