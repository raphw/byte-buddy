package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForInstrumentedMethodExcludingTest extends AbstractTypeAttributeAppenderTest {

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
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.ForInstrumentedType.Excluding.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(mock(AnnotationDescription.class)));
            }
        }).apply();
    }
}
