package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class OriginBinderTest extends AbstractAnnotationBinderTest<Origin> {

    private static final String FOO = "foo";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private TypeDescription targetType;

    public OriginBinderTest() {
        super(Origin.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(target.getTypeDescription()).thenReturn(targetType);
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
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(instrumentationTarget).getOriginType();
    }

    @Test
    public void testMethodBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(Method.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testStringBinding() throws Exception {
        when(targetType.getInternalName()).thenReturn(FOO);
        when(targetType.represents(String.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleBinding() throws Exception {
        targetType = new TypeDescription.ForLoadedType(JavaType.METHOD_HANDLE.load());
        when(target.getTypeDescription()).thenReturn(targetType);
        when(source.getDeclaringType()).thenReturn(mock(TypeDescription.class));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeBinding() throws Exception {
        targetType = new TypeDescription.ForLoadedType(JavaType.METHOD_TYPE.load());
        when(target.getTypeDescription()).thenReturn(targetType);
        when(source.getDescriptor()).thenReturn(FOO);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotationDescription, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBinding() throws Exception {
        when(targetType.getName()).thenReturn(FOO);
        Origin.Binder.INSTANCE.bind(annotationDescription, source, target, instrumentationTarget, assigner);
    }
}
