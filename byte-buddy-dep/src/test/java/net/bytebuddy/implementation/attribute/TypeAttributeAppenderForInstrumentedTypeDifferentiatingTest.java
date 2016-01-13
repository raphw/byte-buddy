package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.mockito.Mockito.when;

public class TypeAttributeAppenderForInstrumentedTypeDifferentiatingTest extends AbstractTypeAttributeAppenderTest {
/*
    @Test
    public void testApplicationExcludedAnnotation() throws Exception {
        when(instrumentedType.getDeclaredAnnotations())
                .thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        new TypeAttributeAppender.ForInstrumentedType.Excluding(Collections.singleton(AnnotationDescription.ForLoadedAnnotation.of(new Baz.Instance())))
                .apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
    }

    @Test
    public void testApplicationNoExcludedAnnotation() throws Exception {
        when(instrumentedType.getDeclaredAnnotations())
                .thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        new TypeAttributeAppender.ForInstrumentedType.Excluding(Collections.<AnnotationDescription>emptySet())
                .apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
    }*/ // TODO: Complete

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.ForInstrumentedType.Differentiating.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                AnnotationDescription[] annotationDescription = new AnnotationDescription[new Random().nextInt(10000)];
                when(mock.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Arrays.asList(annotationDescription)));
                when(mock.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
                when(mock.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
            }
        }).apply();
    }
}
