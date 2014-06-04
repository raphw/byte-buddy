package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getInternalName()).thenReturn(FOO);
    }

    @Test
    public void testTypeCreation() throws Exception {
        TypeCreation.forType(typeDescription).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitTypeInsn(Opcodes.NEW, FOO);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
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
    public void testHashCodeEquals() throws Exception {
        assertThat(TypeCreation.forType(typeDescription).hashCode(), is(TypeCreation.forType(typeDescription).hashCode()));
        assertThat(TypeCreation.forType(typeDescription), is(TypeCreation.forType(typeDescription)));
        assertThat(TypeCreation.forType(typeDescription).hashCode(), not(is(TypeCreation.forType(mock(TypeDescription.class)).hashCode())));
        assertThat(TypeCreation.forType(typeDescription), not(is(TypeCreation.forType(mock(TypeDescription.class)))));
    }
}
