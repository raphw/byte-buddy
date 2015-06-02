package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.asm.Type;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForSuperTypeTest extends AbstractTypeAttributeAppenderTest {

    @Mock
    private TypeDescription superType;

    @Test
    public void testSuperTypeAnnotationAppender() throws Exception {
        when(typeDescription.getSuperType()).thenReturn(superType);
        when(superType.getDeclaredAnnotations()).thenReturn(new AnnotationList
                .ForLoadedAnnotation(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        TypeAttributeAppender.ForSuperType.INSTANCE.apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verify(typeDescription, atLeast(1)).getSuperType();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.ForSuperType.class).apply();
    }
}
