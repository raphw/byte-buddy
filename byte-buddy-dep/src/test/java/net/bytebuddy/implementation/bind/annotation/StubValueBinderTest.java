package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class StubValueBinderTest extends AbstractAnnotationBinderTest<StubValue> {

    @Mock
    private TypeDescription type;

    @Mock
    private TypeDescription.Generic genericType;

    public StubValueBinderTest() {
        super(StubValue.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(genericType.asErasure()).thenReturn(type);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<StubValue> getSimpleBinder() {
        return StubValue.Binder.INSTANCE;
    }

    @Test
    public void testVoidReturnType() throws Exception {
        when(target.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        assertThat(StubValue.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner).isValid(), is(true));
    }

    @Test
    public void testNonVoidAssignableReturnType() throws Exception {
        when(target.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(source.getReturnType()).thenReturn(genericType);
        when(stackManipulation.isValid()).thenReturn(true);
        assertThat(StubValue.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner).isValid(), is(true));
    }

    @Test
    public void testNonVoidNonAssignableReturnType() throws Exception {
        when(target.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(source.getReturnType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(stackManipulation.isValid()).thenReturn(false);
        assertThat(StubValue.Binder.INSTANCE.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner).isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalParameter() throws Exception {
        when(target.getType()).thenReturn(genericType);
        StubValue.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(StubValue.Binder.class).apply();
    }
}
