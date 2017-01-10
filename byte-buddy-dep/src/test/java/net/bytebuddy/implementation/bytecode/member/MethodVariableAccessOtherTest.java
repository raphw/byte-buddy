package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodVariableAccessOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testVoidArgument() throws Exception {
        TypeDescription voidTypeDescription = mock(TypeDescription.class);
        when(voidTypeDescription.isPrimitive()).thenReturn(true);
        when(voidTypeDescription.represents(void.class)).thenReturn(true);
        MethodVariableAccess.of(voidTypeDescription);
    }

    @Test
    public void testIncrement() throws Exception {
        StackManipulation stackManipulation = MethodVariableAccess.INTEGER.increment(4, 1);
        assertThat(stackManipulation.isValid(), is(true));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitIincInsn(4, 1);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testThisReference() throws Exception {
        assertThat(MethodVariableAccess.loadThis(), is(MethodVariableAccess.REFERENCE.loadFrom(0)));
    }

    @Test
    public void testLoadParameter() throws Exception {
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getType()).thenReturn(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(int.class));
        when(parameterDescription.getOffset()).thenReturn(4);
        assertThat(MethodVariableAccess.load(parameterDescription), is(MethodVariableAccess.INTEGER.loadFrom(4)));
    }

    @Test
    public void testStoreParameter() throws Exception {
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getType()).thenReturn(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(int.class));
        when(parameterDescription.getOffset()).thenReturn(4);
        assertThat(MethodVariableAccess.store(parameterDescription), is(MethodVariableAccess.INTEGER.storeAt(4)));
    }

    @Test
    public void testIncrementParameter() throws Exception {
        ParameterDescription parameterDescription = mock(ParameterDescription.class);
        when(parameterDescription.getType()).thenReturn(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(int.class));
        when(parameterDescription.getOffset()).thenReturn(4);
        assertThat(MethodVariableAccess.increment(parameterDescription, 42), is(MethodVariableAccess.INTEGER.increment(4, 42)));
    }

    @Test(expected = IllegalStateException.class)
    public void testReferenceCannotIncrement() throws Exception {
        MethodVariableAccess.REFERENCE.increment(0, 1);
    }

    @Test(expected = IllegalStateException.class)
    public void testLongCannotIncrement() throws Exception {
        MethodVariableAccess.LONG.increment(0, 1);
    }

    @Test(expected = IllegalStateException.class)
    public void testFloatCannotIncrement() throws Exception {
        MethodVariableAccess.FLOAT.increment(0, 1);
    }

    @Test(expected = IllegalStateException.class)
    public void testDoubleCannotIncrement() throws Exception {
        MethodVariableAccess.DOUBLE.increment(0, 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodVariableAccess.class).apply();
        ObjectPropertyAssertion.of(MethodVariableAccess.OffsetLoading.class).apply();
        ObjectPropertyAssertion.of(MethodVariableAccess.OffsetWriting.class).apply();
        ObjectPropertyAssertion.of(MethodVariableAccess.OffsetIncrementing.class).apply();
        ObjectPropertyAssertion.of(MethodVariableAccess.MethodLoading.class).apply();
        ObjectPropertyAssertion.of(MethodVariableAccess.MethodLoading.TypeCastingHandler.NoOp.class).apply();
    }
}
