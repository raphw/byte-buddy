package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import org.junit.Test;
import org.objectweb.asm.Type;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class RecordComponentAttributeAppenderForAnnotationsTest extends AbstractRecordComponentAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new RecordComponentAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Qux.Instance())).apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verifyNoMoreInteractions(recordComponentVisitor);
        verifyNoMoreInteractions(recordComponentDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new RecordComponentAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Baz.Instance())).apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verify(recordComponentVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(recordComponentVisitor);
        verifyNoMoreInteractions(recordComponentDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new RecordComponentAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance())).apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verify(recordComponentVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(recordComponentVisitor);
        verifyNoMoreInteractions(recordComponentDescription);
    }

    @Test
    public void testFactory() throws Exception {
        RecordComponentAttributeAppender.Explicit recordComponentAttributeAppender = new RecordComponentAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        assertThat(recordComponentAttributeAppender.make(instrumentedType), sameInstance((RecordComponentAttributeAppender) recordComponentAttributeAppender));
    }

    public @interface SimpleAnnotation {

        String value();
    }
}
