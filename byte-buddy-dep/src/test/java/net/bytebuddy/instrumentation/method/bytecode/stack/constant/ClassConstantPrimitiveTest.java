package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ClassConstantPrimitiveTest {

    private final TypeDescription primitiveType, wrapperType;
    @Rule
    public TestRule mockitoRule = new MockitoRule(this);
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    public ClassConstantPrimitiveTest(Class<?> primitiveType, Class<?> wrapperType) {
        this.primitiveType = new TypeDescription.ForLoadedType(primitiveType);
        this.wrapperType = new TypeDescription.ForLoadedType(wrapperType);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {void.class, Void.class},
                {boolean.class, Boolean.class},
                {byte.class, Byte.class},
                {short.class, Short.class},
                {char.class, Character.class},
                {int.class, Integer.class},
                {long.class, Long.class},
                {float.class, Float.class},
                {double.class, Double.class}
        });
    }

    @Test
    public void testClassConstant() throws Exception {
        StackManipulation stackManipulation = ClassConstant.of(primitiveType);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, wrapperType.getInternalName(), "TYPE", "Ljava/lang/Class;");
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }
}
