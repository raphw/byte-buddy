package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.AbstractImplementationTargetTest;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RebaseImplementationTargetTest extends AbstractImplementationTargetTest {

    private static final String BAR = "bar";

    @Mock
    private MethodDescription.InDefinedShape rebasedMethod;

    @Mock
    private MethodDescription.Token rebasedToken;

    @Mock
    private MethodDescription.SignatureToken rebasedSignatureToken;

    @Mock
    private MethodRebaseResolver.Resolution resolution;

    @Mock
    private TypeDescription rawSuperClass;

    @Mock
    private TypeDescription.Generic superClass;

    @Before
    public void setUp() throws Exception {
        when(methodGraph.locate(Mockito.any(MethodDescription.SignatureToken.class))).thenReturn(MethodGraph.Node.Unresolved.INSTANCE);
        when(instrumentedType.getSuperClass()).thenReturn(superClass);
        when(superClass.asErasure()).thenReturn(rawSuperClass);
        when(rawSuperClass.getInternalName()).thenReturn(BAR);
        when(rebasedMethod.getInternalName()).thenReturn(QUX);
        when(rebasedMethod.asToken(ElementMatchers.is(instrumentedType))).thenReturn(rebasedToken);
        when(rebasedMethod.getDescriptor()).thenReturn(FOO);
        when(rebasedMethod.asDefined()).thenReturn(rebasedMethod);
        when(rebasedMethod.getReturnType()).thenReturn(genericReturnType);
        when(rebasedMethod.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
        when(rebasedMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(rebasedMethod.asSignatureToken()).thenReturn(rebasedSignatureToken);
        super.setUp();
    }

    protected Implementation.Target makeImplementationTarget() {
        return new RebaseImplementationTarget(instrumentedType, methodGraph, defaultMethodInvocation, Collections.singletonMap(rebasedSignatureToken, resolution));
    }

    @Test
    public void testNonRebasedMethodIsInvokable() throws Exception {
        when(invokableMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(invokableMethod.isSpecializableFor(instrumentedType)).thenReturn(true);
        when(resolution.isRebased()).thenReturn(false);
        when(resolution.getResolvedMethod()).thenReturn(invokableMethod);
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeSuper(rebasedSignatureToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) invokableMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(instrumentedType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAZ, FOO, QUX, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testRebasedMethodIsInvokable() throws Exception {
        when(invokableMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(resolution.isRebased()).thenReturn(true);
        when(resolution.getResolvedMethod()).thenReturn(rebasedMethod);
        when(resolution.getAppendedParameters()).thenReturn(new TypeList.Empty());
        when(rebasedMethod.isSpecializableFor(instrumentedType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeSuper(rebasedSignatureToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) rebasedMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(instrumentedType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAZ, QUX, FOO, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testRebasedConstructorIsInvokable() throws Exception {
        when(rebasedMethod.isConstructor()).thenReturn(true);
        when(invokableMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(resolution.isRebased()).thenReturn(true);
        when(resolution.getResolvedMethod()).thenReturn(rebasedMethod);
        when(resolution.getAppendedParameters()).thenReturn(new TypeList.Explicit(TypeDescription.ForLoadedType.of(Object.class)));
        when(rebasedMethod.isSpecializableFor(instrumentedType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeSuper(rebasedSignatureToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) rebasedMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(instrumentedType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitInsn(Opcodes.ACONST_NULL);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAZ, QUX, FOO, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
    }

    @Test
    public void testNonSpecializableRebaseMethodIsNotInvokable() throws Exception {
        when(invokableMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(resolution.isRebased()).thenReturn(true);
        when(resolution.getResolvedMethod()).thenReturn(rebasedMethod);
        when(resolution.getAppendedParameters()).thenReturn(new TypeList.Empty());
        when(rebasedMethod.isSpecializableFor(instrumentedType)).thenReturn(false);
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeSuper(rebasedSignatureToken);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testSuperTypeMethodIsInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(rawSuperClass)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) invokableMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(rawSuperClass));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, FOO, QUX, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testNonSpecializableSuperClassMethodIsNotInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(rawSuperClass)).thenReturn(false);
        when(resolution.isRebased()).thenReturn(false);
        when(resolution.getResolvedMethod()).thenReturn(invokableMethod);
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testOriginType() throws Exception {
        assertThat(makeImplementationTarget().getOriginType(), is((TypeDefinition) instrumentedType));
    }
}
