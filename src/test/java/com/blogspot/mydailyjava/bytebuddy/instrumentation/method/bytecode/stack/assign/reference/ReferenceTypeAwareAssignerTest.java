package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.reference;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ReferenceTypeAwareAssignerTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private TypeDescription sourceTypeDescription, targetTypeDescription;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testMutualAssignable() throws Exception {
        defineAssignability(true, true);
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testSourceToTargetAssignable() throws Exception {
        defineAssignability(true, false);
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testTargetToSourceAssignable() throws Exception {
        defineAssignability(false, true);
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(sourceTypeDescription, targetTypeDescription, false);
        assertThat(stackManipulation.isValid(), is(false));
        stackManipulation.apply(methodVisitor, instrumentationContext);
    }

    @Test
    public void testTargetToSourceAssignableRuntimeType() throws Exception {
        defineAssignability(false, false);
        when(targetTypeDescription.getInternalName()).thenReturn(FOO);
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(sourceTypeDescription, targetTypeDescription, true);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitTypeInsn(Opcodes.CHECKCAST, FOO);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testPrimitiveAssignabilityWhenEqual() throws Exception {
        TypeDescription primitiveType = new TypeDescription.ForLoadedType(int.class); // Note: cannot mock equals
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(primitiveType, primitiveType, true);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testPrimitiveAssignabilityWhenNotEqual() throws Exception {
        TypeDescription primitiveType = new TypeDescription.ForLoadedType(int.class); // Note: cannot mock equals
        TypeDescription otherPrimitiveType = new TypeDescription.ForLoadedType(long.class); // Note: cannot mock equals
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(primitiveType, otherPrimitiveType, true);
        assertThat(stackManipulation.isValid(), is(false));
        stackManipulation.apply(methodVisitor, instrumentationContext);
    }

    private void defineAssignability(boolean sourceToTarget, boolean targetToSource) {
        if (sourceToTarget) {
            when(sourceTypeDescription.isAssignableTo(targetTypeDescription)).thenReturn(true);
            when(targetTypeDescription.isAssignableFrom(sourceTypeDescription)).thenReturn(true);
        }
        if (targetToSource) {
            when(targetTypeDescription.isAssignableTo(sourceTypeDescription)).thenReturn(true);
            when(sourceTypeDescription.isAssignableFrom(targetTypeDescription)).thenReturn(true);
        }
    }
}
