package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class DynamicConstantBinderTest extends AbstractAnnotationBinderTest<DynamicConstant> {

    private static final String FOO = "foo", BAR = "bar";

    @Mock
    private TypeDescription targetType;

    @Mock
    private TypeDescription.Generic typeDescription;

    @Mock
    private TypeList.Generic interfaces;

    @Mock
    private TypeList rawInterfaces;

    public DynamicConstantBinderTest() {
        super(DynamicConstant.class);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(target.getType()).thenReturn(typeDescription);
        when(typeDescription.asErasure()).thenReturn(targetType);
        when(instrumentedType.getInterfaces()).thenReturn(interfaces);
        when(interfaces.asErasures()).thenReturn(rawInterfaces);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<DynamicConstant> getSimpleBinder() {
        return DynamicConstant.Binder.INSTANCE;
    }

    @Test
    public void testDynamicConstant() throws Exception {
        doReturn(void.class).when(annotation).bootstrapReturnType();
        doReturn(void.class).when(annotation).bootstrapOwner();
        doReturn(new Class<?>[] {Object.class}).when(annotation).bootstrapParameterTypes();
        when(annotation.invokedynamic()).thenReturn(false);
        when(annotation.name()).thenReturn(FOO);
        when(annotation.bootstrapName()).thenReturn(BAR);
        when(annotation.bootstrapType()).thenReturn(JavaConstant.MethodHandle.HandleType.INVOKE_STATIC);
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DynamicConstant.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testDynamicConstantInvokedynamic() throws Exception {
        doReturn(void.class).when(annotation).bootstrapReturnType();
        doReturn(void.class).when(annotation).bootstrapOwner();
        doReturn(new Class<?>[] {Object.class}).when(annotation).bootstrapParameterTypes();
        when(annotation.invokedynamic()).thenReturn(true);
        when(annotation.name()).thenReturn(FOO);
        when(annotation.bootstrapName()).thenReturn(BAR);
        when(annotation.bootstrapType()).thenReturn(JavaConstant.MethodHandle.HandleType.INVOKE_STATIC);
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = DynamicConstant.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }
}
