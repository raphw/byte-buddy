package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class OriginBinderTest extends AbstractAnnotationBinderTest<Origin> {

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

    @Test
    public void testAnnotationType() throws Exception {
        assertEquals(Origin.class, Origin.Binder.INSTANCE.getHandledType());
    }

    @Test
    public void testClassBinding() throws Exception {
        when(targetType.represents(Class.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotation, INDEX, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testMethodBinding() throws Exception {
        when(targetType.represents(Method.class)).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Origin.Binder.INSTANCE
                .bind(annotation, INDEX, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBinding() throws Exception {
        Origin.Binder.INSTANCE.bind(annotation, INDEX, source, target, instrumentationTarget, assigner);
    }
}
