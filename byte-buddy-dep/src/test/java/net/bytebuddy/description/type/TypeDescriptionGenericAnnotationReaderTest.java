package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TypeDescriptionGenericAnnotationReaderTest {

    @Test
    public void testLegacyVmReturnsNoOpReaders() throws Exception {
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolve(null),
                is(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveSuperType(null),
                is(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveInterface(null, 0),
                is(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveReturnType(null),
                is(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveParameterType(null, 0),
                is(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveExceptionType(null, 0),
                is(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.INSTANCE.resolveTypeVariable(null),
                is(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
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

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.NoOp.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForComponentType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForTypeArgument.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForTypeVariableBoundType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForWildcardLowerBoundType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForWildcardUpperBoundType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForModernVm.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForModernVm.Resolved.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForModernVm.AnnotatedExceptionType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForModernVm.AnnotatedFieldType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForModernVm.AnnotatedInterfaceType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForModernVm.AnnotatedParameterizedType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForModernVm.AnnotatedReturnType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForModernVm.AnnotatedSuperType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForModernVm.AnnotatedTypeVariableType.class).apply();
    }
}