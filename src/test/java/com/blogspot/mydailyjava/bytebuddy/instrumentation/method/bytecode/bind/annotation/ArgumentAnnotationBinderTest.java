package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.LegalTrivialAssignment;
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
        final int sourceIndex = 2, targetIndex = 1;
        when(assigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean()))
                .thenReturn(LegalTrivialAssignment.INSTANCE);
        when(annotation.value()).thenReturn(sourceIndex);
        TypeList sourceParameters = makeTypeList(null, null, Object.class);
        when(source.getParameterTypes()).thenReturn(sourceParameters);
        when(source.isStatic()).thenReturn(false);
        TypeList targetParameters = makeTypeList(null, Void.class);
        when(target.getParameterTypes()).thenReturn(targetParameters);
        when(target.getParameterAnnotations()).thenReturn(new Annotation[targetIndex + 1][0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = Argument.Binder.INSTANCE
                .bind(annotation, targetIndex, source, target, typeDescription, assigner);
        assertThat(identifiedBinding.isValid(), is(true));
        Object expectedToken = new MostSpecificTypeResolver.ParameterIndexToken(sourceIndex);
        assertThat(identifiedBinding.getIdentificationToken(), equalTo(expectedToken));
        assertThat(identifiedBinding.getIdentificationToken().hashCode(), equalTo(expectedToken.hashCode()));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameterTypes();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(assigner).assign(sourceParameters.get(sourceIndex), targetParameters.get(targetIndex), false);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testLegalBindingRuntimeType() throws Exception {
        final int sourceIndex = 2, targetIndex = 1;
        when(assigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean()))
                .thenReturn(LegalTrivialAssignment.INSTANCE);
        when(annotation.value()).thenReturn(sourceIndex);
        TypeList sourceParameters = makeTypeList(null, null, Object.class);
        when(source.getParameterTypes()).thenReturn(sourceParameters);
        when(source.isStatic()).thenReturn(false);
        TypeList targetParameters = makeTypeList(null, Void.class);
        when(target.getParameterTypes()).thenReturn(targetParameters);
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][]{{}, {runtimeType}});
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = Argument.Binder.INSTANCE
                .bind(annotation, targetIndex, source, target, typeDescription, assigner);
        assertThat(identifiedBinding.isValid(), is(true));
        Object expectedToken = new MostSpecificTypeResolver.ParameterIndexToken(sourceIndex);
        assertThat(identifiedBinding.getIdentificationToken(), equalTo(expectedToken));
        assertThat(identifiedBinding.getIdentificationToken().hashCode(), equalTo(expectedToken.hashCode()));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameterTypes();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(assigner).assign(sourceParameters.get(sourceIndex), targetParameters.get(targetIndex), true);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        final int sourceIndex = 0, targetIndex = 0;
        when(annotation.value()).thenReturn(sourceIndex);
        when(source.getParameterTypes()).thenReturn(makeTypeList());
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

    private static TypeList makeTypeList(Class<?>... type) {
        return new TypeList.ForLoadedType(type);
    }
}
