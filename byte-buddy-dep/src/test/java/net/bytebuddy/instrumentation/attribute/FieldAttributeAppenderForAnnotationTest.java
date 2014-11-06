package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.mockito.Mockito.*;

public class FieldAttributeAppenderForAnnotationTest extends AbstractFieldAttributeAppenderTest {

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new FieldAttributeAppender.ForAnnotation(new Qux.Instance()).apply(fieldVisitor, fieldDescription);
        verifyZeroInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new FieldAttributeAppender.ForAnnotation(new Baz.Instance()).apply(fieldVisitor, fieldDescription);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new FieldAttributeAppender.ForAnnotation(new QuxBaz.Instance()).apply(fieldVisitor, fieldDescription);
        verify(fieldVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAttributeAppender.ForAnnotation.class).generate(new ObjectPropertyAssertion.Generator<Annotation>() {
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

    public static @interface SimpleAnnotation {

        String value();
    }
}
