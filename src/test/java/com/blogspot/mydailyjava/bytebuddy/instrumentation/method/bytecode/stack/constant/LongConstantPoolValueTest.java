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
public class LongConstantPoolValueTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Long.MIN_VALUE},
                {Integer.MIN_VALUE},
                {-100L},
                {-2L},
                {6L},
                {7L},
                {100L},
                {Integer.MAX_VALUE},
                {Long.MAX_VALUE},
        });
    }

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private final long value;

    public LongConstantPoolValueTest(long value) {
        this.value = value;
    }

    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Test
    public void testBiPush() throws Exception {
        StackManipulation.Size size = LongConstant.forValue(value).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(2));
        assertThat(size.getMaximalSize(), is(2));
        verify(methodVisitor).visitLdcInsn(value);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }
}
