package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.AbstractImplementationTargetTest;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SubclassImplementationTargetTest extends AbstractImplementationTargetTest {

    private static final String BAR = "bar", BAZ = "baz";

    @Mock
    private GenericTypeDescription superType;

    @Mock
    private TypeDescription rawSuperType;

    @Mock
    private MethodDescription.InGenericShape superTypeConstructor;

    @Mock
    private MethodDescription.InDefinedShape definedSuperTypeConstructor;

    @Mock
    private MethodDescription.Token superConstructorToken;

    @Override
    @Before
    public void setUp() throws Exception {
        when(superGraph.locate(Mockito.any(MethodDescription.Token.class))).thenReturn(MethodGraph.Node.Unresolved.INSTANCE);
        when(superGraph.locate(invokableToken)).thenReturn(new MethodGraph.Node.Simple(invokableMethod));
        when(instrumentedType.getSuperType()).thenReturn(superType);
        when(superType.asErasure()).thenReturn(rawSuperType);
        when(superType.asGenericType()).thenReturn(superType);
        when(rawSuperType.asGenericType()).thenReturn(superType);
        when(rawSuperType.asErasure()).thenReturn(rawSuperType);
        when(rawSuperType.getInternalName()).thenReturn(BAR);
        when(superType.getDeclaredMethods())
                .thenReturn(new MethodList.Explicit<MethodDescription.InGenericShape>(Collections.singletonList(superTypeConstructor)));
        when(superTypeConstructor.asDefined()).thenReturn(definedSuperTypeConstructor);
        when(definedSuperTypeConstructor.getReturnType()).thenReturn(GenericTypeDescription.VOID);
        when(definedSuperTypeConstructor.getDeclaringType()).thenReturn(rawSuperType);
        when(definedSuperTypeConstructor.isConstructor()).thenReturn(true);
        when(superTypeConstructor.isVisibleTo(instrumentedType)).thenReturn(true);
        when(superTypeConstructor.asToken()).thenReturn(superConstructorToken);
        when(definedSuperTypeConstructor.getInternalName()).thenReturn(QUX);
        when(definedSuperTypeConstructor.getDescriptor()).thenReturn(BAZ);
        when(superTypeConstructor.isConstructor()).thenReturn(true);
        when(superTypeConstructor.getDeclaringType()).thenReturn(superType);
        when(superTypeConstructor.getReturnType()).thenReturn(GenericTypeDescription.VOID);
        when(superTypeConstructor.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InGenericShape>());
        when(invokableToken.getInternalName()).thenReturn(FOO);
        when(superConstructorToken.getInternalName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
        super.setUp();
    }

    @Override
    protected Implementation.Target makeImplementationTarget() {
        return new SubclassImplementationTarget(instrumentedType, methodGraph, SubclassImplementationTarget.OriginTypeResolver.SUPER_TYPE);
    }

    @Test
    public void testSuperTypeMethodIsInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(rawSuperType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) invokableMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(rawSuperType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, FOO, QUX, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testNonSpecializableSuperTypeMethodIsNotInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(rawSuperType)).thenReturn(false);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testSuperConstructorIsInvokable() throws Exception {
        when(invokableMethod.isConstructor()).thenReturn(true);
        when(definedSuperTypeConstructor.isSpecializableFor(rawSuperType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(superConstructorToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) superTypeConstructor));
        assertThat(specialMethodInvocation.getTypeDescription(), is(rawSuperType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, QUX, BAZ, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassImplementationTarget.class).apply();
        ObjectPropertyAssertion.of(SubclassImplementationTarget.OriginTypeResolver.class).apply();
    }
}
