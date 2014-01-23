package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MostSpecificTypeResolver;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ArgumentAnnotationBinderTest extends AbstractAnnotationBinderTest<Argument> {

    public ArgumentAnnotationBinderTest() {
        super(Argument.class);
    }

    @Test
    public void testAnnotationType() throws Exception {
        assertEquals(Argument.class, Argument.Binder.INSTANCE.getHandledType());
    }

    @Test
    public void testLegalBinding() throws Exception {
        final int sourceIndex = 2, targetIndex = 1;
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        when(annotation.value()).thenReturn(sourceIndex);
        when(source.getParameterTypes()).thenReturn(new Class<?>[]{null, null, Object.class});
        when(source.isStatic()).thenReturn(false);
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{null, Void.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[targetIndex + 1][0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = Argument.Binder.INSTANCE
                .bind(annotation, targetIndex, source, target, typeDescription, assigner);
        assertThat(identifiedBinding.isValid(), is(true));
        Object expectedToken = new MostSpecificTypeResolver.ParameterIndexToken(targetIndex);
        assertThat(identifiedBinding.getIdentificationToken(), equalTo(expectedToken));
        assertThat(identifiedBinding.getIdentificationToken().hashCode(), equalTo(expectedToken.hashCode()));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameterTypes();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(assigner).assign(Object.class, Void.class, false);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        final int sourceIndex = 0, targetIndex = 0;
        when(annotation.value()).thenReturn(sourceIndex);
        when(source.getParameterTypes()).thenReturn(new Class<?>[0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = Argument.Binder.INSTANCE
                .bind(annotation, targetIndex, source, target, typeDescription, assigner);
        assertThat(identifiedBinding.isValid(), is(false));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameterTypes();
        verifyZeroInteractions(assigner);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAnnotationValue() throws Exception {
        when(annotation.value()).thenReturn(-1);
        Argument.Binder.INSTANCE.bind(annotation, 0, source, target, typeDescription, assigner);
    }
}
