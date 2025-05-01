package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
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
public class NegationTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Negation.INTEGER, Opcodes.INEG},
                {Negation.LONG, Opcodes.LNEG},
                {Negation.FLOAT, Opcodes.FNEG},
                {Negation.DOUBLE, Opcodes.DNEG}
        });
    }

    private final StackManipulation stackManipulation;

    private final int opcodes;

    public NegationTest(StackManipulation stackManipulation, int opcodes) {
        this.stackManipulation = stackManipulation;
        this.opcodes = opcodes;
    }

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testNegation() {
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getMaximalSize(), is(0));
        assertThat(size.getSizeImpact(), is(0));
        verify(methodVisitor).visitInsn(opcodes);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }
}
