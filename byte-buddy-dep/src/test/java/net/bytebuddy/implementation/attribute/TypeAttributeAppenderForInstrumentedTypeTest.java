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

    @Mock
    private TypeDescription rawSuperType;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(typeDescription.getSuperType()).thenReturn(targetType);
        when(targetType.asErasure()).thenReturn(rawSuperType);
    }

    @Test
    public void testSuperTypeAnnotationAppender() throws Exception {
        when(typeDescription.getSuperType()).thenReturn(targetType);
        when(rawSuperType.getDeclaredAnnotations()).thenReturn(new AnnotationList
                .ForLoadedAnnotation(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        new TypeAttributeAppender.ForInstrumentedType(valueFilter).apply(classVisitor, typeDescription, targetType);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verify(typeDescription).getSuperType();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testDoesNotApplyForTargetTypeBeingInstrumentedType() throws Exception {
        when(typeDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList
                .ForLoadedAnnotation(new Qux.Instance(), new Baz.Instance(), new QuxBaz.Instance()));
        new TypeAttributeAppender.ForInstrumentedType(valueFilter).apply(classVisitor, typeDescription, typeDescription);
        verify(typeDescription).getSuperType();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.ForInstrumentedType.class).apply();
    }
}
