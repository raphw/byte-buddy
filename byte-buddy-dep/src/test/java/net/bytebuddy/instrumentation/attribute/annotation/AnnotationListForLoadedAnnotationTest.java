package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;

import static org.mockito.Mockito.doReturn;

public class AnnotationListForLoadedAnnotationTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationList.ForLoadedAnnotation.class).generate(new ObjectPropertyAssertion.Generator<Annotation>() {
            @Override
            public Class<? extends Annotation> generate() {
                return Foo.class;
            }
        }).refine(new ObjectPropertyAssertion.Refinement<Foo>() {
            @Override
            public void apply(Foo mock) {
                doReturn(Foo.class).when(mock).annotationType();
            }
        }).apply();
    }

    private static @interface Foo {

    }

    @Inherited
    private static @interface Bar {

    }
}
