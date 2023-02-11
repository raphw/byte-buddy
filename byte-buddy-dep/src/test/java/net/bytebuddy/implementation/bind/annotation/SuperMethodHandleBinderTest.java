package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class SuperMethodHandleBinderTest extends AbstractAnnotationBinderTest<SuperMethodHandle> {

    public SuperMethodHandleBinderTest() {
        super(SuperMethodHandle.class);
    }

    @Mock
    private TypeDescription targetType;

    @Mock
    private TypeDescription.Generic genericTargetType;

    @Mock
    private MethodDescription.SignatureToken token;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private JavaConstant.MethodHandle methodHandle;

    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperMethodHandle> getSimpleBinder() {
        return SuperMethodHandle.Binder.INSTANCE;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(genericTargetType);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(source.asSignatureToken()).thenReturn(token);
        when(specialMethodInvocation.withCheckedCompatibilityTo(sourceTypeToken)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.toMethodHandle()).thenReturn(methodHandle);
        when(methodHandle.toStackManipulation()).thenReturn(StackManipulation.Trivial.INSTANCE);
    }

    @Test(expected = IllegalStateException.class)
    public void testBindNoMethodParameter() throws Exception {
        SuperMethodHandle.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test
    public void testBind() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(implementationTarget.invokeSuper(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testBindDefaultFallback() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(annotation.fallbackToDefault()).thenReturn(true);
        when(implementationTarget.invokeDominant(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testBindIllegal() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(implementationTarget.invokeSuper(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(false));
    }

    @Test
    public void testBindIllegalFallback() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(annotation.nullIfImpossible()).thenReturn(true);
        when(implementationTarget.invokeSuper(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testNoMethod() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(false);
        when(annotation.nullIfImpossible()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(false));
    }

    @Test
    public void testNoMethodFallback() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(false);
        when(annotation.nullIfImpossible()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }
}
