package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class AnnotationDrivenBinderNoArgumentsTest {

    private AnnotationDrivenBinder.AnnotationDefaultHandler<?> annotationDefaultHandler;
    private Assigner assigner;

    private MethodDelegationBinder methodDelegationBinder;

    private TypeDescription typeDescription;
    private MethodDescription source, target;

    @Before
    public void setUp() throws Exception {
        annotationDefaultHandler = mock(AnnotationDrivenBinder.AnnotationDefaultHandler.class);
        assigner = mock(Assigner.class);
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        methodDelegationBinder = new AnnotationDrivenBinder(
                Collections.<AnnotationDrivenBinder.ArgumentBinder<?>>emptyList(),
                annotationDefaultHandler,
                assigner);
        typeDescription = mock(TypeDescription.class);
        source = mock(MethodDescription.class, RETURNS_MOCKS);
        target = mock(MethodDescription.class, RETURNS_MOCKS);
    }

    @Test
    public void testEmptyMethodBinding() throws Exception {
        MethodDelegationBinder.BoundMethodDelegation boundMethodDelegation = methodDelegationBinder.bind(typeDescription, source, target);
        assertThat(boundMethodDelegation.isBound(), is(true));
        assertThat(boundMethodDelegation.getBindingTarget(), is(target));

        verify(assigner).assign(any(Class.class), any(Class.class), eq(false));
        verifyNoMoreInteractions(assigner);

        verify(annotationDefaultHandler).makeIterator(typeDescription, source, target);
        verifyNoMoreInteractions(annotationDefaultHandler);

        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).isAnnotationPresent(RuntimeType.class);
        verify(target, atLeast(1)).getParameterTypes();
    }
}
