package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderExplicitTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new TypeAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()))
                .apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new TypeAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()))
                .apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new TypeAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()))
                .apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.Explicit.class).generate(new ObjectPropertyAssertion.Generator<Annotation>() {
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
