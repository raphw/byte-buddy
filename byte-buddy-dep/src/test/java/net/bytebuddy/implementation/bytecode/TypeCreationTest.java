package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeCreationTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getInternalName()).thenReturn(FOO);
    }

    @Test
    public void testTypeCreation() throws Exception {
        StackManipulation stackManipulation = TypeCreation.forType(typeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitTypeInsn(Opcodes.NEW, FOO);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeCreationArray() throws Exception {
        when(typeDescription.isArray()).thenReturn(true);
        TypeCreation.forType(typeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeCreationPrimitive() throws Exception {
        when(typeDescription.isPrimitive()).thenReturn(true);
        TypeCreation.forType(typeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeCreationAbstract() throws Exception {
        when(typeDescription.isAbstract()).thenReturn(true);
        TypeCreation.forType(typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeCreation.class).apply();
    }
}
