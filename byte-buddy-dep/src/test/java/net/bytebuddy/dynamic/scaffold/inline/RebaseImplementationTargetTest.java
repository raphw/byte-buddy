package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.AbstractImplementationTargetTest;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
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

public class RebaseImplementationTargetTest extends AbstractImplementationTargetTest {

    private static final String BAR = "bar";

    @Mock
    private MethodRebaseResolver methodRebaseResolver;

    @Mock
    private MethodDescription.InDefinedShape rebasedMethod;

    @Mock
    private MethodDescription.Token rebasedToken;

    @Mock
    private MethodRebaseResolver.Resolution resolution;

    @Mock
    private TypeDescription superType;

    @Override
    @Before
    public void setUp() throws Exception {
        when(methodGraph.locate(Mockito.any(MethodDescription.Token.class))).thenReturn(MethodGraph.Node.Unresolved.INSTANCE);
        when(instrumentedType.getSuperType()).thenReturn(superType);
        when(superType.asErasure()).thenReturn(superType);
        when(superType.getInternalName()).thenReturn(BAR);
        when(rebasedMethod.getInternalName()).thenReturn(QUX);
        when(rebasedMethod.getDescriptor()).thenReturn(FOO);
        when(rebasedMethod.asDefined()).thenReturn(rebasedMethod);
        when(rebasedMethod.getReturnType()).thenReturn(returnType);
        when(rebasedMethod.getParameters()).thenReturn(new ParameterList.Empty());
        when(rebasedMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(rebasedMethod.asToken()).thenReturn(rebasedToken);
        when(methodRebaseResolver.resolve(rebasedMethod)).thenReturn(resolution);
        super.setUp();
    }

    @Override
    protected Implementation.Target makeImplementationTarget() {
        return RebaseImplementationTarget.of(instrumentedType,
                methodGraph,
                new MethodList.Explicit<MethodDescription.InDefinedShape>(Collections.singletonList(rebasedMethod)),
                methodRebaseResolver);
    }

    @Test
    public void testNonRebasedMethodIsInvokable() throws Exception {
        when(invokableMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(invokableMethod.isSpecializableFor(instrumentedType)).thenReturn(true);
        when(resolution.isRebased()).thenReturn(false);
        when(resolution.getResolvedMethod()).thenReturn(invokableMethod);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(rebasedToken);
        verify(methodRebaseResolver).resolve(rebasedMethod);
        verifyNoMoreInteractions(methodRebaseResolver);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) invokableMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(instrumentedType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAZ, FOO, QUX, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testRebasedMethodIsInvokable() throws Exception {
        when(invokableMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(resolution.isRebased()).thenReturn(true);
        when(resolution.getResolvedMethod()).thenReturn(rebasedMethod);
        when(resolution.getAdditionalArguments()).thenReturn(StackManipulation.Trivial.INSTANCE);
        when(rebasedMethod.isSpecializableFor(instrumentedType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(rebasedToken);
        verify(methodRebaseResolver).resolve(rebasedMethod);
        verifyNoMoreInteractions(methodRebaseResolver);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) rebasedMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(instrumentedType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAZ, QUX, FOO, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testRebasedConstructorIsInvokable() throws Exception {
        when(rebasedMethod.isConstructor()).thenReturn(true);
        when(invokableMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(resolution.isRebased()).thenReturn(true);
        when(resolution.getResolvedMethod()).thenReturn(rebasedMethod);
        when(resolution.getAdditionalArguments()).thenReturn(NullConstant.INSTANCE);
        when(rebasedMethod.isSpecializableFor(instrumentedType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(rebasedToken);
        verify(methodRebaseResolver).resolve(rebasedMethod);
        verifyNoMoreInteractions(methodRebaseResolver);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) rebasedMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(instrumentedType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitInsn(Opcodes.ACONST_NULL);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAZ, QUX, FOO, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
    }

    @Test
    public void testNonSpecializableRebaseMethodIsNotInvokable() throws Exception {
        when(invokableMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(resolution.isRebased()).thenReturn(true);
        when(resolution.getResolvedMethod()).thenReturn(rebasedMethod);
        when(resolution.getAdditionalArguments()).thenReturn(StackManipulation.Trivial.INSTANCE);
        when(rebasedMethod.isSpecializableFor(instrumentedType)).thenReturn(false);
        when(methodRebaseResolver.resolve(invokableMethod)).thenReturn(new MethodRebaseResolver.Resolution.Preserved(invokableMethod));
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(rebasedToken);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testSuperTypeMethodIsInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(superType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) invokableMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(superType));
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
        when(invokableMethod.isSpecializableFor(superType)).thenReturn(false);
        when(resolution.isRebased()).thenReturn(false);
        when(resolution.getResolvedMethod()).thenReturn(invokableMethod);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testOriginType() throws Exception {
        assertThat(implementationTarget.getOriginType(), is(instrumentedType));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(RebaseImplementationTarget.class).apply();
    }
}
