package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForSuperTypeTest extends AbstractTypeAttributeAppenderTest {

    @Mock
    private TypeDescription superType;

    @Test
    public void testSuperTypeAnnotationAppender() throws Exception {
        when(typeDescription.getSupertype()).thenReturn(superType);
        when(superType.getAnnotations()).thenReturn(new Annotation[]{ new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance() });
        TypeAttributeAppender.ForSuperType.INSTANCE.apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verify(typeDescription, atLeast(1)).getSupertype();
    }
}
