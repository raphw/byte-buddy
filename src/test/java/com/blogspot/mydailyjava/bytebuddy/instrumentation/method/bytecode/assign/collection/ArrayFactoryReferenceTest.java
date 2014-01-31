package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.collection;

import org.junit.Before;
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

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Object.class},
                {Object[].class},
                {String.class},
        });
    }

    private final Class<?> type;

    public ArrayFactoryReferenceTest(Class<?> type) {
        this.type = type;
    }

    private String internalTypeName;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        internalTypeName = Type.getInternalName(type);
    }

    @Test
    public void testArrayCreation() throws Exception {
        testCreation(type, Opcodes.AASTORE);

    }

    @Override
    protected void verifyArrayCreation(MethodVisitor methodVisitor) {
        verify(methodVisitor).visitTypeInsn(Opcodes.ANEWARRAY, internalTypeName);
    }
}
