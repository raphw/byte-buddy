package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class SuperMethodBinderTest extends AbstractAnnotationBinderTest<SuperMethod> {

    public SuperMethodBinderTest() {
        super(SuperMethod.class);
    }

    @Mock
    private TypeDescription targetType;

    @Mock
    private TypeDescription.Generic genericTargetType;

    @Mock
    private MethodDescription.SignatureToken token;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperMethod> getSimpleBinder() {
        return SuperMethod.Binder.INSTANCE;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(genericTargetType);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(source.asSignatureToken()).thenReturn(token);
    }

    @Test(expected = IllegalStateException.class)
    public void testBindNoMethodParameter() throws Exception {
        SuperMethod.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test
    public void testBind() throws Exception {
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(implementationTarget.invokeSuper(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethod.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testBindDefaultFallback() throws Exception {
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(annotation.fallbackToDefault()).thenReturn(true);
        when(implementationTarget.invokeDominant(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethod.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }

    @Test
    public void testBindIllegal() throws Exception {
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(implementationTarget.invokeSuper(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethod.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(false));
    }

    @Test
    public void testBindIllegalFallback() throws Exception {
        when(targetType.isAssignableFrom(Method.class)).thenReturn(true);
        when(source.isMethod()).thenReturn(true);
        when(annotation.nullIfImpossible()).thenReturn(true);
        when(implementationTarget.invokeSuper(token)).thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethod.Binder.INSTANCE.bind(annotationDescription,
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
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethod.Binder.INSTANCE.bind(annotationDescription,
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
        MethodDelegationBinder.ParameterBinding<?> binding = SuperMethod.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(binding.isValid(), is(true));
    }
}
