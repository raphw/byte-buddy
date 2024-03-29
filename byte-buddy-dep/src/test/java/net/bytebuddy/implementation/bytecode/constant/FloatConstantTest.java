package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(Parameterized.class)
public class FloatConstantTest {

    private final float value;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public FloatConstantTest(float value) {
        this.value = value;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Float.MIN_VALUE},
                {-100f},
                {-2f},
                {0.5f},
                {6f},
                {7f},
                {100f},
                {Float.MAX_VALUE},
        });
    }

    @Test
    public void testBiPush() throws Exception {
        StackManipulation floatConstant = FloatConstant.forValue(value);
        assertThat(floatConstant.isValid(), is(true));
        StackManipulation.Size size = floatConstant.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitLdcInsn(value);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }
}
