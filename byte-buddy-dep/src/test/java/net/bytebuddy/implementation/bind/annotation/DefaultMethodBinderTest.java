package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class DefaultMethodBinderTest extends AbstractAnnotationBinderTest<DefaultMethod> {

    public DefaultMethodBinderTest() {
        super(DefaultMethod.class);
    }

    @Mock
    private TypeDescription targetType, interfaceType;

    @Mock
    private TypeDescription.Generic genericTargetType, genericInterfaceType;

    @Mock
    private MethodDescription.SignatureToken token;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<DefaultMethod> getSimpleBinder() {
        return DefaultMethod.Binder.INSTANCE;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(genericTargetType);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(genericInterfaceType));
        when(genericInterfaceType.asGenericType()).thenReturn(genericInterfaceType);
        when(genericInterfaceType.asErasure()).thenReturn(interfaceType);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(source.asSignatureToken()).thenReturn(token);
    }

    @Test(expected = IllegalStateException.class)
    public void testBindNoMethodParameter() throws Exception {
        DefaultMethod.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBind() throws Exception {
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(implementationTarget.invokeDefault(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(annotation.targetType()).thenReturn((Class) void.class);
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethod.Binder.INSTANCE.bind(annotationDescription,
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
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(implementationTarget.invokeDefault(token)).thenReturn(specialMethodInvocation);
        when(annotation.targetType()).thenReturn((Class) void.class);
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethod.Binder.INSTANCE.bind(annotationDescription,
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
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(implementationTarget.invokeDefault(token, new TypeDescription.ForLoadedType(Runnable.class))).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(annotation.targetType()).thenReturn((Class) Runnable.class);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(genericInterfaceType, genericInterfaceType));
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethod.Binder.INSTANCE.bind(annotationDescription,
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
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(annotation.targetType()).thenReturn((Class) Void.class);
        DefaultMethod.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindIllegalFallback() throws Exception {
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(annotation.nullIfImpossible()).thenReturn(true);
        when(implementationTarget.invokeDefault(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        when(annotation.targetType()).thenReturn((Class) void.class);
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethod.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testNoMethod() throws Exception {
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(false);
        when(annotation.nullIfImpossible()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethod.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(false));
    }

    @Test
    public void testNoMethodFallback() throws Exception {
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(false);
        when(annotation.nullIfImpossible()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = DefaultMethod.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DefaultMethod.Binder.class).apply();
        ObjectPropertyAssertion.of(DefaultMethod.Binder.MethodLocator.ForExplicitType.class).apply();
        ObjectPropertyAssertion.of(DefaultMethod.Binder.MethodLocator.ForImplicitType.class).apply();
        ObjectPropertyAssertion.of(DefaultMethod.Binder.DelegationMethod.class).apply();
    }
}
