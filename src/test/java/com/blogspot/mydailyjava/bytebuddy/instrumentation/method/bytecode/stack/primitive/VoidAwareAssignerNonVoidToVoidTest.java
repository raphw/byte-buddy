package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.Assigner;
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
public class VoidAwareAssignerNonVoidToVoidTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {byte.class, Opcodes.POP},
                {short.class, Opcodes.POP},
                {char.class, Opcodes.POP},
                {int.class, Opcodes.POP},
                {long.class, Opcodes.POP2},
                {float.class, Opcodes.POP},
                {double.class, Opcodes.POP2},
                {Object.class, Opcodes.POP}
        });
    }

    private final Class<?> sourceType;
    private final int opcode;

    public VoidAwareAssignerNonVoidToVoidTest(Class<?> sourceType, int opcode) {
        this.sourceType = sourceType;
        this.opcode = opcode;
    }

    @Mock
    private TypeDescription sourceTypeDescription, targetTypeDescription;
    @Mock
    private Assigner chainedAssigner;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(sourceTypeDescription.represents(sourceType)).thenReturn(true);
        if (sourceType.isPrimitive()) {
            when(sourceTypeDescription.isPrimitive()).thenReturn(true);
        }
        when(targetTypeDescription.represents(void.class)).thenReturn(true);
        when(targetTypeDescription.isPrimitive()).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(chainedAssigner);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testAssignDefaultValue() throws Exception {
        testAssignDefaultValue(true);
    }

    @Test
    public void testAssignNoDefaultValue() throws Exception {
        testAssignDefaultValue(false);
    }

    private void testAssignDefaultValue(boolean defaultValue) throws Exception {
        Assigner voidAwareAssigner = new VoidAwareAssigner(chainedAssigner, defaultValue);
        StackManipulation stackManipulation = voidAwareAssigner.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(-1 * StackSize.of(sourceType).getSize()));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
