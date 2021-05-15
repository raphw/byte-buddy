package net.bytebuddy.description.type;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionGenericAnnotationReaderTest {

    @Test
    public void testNoOpReaderReturnsZeroAnnotations() throws Exception {
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.getDeclaredAnnotations().length, is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoOpReaderNoHierarchyAnnotations() throws Exception {
        TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.getAnnotations();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoOpReaderNoSpecificAnnotations() throws Exception {
        TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.getAnnotation(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoOpReaderNoSpecificAnnotationPresent() throws Exception {
        TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.isAnnotationPresent(null);
    }

    @Test
    public void testAnnotationReaderNoOpTest() throws Exception {
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofComponentType(),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofOuterClass(),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofOwnerType(),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofTypeArgument(0),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofTypeVariableBoundType(0),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofWildcardLowerBoundType(0),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofWildcardUpperBoundType(0),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
    }
}
