package net.bytebuddy.implementation.bytecode.assign.reference;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.After;
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

public class ReferenceTypeAwareAssignerTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private TypeDescription.Generic source, target;

    @Mock
    private TypeDescription rawSource, rawTarget;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    public void setUp() throws Exception {
        when(source.asErasure()).thenReturn(rawSource);
        when(target.asErasure()).thenReturn(rawTarget);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testMutualAssignable() throws Exception {
        defineAssignability(true, true);
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testSourceToTargetAssignable() throws Exception {
        defineAssignability(true, false);
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testTargetToSourceAssignable() throws Exception {
        defineAssignability(false, true);
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.STATIC);
        assertThat(stackManipulation.isValid(), is(false));
        stackManipulation.apply(methodVisitor, implementationContext);
    }

    @Test
    public void testTargetToSourceAssignableRuntimeType() throws Exception {
        defineAssignability(false, false);
        when(rawTarget.getInternalName()).thenReturn(FOO);
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(source, target, Assigner.Typing.DYNAMIC);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitTypeInsn(Opcodes.CHECKCAST, FOO);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testPrimitiveAssignabilityWhenEqual() throws Exception {
        TypeDescription.Generic primitiveType = new TypeDescription.Generic.OfNonGenericType.ForLoadedType(int.class); // Note: cannot mock equals
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(primitiveType, primitiveType, Assigner.Typing.DYNAMIC);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testPrimitiveAssignabilityWhenNotEqual() throws Exception {
        TypeDescription.Generic primitiveType = new TypeDescription.Generic.OfNonGenericType.ForLoadedType(int.class); // Note: cannot mock equals
        TypeDescription.Generic otherPrimitiveType = new TypeDescription.Generic.OfNonGenericType.ForLoadedType(long.class); // Note: cannot mock equals
        StackManipulation stackManipulation = ReferenceTypeAwareAssigner.INSTANCE.assign(primitiveType, otherPrimitiveType, Assigner.Typing.DYNAMIC);
        assertThat(stackManipulation.isValid(), is(false));
        stackManipulation.apply(methodVisitor, implementationContext);
    }

    private void defineAssignability(boolean sourceToTarget, boolean targetToSource) {
        if (sourceToTarget) {
            when(rawSource.isAssignableTo(rawTarget)).thenReturn(true);
            when(rawTarget.isAssignableFrom(rawSource)).thenReturn(true);
        }
        if (targetToSource) {
            when(rawTarget.isAssignableTo(rawSource)).thenReturn(true);
            when(rawSource.isAssignableFrom(rawTarget)).thenReturn(true);
        }
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ReferenceTypeAwareAssigner.class).apply();
    }
}
