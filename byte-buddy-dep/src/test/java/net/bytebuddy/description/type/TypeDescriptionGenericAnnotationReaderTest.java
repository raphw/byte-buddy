package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.CompoundList;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

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

    @Test(expected = IllegalStateException.class)
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
                is((TypeDescription.Generic.AnnotationReader)TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofOuterClass(),
                is((TypeDescription.Generic.AnnotationReader)TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofOwnerType(),
                is((TypeDescription.Generic.AnnotationReader)TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofTypeArgument(0),
                is((TypeDescription.Generic.AnnotationReader)TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofTypeVariableBoundType(0),
                is((TypeDescription.Generic.AnnotationReader)TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofWildcardLowerBoundType(0),
                is((TypeDescription.Generic.AnnotationReader)TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
        assertThat(TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE.ofWildcardUpperBoundType(0),
                is((TypeDescription.Generic.AnnotationReader)TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.NoOp.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForComponentType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForOwnerType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForTypeArgument.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForTypeVariableBoundType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForWildcardLowerBoundType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.ForWildcardUpperBoundType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForLegacyVm.class).apply();
        final Iterator<Method> methods = CompoundList.of(Arrays.asList(Object.class.getDeclaredMethods()), Arrays.asList(String.class.getDeclaredMethods())).iterator();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForJava8CapableVm.class)
                .create(new ObjectPropertyAssertion.Creator<Method>() {
                    @Override
                    public Method create() {
                        return methods.next();
                    }
                }).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForJava8CapableVm.Resolved.class).apply();
        final Iterator<Field> fields = Arrays.asList(Thread.class.getDeclaredFields()).iterator();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForJava8CapableVm.AnnotatedFieldType.class)
                .create(new ObjectPropertyAssertion.Creator<Field>() {
                    @Override
                    public Field create() {
                        return fields.next();
                    }
                }).apply();
        final Iterator<Class<?>> types = Arrays.<Class<?>>asList(Object.class, String.class, Integer.class, Number.class).iterator();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForJava8CapableVm.AnnotatedInterfaceType.class)
                .create(new ObjectPropertyAssertion.Creator<Class<?>>() {
                    @Override
                    public Class<?> create() {
                        return types.next();
                    }
                }).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForJava8CapableVm.AnnotatedParameterizedType.class).apply();
        final Iterator<Method> methods2 = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForJava8CapableVm.AnnotatedReturnType.class)
                .create(new ObjectPropertyAssertion.Creator<Method>() {
                    @Override
                    public Method create() {
                        return methods2.next();
                    }
                }).apply();
        final Iterator<Class<?>> types2 = Arrays.<Class<?>>asList(Object.class, String.class, Integer.class, Number.class).iterator();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForJava8CapableVm.AnnotatedSuperClass.class)
                .create(new ObjectPropertyAssertion.Creator<Class<?>>() {
                    @Override
                    public Class<?> create() {
                        return types2.next();
                    }
                }).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForJava8CapableVm.AnnotatedTypeVariableType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.AnnotationReader.Dispatcher.ForJava8CapableVm.AnnotatedExceptionType.class).apply();
    }
}