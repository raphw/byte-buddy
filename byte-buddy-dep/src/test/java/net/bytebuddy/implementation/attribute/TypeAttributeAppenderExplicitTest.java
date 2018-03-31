package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeList;
import org.junit.Test;
import org.objectweb.asm.Type;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderExplicitTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testAnnotationNoRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        new TypeAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Qux.Instance())).apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testAnnotationByteCodeRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        new TypeAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Baz.Instance())).apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(instrumentedType);

    }

    @Test
    public void testAnnotationClassFileRetention() throws Exception {
        when(instrumentedType.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()));
        new TypeAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance())).apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(instrumentedType);
    }

    public @interface SimpleAnnotation {

        String value();
    }
}
