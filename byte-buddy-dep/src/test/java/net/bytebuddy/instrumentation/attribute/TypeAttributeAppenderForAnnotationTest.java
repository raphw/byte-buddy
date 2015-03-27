package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForAnnotationTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new TypeAttributeAppender.ForAnnotation(new Qux.Instance()).apply(classVisitor, typeDescription);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new TypeAttributeAppender.ForAnnotation(new Baz.Instance()).apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new TypeAttributeAppender.ForAnnotation(new QuxBaz.Instance()).apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.ForAnnotation.class).generate(new ObjectPropertyAssertion.Generator<Annotation>() {
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
