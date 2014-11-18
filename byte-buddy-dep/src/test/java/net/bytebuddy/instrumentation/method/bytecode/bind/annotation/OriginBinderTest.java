package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class OriginBinderTest extends AbstractAnnotationBinderTest<Origin> {

    private static final String FOO = "foo";

    private static final String METHOD_HANDLE_TYPE_INTERNAL_NAME = "java/lang/invoke/MethodHandle";

    private static final String METHOD_TYPE_TYPE_INTERNAL_NAME = "java/lang/invoke/MethodType";

    private static final int INDEX = 0;

    @Mock
    private TypeDescription targetType;

    public OriginBinderTest() {
        super(Origin.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(targetTypeList.get(INDEX)).thenReturn(targetType);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Origin> getSimpleBinder() {
        return Origin.Binder.INSTANCE;
    }

    @Test
    public void testClassBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(Class.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, INDEX, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(instrumentationTarget).getOriginType();
    }

    @Test
    public void testMethodBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(Method.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, INDEX, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testStringBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(String.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, INDEX, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testMethodHandleBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(METHOD_HANDLE_TYPE_INTERNAL_NAME);
        when(source.getDeclaringType()).thenReturn(mock(TypeDescription.class));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, INDEX, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testMethodTypeBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(METHOD_TYPE_TYPE_INTERNAL_NAME);
        when(source.getDescriptor()).thenReturn(FOO);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, INDEX, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        Origin.Binder.INSTANCE.bind(annotationDescription, INDEX, source, target, instrumentationTarget, assigner);
    }
}
