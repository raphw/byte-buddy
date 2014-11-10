package net.bytebuddy.instrumentation.field;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldDescriptionForLoadedFieldTest extends AbstractFieldDescriptionTest {

    @Override
    protected FieldDescription describe(Field field) {
        return new FieldDescription.ForLoadedField(field);
    }

    @Test
    public void testAnnotations() throws Exception {
        assertThat(describe(Sample.class.getDeclaredField("annotated")).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotation(Sample.class
                        .getDeclaredField("annotated").getDeclaredAnnotations())));
        assertThat(describe(Sample.class.getDeclaredField("nonAnnotated")).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.Empty()));
    }

    private static class Sample {

        @SampleAnnotation
        private Void annotated;

        private Void nonAnnotated;
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SampleAnnotation {
    }
}
