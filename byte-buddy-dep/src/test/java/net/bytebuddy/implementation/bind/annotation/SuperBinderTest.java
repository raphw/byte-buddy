package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SuperBinderTest extends AbstractAnnotationBinderTest<Super> {

    @Mock
    private TypeDescription targetType;

    @Mock
    private TypeDescription.Generic genericTargetType;

    public SuperBinderTest() {
        super(Super.class);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(genericTargetType);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(annotation.strategy()).thenReturn(Super.Instantiation.CONSTRUCTOR);
        when(annotation.constructorParameters()).thenReturn(new Class<?>[0]);
        doReturn(Super.ConstructorResolver.Default.class).when(annotation).constructorResolver();
        when(targetType.asErasure()).thenReturn(targetType);
        when(targetType.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(
                new MethodDescription.ForLoadedConstructor(Object.class.getConstructor())));
    }

    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Super> getSimpleBinder() {
        return Super.Binder.INSTANCE;
    }

    @Test
    public void testAssignableBinding() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        when(stackManipulation.isValid()).thenReturn(true);
        when(instrumentedType.isAssignableTo(targetType)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Super.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testIllegalBindingForNonAssignableType() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Super.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test
    public void testIllegalBindingStaticMethod() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        when(source.isStatic()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Super.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testPrimitiveParameterType() throws Exception {
        when(genericTargetType.isPrimitive()).thenReturn(true);
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test(expected = IllegalStateException.class)
    public void testArrayParameterType() throws Exception {
        when(genericTargetType.isArray()).thenReturn(true);
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test(expected = IllegalStateException.class)
    public void testPrimitiveProxyType() throws Exception {
        doReturn(int.class).when(annotation).proxyType();
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test(expected = IllegalStateException.class)
    public void testArrayProxyType() throws Exception {
        doReturn(Object[].class).when(annotation).proxyType();
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignableType() throws Exception {
        doReturn(Void.class).when(annotation).proxyType();
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test(expected = IllegalStateException.class)
    public void testFinalProxyType() throws Exception {
        doReturn(void.class).when(annotation).proxyType();
        when(targetType.isFinal()).thenReturn(true);
        Super.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }
}
