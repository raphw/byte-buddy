package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class IntegerConstantTest {

    private final int value;

    private final PushType pushType;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public IntegerConstantTest(int value, PushType pushType) {
        this.value = value;
        this.pushType = pushType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Integer.MIN_VALUE, PushType.LDC},
                {Short.MIN_VALUE - 1, PushType.LDC},
                {Short.MIN_VALUE, PushType.SIPUSH},
                {Byte.MIN_VALUE - 1, PushType.SIPUSH},
                {Byte.MIN_VALUE, PushType.BIPUSH},
                {-100, PushType.BIPUSH},
                {-2, PushType.BIPUSH},
                {6, PushType.BIPUSH},
                {7, PushType.BIPUSH},
                {100, PushType.BIPUSH},
                {Byte.MAX_VALUE, PushType.BIPUSH},
                {Byte.MAX_VALUE + 1, PushType.SIPUSH},
                {Short.MAX_VALUE, PushType.SIPUSH},
                {Short.MAX_VALUE + 1, PushType.LDC},
                {Integer.MAX_VALUE, PushType.LDC},
        });
    }

    @Test
    public void testBiPush() throws Exception {
        StackManipulation integerConstant = IntegerConstant.forValue(value);
        assertThat(integerConstant.isValid(), is(true));
        StackManipulation.Size size = integerConstant.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        pushType.verifyInstruction(methodVisitor, value);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(IntegerConstant.SingleBytePush.class).apply();
        ObjectPropertyAssertion.of(IntegerConstant.TwoBytePush.class).apply();
        ObjectPropertyAssertion.of(IntegerConstant.ConstantPool.class).apply();
    }

    private enum PushType {

        BIPUSH,
        SIPUSH,
        LDC;

        private void verifyInstruction(MethodVisitor methodVisitor, int value) {
            switch (this) {
                case BIPUSH:
                    verify(methodVisitor).visitIntInsn(Opcodes.BIPUSH, value);
                    break;
                case SIPUSH:
                    verify(methodVisitor).visitIntInsn(Opcodes.SIPUSH, value);
                    break;
                case LDC:
                    verify(methodVisitor).visitLdcInsn(value);
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }
}
