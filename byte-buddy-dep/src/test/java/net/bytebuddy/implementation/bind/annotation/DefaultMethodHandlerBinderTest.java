package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class DefaultMethodHandlerBinderTest extends AbstractAnnotationBinderTest<DefaultMethodHandle> {

    public DefaultMethodHandlerBinderTest() {
        super(DefaultMethodHandle.class);
    }

    @Mock
    private TypeDescription targetType, interfaceType;

    @Mock
    private TypeDescription.Generic genericTargetType, genericInterfaceType;

    @Mock
    private MethodDescription.SignatureToken token;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private JavaConstant.MethodHandle methodHandle;

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<DefaultMethodHandle> getSimpleBinder() {
        return DefaultMethodHandle.Binder.INSTANCE;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(genericTargetType);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(genericInterfaceType));
        when(genericInterfaceType.asGenericType()).thenReturn(genericInterfaceType);
        when(genericInterfaceType.asErasure()).thenReturn(interfaceType);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(source.asSignatureToken()).thenReturn(token);
        when(specialMethodInvocation.withCheckedCompatibilityTo(sourceTypeToken)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.toMethodHandle()).thenReturn(methodHandle);
        when(methodHandle.toStackManipulation()).thenReturn(StackManipulation.Trivial.INSTANCE);
    }

    @Test(expected = IllegalStateException.class)
    public void testBindNoMethodParameter() throws Exception {
        DefaultMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBind() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(implementationTarget.invokeDefault(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(annotation.targetType()).thenReturn((Class) void.class);
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindNoInterface() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(implementationTarget.invokeDefault(token)).thenReturn(specialMethodInvocation);
        when(annotation.targetType()).thenReturn((Class) void.class);
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindExplicit() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(implementationTarget.invokeDefault(token, TypeDescription.ForLoadedType.of(Runnable.class))).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(annotation.targetType()).thenReturn((Class) Runnable.class);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(genericInterfaceType, genericInterfaceType));
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testBindExplicitNoInterface() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(annotation.targetType()).thenReturn((Class) Void.class);
        DefaultMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindIllegalFallback() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(annotation.nullIfImpossible()).thenReturn(true);
        when(implementationTarget.invokeDefault(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        when(annotation.targetType()).thenReturn((Class) void.class);
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethodHandle.Binder.INSTANCE.bind(annotationDescription,
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
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethodHandle.Binder.INSTANCE.bind(annotationDescription,
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
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethodHandle.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }
}
