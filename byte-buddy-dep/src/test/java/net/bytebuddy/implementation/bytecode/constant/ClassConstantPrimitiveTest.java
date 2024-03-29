package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.type.TypeDescription;
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
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(Parameterized.class)
public class ClassConstantPrimitiveTest {

    private final TypeDescription primitiveType, wrapperType;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public ClassConstantPrimitiveTest(Class<?> primitiveType, Class<?> wrapperType) {
        this.primitiveType = TypeDescription.ForLoadedType.of(primitiveType);
        this.wrapperType = TypeDescription.ForLoadedType.of(wrapperType);
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
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, wrapperType.getInternalName(), "TYPE", "Ljava/lang/Class;");
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }
}
