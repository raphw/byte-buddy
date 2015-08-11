package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StubValueBinderTest extends AbstractAnnotationBinderTest<StubValue> {

    public StubValueBinderTest() {
        super(StubValue.class);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<StubValue> getSimpleBinder() {
        return StubValue.Binder.INSTANCE;
    }

    @Test
    public void testVoidReturnType() throws Exception {
        when(target.getType()).thenReturn(TypeDescription.OBJECT);
        when(source.getReturnType()).thenReturn(TypeDescription.VOID);
        assertThat(StubValue.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner).isValid(), is(true));
    }

    @Test
    public void testNonVoidAssignableReturnType() throws Exception {
        when(target.getType()).thenReturn(TypeDescription.OBJECT);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        when(source.getReturnType()).thenReturn(typeDescription);
        when(stackManipulation.isValid()).thenReturn(true);
        assertThat(StubValue.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner).isValid(), is(true));
    }

    @Test
    public void testNonVoidNonAssignableReturnType() throws Exception {
        when(target.getType()).thenReturn(TypeDescription.OBJECT);
        when(source.getReturnType()).thenReturn(TypeDescription.OBJECT);
        when(stackManipulation.isValid()).thenReturn(false);
        assertThat(StubValue.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner).isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalParameter() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        when(target.getType()).thenReturn(typeDescription);
        StubValue.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(StubValue.Binder.class).apply();
    }
}