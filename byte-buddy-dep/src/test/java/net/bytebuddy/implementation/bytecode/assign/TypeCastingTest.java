package net.bytebuddy.implementation.bytecode.assign;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeCastingTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoTest = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testCasting() throws Exception {
        when(typeDescription.getInternalName()).thenReturn(FOO);
        StackManipulation.Size size = new TypeCasting(typeDescription).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitTypeInsn(Opcodes.CHECKCAST, FOO);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveCastingThrowsException() throws Exception {
        when(typeDescription.isPrimitive()).thenReturn(true);
        TypeCasting.to(typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeCasting.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when((mock).getInternalName()).thenReturn(FOO + new Random().nextInt());
            }
        }).apply();
    }
}
