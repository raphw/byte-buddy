package net.bytebuddy.description.annotation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.annotation.Annotation;

public class AnnotationDescriptionBuilderTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Test(expected = IllegalArgumentException.class)
    public void testNonMatchingEnumerationValue() throws Exception {
        AnnotationDescription.Builder.ofType(Foo.class).define(FOO, FooBar.FIRST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonMatchingAnnotationValue() throws Exception {
        AnnotationDescription.Builder.ofType(Qux.class).define(FOO, new QuxBaz.Instance());
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testNonMatchingEnumerationArrayValue() throws Exception {
        AnnotationDescription.Builder.ofType(Foo.class).defineEnumerationArray(BAR, (Class) Bar.class, Bar.FIRST, FooBar.SECOND);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testNonMatchingAnnotationArrayValue() throws Exception {
        AnnotationDescription.Builder.ofType(Foo.class).defineAnnotationArray(BAZ, (Class) Qux.class, new Qux.Instance(), new QuxBaz.Instance());
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testNonAnnotationType() throws Exception {
        AnnotationDescription.Builder.ofType((Class) Object.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompleteAnnotation() throws Exception {
        AnnotationDescription.Builder.ofType(Foo.class).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownProperty() throws Exception {
        AnnotationDescription.Builder.ofType(Foo.class).define(FOO + BAR, FOO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalProperty() throws Exception {
        AnnotationDescription.Builder.ofType(Foo.class).define(FOO, FOO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateProperty() throws Exception {
        AnnotationDescription.Builder.ofType(Foo.class).define(BAZ, FOO).define(BAZ, FOO);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationDescription.Builder.class).apply();
    }

    public enum Bar {
        FIRST,
        SECOND
    }

    public enum FooBar {
        FIRST,
        SECOND
    }

    public @interface Foo {

        Bar foo();

        Bar[] bar();

        Qux qux();

        String baz();
    }

    public @interface Qux {

        class Instance implements Qux {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Qux.class;
            }
        }
    }

    public @interface QuxBaz {

        class Instance implements QuxBaz {

            @Override
            public Class<? extends Annotation> annotationType() {
                return QuxBaz.class;
            }
        }
    }
}
