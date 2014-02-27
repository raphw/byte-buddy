package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class FloatConstantPoolValueTest {

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

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private final float value;

    public FloatConstantPoolValueTest(float value) {
        this.value = value;
    }

    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Test
    public void testBiPush() throws Exception {
        StackManipulation.Size size = FloatConstant.forValue(value).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitLdcInsn(value);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }
}
