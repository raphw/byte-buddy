package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipeBinderTest extends AbstractAnnotationBinderTest<Pipe> {

    private TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> binder;

    @Mock
    private MethodDescription targetMethod;

    @Mock
    private TypeDescription targetMethodType;

    @Mock
    private TypeDescription.Generic genericTargetMethodType;

    public PipeBinderTest() {
        super(Pipe.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(targetMethod.getDeclaringType()).thenReturn(targetMethodType);
        when(genericTargetMethodType.asErasure()).thenReturn(targetMethodType);
        binder = new Pipe.Binder(targetMethod);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> getSimpleBinder() {
        return binder;
    }

    @Test
    public void testParameterBinding() throws Exception {
        when(target.getType()).thenReturn(genericTargetMethodType);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = binder.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testCannotPipeStaticMethod() throws Exception {
        when(target.getType()).thenReturn(genericTargetMethodType);
        when(source.isStatic()).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = binder.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterBindingOnIllegalTargetTypeThrowsException() throws Exception {
        TypeDescription.Generic targetType = mock(TypeDescription.Generic.class);
        TypeDescription rawTargetType = mock(TypeDescription.class);
        when(targetType.asErasure()).thenReturn(rawTargetType);
        when(target.getType()).thenReturn(targetType);
        binder.bind(annotationDescription,
                source,
                target,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
    }
}
