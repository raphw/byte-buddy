package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.MethodLookupEngine;
import net.bytebuddy.implementation.AbstractImplementationTargetTest;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class RebaseImplementationTargetTest extends AbstractImplementationTargetTest {

    private static final String BAR = "bar", BAZ = "baz", FOOBAR = "foobar", QUX = "qux";

    @Mock
    private MethodRebaseResolver methodRebaseResolver;

    @Mock
    private MethodRebaseResolver.Resolution rebasedResolution, nonRebasedResolution;

    @Mock
    private StackManipulation additionalArguments;

    @Mock
    private MethodDescription targetRebaseMethod, rebasedMethod, nonRebasedMethod, superMethod;

    @Mock
    private TypeDescription superType;

    @Override
    @Before
    public void setUp() throws Exception {
        when(instrumentedType.asRawType()).thenReturn(instrumentedType);
        when(instrumentedType.getInternalName()).thenReturn(BAR);
        when(targetRebaseMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(rebasedMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(methodRebaseResolver.resolve(targetRebaseMethod)).thenReturn(rebasedResolution);
        when(rebasedResolution.isRebased()).thenReturn(true);
        when(rebasedResolution.getResolvedMethod()).thenReturn(rebasedMethod);
        when(rebasedMethod.getReturnType()).thenReturn(returnType);
        when(rebasedMethod.getInternalName()).thenReturn(BAZ);
        when(rebasedMethod.getDescriptor()).thenReturn(FOOBAR);
        when(rebasedMethod.isSpecializableFor(instrumentedType)).thenReturn(true);
        when(rebasedResolution.getAdditionalArguments()).thenReturn(additionalArguments);
        when(additionalArguments.isValid()).thenReturn(true);
        when(additionalArguments.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(instrumentedType.getSuperType()).thenReturn(superType);
        when(superType.getInternalName()).thenReturn(QUX);
        when(superType.getStackSize()).thenReturn(StackSize.ZERO);
        when(superType.asRawType()).thenReturn(superType);
        when(superMethod.getDeclaringType()).thenReturn(superType);
        when(superMethod.getReturnType()).thenReturn(returnType);
        when(superMethod.getInternalName()).thenReturn(BAZ);
        when(superMethod.getDescriptor()).thenReturn(FOOBAR);
        when(nonRebasedMethod.getDeclaringType()).thenReturn(instrumentedType);
        when(nonRebasedMethod.getReturnType()).thenReturn(returnType);
        when(nonRebasedMethod.getInternalName()).thenReturn(BAZ);
        when(nonRebasedMethod.getDescriptor()).thenReturn(FOOBAR);
        when(methodRebaseResolver.resolve(nonRebasedMethod)).thenReturn(nonRebasedResolution);
        when(nonRebasedResolution.isRebased()).thenReturn(false);
        when(nonRebasedResolution.getResolvedMethod()).thenReturn(nonRebasedMethod);
        super.setUp();
    }

    @Override
    protected Implementation.Target makeImplementationTarget() {
        return new RebaseImplementationTarget(finding, bridgeMethodResolverFactory, methodRebaseResolver);
    }

    @Test
    public void testRebasedMethodIsInvokable() throws Exception {
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(targetRebaseMethod, methodLookup);
        verify(methodRebaseResolver).resolve(targetRebaseMethod);
        verifyNoMoreInteractions(methodRebaseResolver);
        assertThat(specialMethodInvocation.isValid(), is(true));
        verify(additionalArguments).isValid();
        assertThat(specialMethodInvocation.getMethodDescription(), is(rebasedMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(instrumentedType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, BAZ, FOOBAR, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testAbstractRebasedMethodIsNotInvokable() throws Exception {
        when(rebasedMethod.isAbstract()).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(targetRebaseMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(false));
        verify(methodRebaseResolver).resolve(targetRebaseMethod);
        verifyNoMoreInteractions(methodRebaseResolver);
    }

    @Test
    public void testNonRebasedMethodIsInvokable() throws Exception {
        when(nonRebasedMethod.isSpecializableFor(instrumentedType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(nonRebasedMethod, methodLookup);
        verify(methodRebaseResolver).resolve(nonRebasedMethod);
        verifyNoMoreInteractions(methodRebaseResolver);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is(nonRebasedMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(instrumentedType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, BAZ, FOOBAR, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testAbstractNonRebasedMethodIsNotInvokable() throws Exception {
        when(nonRebasedMethod.isAbstract()).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(nonRebasedMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(false));
        verify(methodRebaseResolver).resolve(nonRebasedMethod);
        verifyNoMoreInteractions(methodRebaseResolver);
    }

    @Test
    public void testSuperTypeMethodIsInvokable() throws Exception {
        when(superMethod.isSpecializableFor(superType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(superMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is(superMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(superType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, QUX, BAZ, FOOBAR, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodRebaseResolver);
    }

    @Test
    public void testAbstractSuperTypeMethodIsNotInvokable() throws Exception {
        when(superMethod.isSpecializableFor(superType)).thenReturn(true);
        when(superMethod.isAbstract()).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(superMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(false));
        verifyZeroInteractions(methodRebaseResolver);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(RebaseImplementationTarget.class).refine(new ObjectPropertyAssertion.Refinement<MethodLookupEngine.Finding>() {
            @Override
            public void apply(MethodLookupEngine.Finding mock) {
                when(mock.getTypeDescription()).thenReturn(mock(TypeDescription.class));
                when(mock.getInvokableMethods()).thenReturn(new MethodList.Empty());
                when(mock.getInvokableDefaultMethods()).thenReturn(Collections.<TypeDescription, Set<MethodDescription>>emptyMap());
            }
        }).refine(new ObjectPropertyAssertion.Refinement<BridgeMethodResolver.Factory>() {
            @Override
            public void apply(BridgeMethodResolver.Factory mock) {
                when(mock.make(any(MethodList.class))).thenReturn(mock(BridgeMethodResolver.class));
            }
        }).apply();
    }
}
