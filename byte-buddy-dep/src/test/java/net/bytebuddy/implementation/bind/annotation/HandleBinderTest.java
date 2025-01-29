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

public class HandleBinderTest extends AbstractAnnotationBinderTest<Handle> {

    private static final String FOO = "foo";

    @Mock
    private TypeDescription targetType;

    @Mock
    private TypeDescription.Generic typeDescription;

    @Mock
    private TypeList.Generic interfaces;

    @Mock
    private TypeList rawInterfaces;

    public HandleBinderTest() {
        super(Handle.class);
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
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Handle> getSimpleBinder() {
        return Handle.Binder.INSTANCE;
    }

    @Test
    public void testAssignableBinding() throws Exception {
        doReturn(void.class).when(annotation).returnType();
        doReturn(void.class).when(annotation).owner();
        doReturn(new Class<?>[] {Object.class}).when(annotation).parameterTypes();
        when(annotation.name()).thenReturn(FOO);
        when(annotation.type()).thenReturn(JavaConstant.MethodHandle.HandleType.INVOKE_STATIC);
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(true);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Handle.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(true));
    }

    @Test
    public void testNonAssignableBinding() throws Exception {
        when(targetType.isAssignableFrom(JavaType.METHOD_HANDLE.getTypeStub())).thenReturn(false);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Handle.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
    }
}
