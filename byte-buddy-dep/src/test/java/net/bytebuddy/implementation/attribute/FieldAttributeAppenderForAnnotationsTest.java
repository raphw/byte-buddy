package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class FieldAttributeAppenderForAnnotationsTest extends AbstractFieldAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new FieldAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Qux.Instance())).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyZeroInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new FieldAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Baz.Instance())).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new FieldAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance())).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testFactory() throws Exception {
        FieldAttributeAppender.Explicit fieldAttributeAppender = new FieldAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        assertThat(fieldAttributeAppender.make(instrumentedType), sameInstance((FieldAttributeAppender) fieldAttributeAppender));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAttributeAppender.Explicit.class).generate(new ObjectPropertyAssertion.Generator<Annotation>() {
            @Override
            public Class<? extends Annotation> generate() {
                return SimpleAnnotation.class;
            }
        }).refine(new ObjectPropertyAssertion.Refinement<SimpleAnnotation>() {
            @Override
            public void apply(SimpleAnnotation mock) {
                doReturn(SimpleAnnotation.class).when(mock).annotationType();
                when(mock.value()).thenReturn("annotation" + System.identityHashCode(mock));
            }
        }).apply();
    }

    public @interface SimpleAnnotation {

        String value();
    }
}
