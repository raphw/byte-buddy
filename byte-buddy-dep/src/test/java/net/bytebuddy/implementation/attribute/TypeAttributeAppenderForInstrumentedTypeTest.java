package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.asm.Type;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForInstrumentedTypeTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testApplication() throws Exception {
        when(instrumentedType.getDeclaredAnnotations()).thenReturn(new AnnotationList
                .ForLoadedAnnotation(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        TypeAttributeAppender.ForInstrumentedType.INSTANCE.apply(classVisitor, instrumentedType, valueFilter);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verify(instrumentedType).getDeclaredAnnotations();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.ForInstrumentedType.class).apply();
    }
}
