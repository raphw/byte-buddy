package net.bytebuddy.implementation.bytecode.assign;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class InstanceCheckTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private TypeDescription typeDescription;

    @Test
    public void testInstanceCheck() {
        when(typeDescription.getInternalName()).thenReturn(FOO);
        StackManipulation stackManipulation = InstanceCheck.of(typeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(typeDescription).isPrimitive();
        verify(typeDescription).getInternalName();
        verifyNoMoreInteractions(typeDescription);
        verify(methodVisitor).visitTypeInsn(Opcodes.INSTANCEOF, FOO);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstanceCheckPrimitiveIllegal() {
        when(typeDescription.isPrimitive()).thenReturn(true);
        InstanceCheck.of(typeDescription);
    }
}
