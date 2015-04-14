package net.bytebuddy.implementation.bytecode.collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class ArrayFactoryReferenceTest extends AbstractArrayFactoryTest {

    private final Class<?> type;

    private final String internalTypeName;

    public ArrayFactoryReferenceTest(Class<?> type) {
        this.type = type;
        internalTypeName = Type.getInternalName(type);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Object.class},
                {Object[].class},
                {String.class},
        });
    }

    @Test
    public void testArrayCreation() throws Exception {
        testCreationUsing(type, Opcodes.AASTORE);
    }

    @Override
    protected void verifyArrayCreation(MethodVisitor methodVisitor) {
        verify(methodVisitor).visitTypeInsn(Opcodes.ANEWARRAY, internalTypeName);
    }
}
