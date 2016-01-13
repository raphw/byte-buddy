package net.bytebuddy.implementation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.TypeReference;

import java.util.Random;

import static org.mockito.Mockito.when;

public class AnnotationAppenderForTypeAnnotationsTest {

    // TODO

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationAppender.ForTypeAnnotations.class).refine(new ObjectPropertyAssertion.Refinement<TypeReference>() {
            @Override
            public void apply(TypeReference mock) {
                when(mock.getValue()).thenReturn(new Random().nextInt());
            }
        }).apply();
    }
}