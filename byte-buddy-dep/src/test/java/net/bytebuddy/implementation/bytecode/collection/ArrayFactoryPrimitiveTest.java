package net.bytebuddy.implementation.bytecode.collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class ArrayFactoryPrimitiveTest extends AbstractArrayFactoryTest {

    private final Class<?> primitiveType;

    private final int createOpcode;

    private final int storeOpcode;

    public ArrayFactoryPrimitiveTest(Class<?> primitiveType, int createOpcode, int storeOpcode) {
        this.primitiveType = primitiveType;
        this.createOpcode = createOpcode;
        this.storeOpcode = storeOpcode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Opcodes.T_BOOLEAN, Opcodes.BASTORE},
                {byte.class, Opcodes.T_BYTE, Opcodes.BASTORE},
                {short.class, Opcodes.T_SHORT, Opcodes.SASTORE},
                {char.class, Opcodes.T_CHAR, Opcodes.CASTORE},
                {int.class, Opcodes.T_INT, Opcodes.IASTORE},
                {long.class, Opcodes.T_LONG, Opcodes.LASTORE},
                {float.class, Opcodes.T_FLOAT, Opcodes.FASTORE},
                {double.class, Opcodes.T_DOUBLE, Opcodes.DASTORE},
        });
    }

    @Test
    public void testArrayCreation() throws Exception {
        testCreationUsing(primitiveType, storeOpcode);
    }

    protected void verifyArrayCreation(MethodVisitor methodVisitor) {
        verify(methodVisitor).visitIntInsn(Opcodes.NEWARRAY, createOpcode);
    }
}
