package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
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
public class ArrayAccessTest {

    private final TypeDescription typeDescription;

    private final int loadOpcode, storeOpcode;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public ArrayAccessTest(Class<?> type, int loadOpcode, int storeOpcode) {
        typeDescription = new TypeDescription.ForLoadedType(type);
        this.loadOpcode = loadOpcode;
        this.storeOpcode = storeOpcode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Opcodes.BALOAD, Opcodes.BASTORE},
                {byte.class, Opcodes.BALOAD, Opcodes.BASTORE},
                {short.class, Opcodes.SALOAD, Opcodes.SASTORE},
                {char.class, Opcodes.CALOAD, Opcodes.CASTORE},
                {int.class, Opcodes.IALOAD, Opcodes.IASTORE},
                {long.class, Opcodes.LALOAD, Opcodes.LASTORE},
                {float.class, Opcodes.FALOAD, Opcodes.FASTORE},
                {double.class, Opcodes.DALOAD, Opcodes.DASTORE},
                {Object.class, Opcodes.AALOAD, Opcodes.AASTORE},
        });
    }

    @Test
    public void testLoad() throws Exception {
        ArrayAccess arrayAccess = ArrayAccess.of(typeDescription);
        assertThat(arrayAccess.load().isValid(), is(true));
        StackManipulation.Size size = arrayAccess.load().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(typeDescription.getStackSize().getSize() - 2));
        assertThat(size.getMaximalSize(), is(typeDescription.getStackSize().getSize()));
        verify(methodVisitor).visitInsn(loadOpcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testStore() throws Exception {
        ArrayAccess arrayAccess = ArrayAccess.of(typeDescription);
        assertThat(arrayAccess.store().isValid(), is(true));
        StackManipulation.Size size = arrayAccess.store().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(-(typeDescription.getStackSize().getSize() + 2)));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInsn(storeOpcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }
}
