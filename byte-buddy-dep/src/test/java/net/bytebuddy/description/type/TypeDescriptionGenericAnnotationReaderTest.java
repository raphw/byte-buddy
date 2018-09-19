package net.bytebuddy.description.type;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class TypeDescriptionGenericAnnotationReaderTest {

    @Test
    public void testLegacyVmReturnsNoOpReaders() throws Exception {
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveFieldType(null),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveSuperClassType(null),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveInterfaceType(null, 0),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveReturnType(null),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveParameterType(null, 0),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveExceptionType(null, 0),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveTypeVariable(null),
                is((TypeDescription.Generic.AnnotationReader) TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveReceiverType(null),
                nullValue(TypeDescription.Generic.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCannotResolveAnnotatedType() throws Exception {
        TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolve(null);
    }

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
