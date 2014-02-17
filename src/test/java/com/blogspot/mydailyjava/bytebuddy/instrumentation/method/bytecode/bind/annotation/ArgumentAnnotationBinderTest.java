package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
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
    public void testLegalBindingNoRuntimeType() throws Exception {
        testLegalBinding(new Annotation[2][0], false);
    }

    @Test
    public void testLegalBindingRuntimeType() throws Exception {
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        testLegalBinding(new Annotation[][] {{}, {runtimeType}}, true);
    }

    private void testLegalBinding(Annotation[][] annotations, boolean considerRuntimeType) throws Exception {
        final int sourceIndex = 2, targetIndex = 1;
        when(stackManipulation.isValid()).thenReturn(true);
        when(annotation.value()).thenReturn(sourceIndex);
        TypeList sourceParameters = mock(TypeList.class);
        when(sourceParameters.size()).thenReturn(sourceIndex + 1);
        TypeDescription sourceType = mock(TypeDescription.class);
        when(sourceParameters.get(sourceIndex)).thenReturn(sourceType);
        when(source.getParameterTypes()).thenReturn(sourceParameters);
        when(source.isStatic()).thenReturn(false);
        TypeList targetParameters = mock(TypeList.class);
        when(targetParameters.size()).thenReturn(targetIndex + 1);
        TypeDescription targetType = mock(TypeDescription.class);
        when(targetParameters.get(targetIndex)).thenReturn(targetType);
        when(target.getParameterTypes()).thenReturn(targetParameters);
        when(target.getParameterAnnotations()).thenReturn(annotations);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = Argument.Binder.INSTANCE
                .bind(annotation, targetIndex, source, target, instrumentedType, assigner);
        assertThat(identifiedBinding.isValid(), is(true));
        Object expectedToken = new MostSpecificTypeResolver.ParameterIndexToken(sourceIndex);
        assertThat(identifiedBinding.getIdentificationToken(), equalTo(expectedToken));
        assertThat(identifiedBinding.getIdentificationToken().hashCode(), equalTo(expectedToken.hashCode()));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameterTypes();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(assigner).assign(sourceType, targetType, considerRuntimeType);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        final int sourceIndex = 0, targetIndex = 0;
        when(annotation.value()).thenReturn(sourceIndex);
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(0);
        when(source.getParameterTypes()).thenReturn(typeList);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = Argument.Binder.INSTANCE
                .bind(annotation, targetIndex, source, target, instrumentedType, assigner);
        assertThat(identifiedBinding.isValid(), is(false));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameterTypes();
        verifyZeroInteractions(assigner);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAnnotationValue() throws Exception {
        when(annotation.value()).thenReturn(-1);
        Argument.Binder.INSTANCE.bind(annotation, 0, source, target, instrumentedType, assigner);
    }
}
