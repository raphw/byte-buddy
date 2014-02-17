package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.test.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveWideningDelegateNontrivialTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {byte.class, long.class, 1, Opcodes.I2L},
                {byte.class, float.class, 0, Opcodes.I2F},
                {byte.class, double.class, 1, Opcodes.I2L},
                {short.class, long.class, 1, Opcodes.I2L},
                {short.class, float.class, 0, Opcodes.I2F},
                {short.class, double.class, 1, Opcodes.I2D},
                {char.class, long.class, 1, Opcodes.I2L},
                {char.class, float.class, 0, Opcodes.I2F},
                {char.class, double.class, 1, Opcodes.I2D},
                {int.class, long.class, 1, Opcodes.I2L},
                {int.class, float.class, 0, Opcodes.I2F},
                {int.class, double.class, 1, Opcodes.I2D},
                {long.class, float.class, -1, Opcodes.L2F},
                {long.class, double.class, 0, Opcodes.L2D},
                {float.class, double.class, 1, Opcodes.F2D}
        });
    }

    private final Class<?> sourceType;
    private final Class<?> targetType;
    private final int sizeChange;
    private final int opcode;

    public PrimitiveWideningDelegateNontrivialTest(Class<?> sourceType,
                                                   Class<?> targetType,
                                                   int sizeChange,
                                                   int opcode) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.sizeChange = sizeChange;
        this.opcode = opcode;
    }

    @Mock
    private TypeDescription sourceTypeDescription, targetTypeDescription;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        when(targetTypeDescription.represents(targetType)).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testWideningConversion() throws Exception {
        StackManipulation stackManipulation = PrimitiveWideningDelegate.forPrimitive(sourceTypeDescription).widenTo(targetTypeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(Math.max(0, sizeChange)));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
