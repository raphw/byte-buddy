package net.bytebuddy.description.type;

import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.ClassFileExtraction;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionForLoadedTypeTest extends AbstractTypeDescriptionTest {

    @Override
    protected TypeDescription describe(Class<?> type) {
        return new TypeDescription.ForLoadedType(type);
    }

    @Override
    protected TypeDescription.Generic describeType(Field field) {
        return new TypeDescription.ForLoadedType(field.getDeclaringClass()).getDeclaredFields().filter(ElementMatchers.is(field)).getOnly().getType();
    }

    @Override
    protected TypeDescription.Generic describeReturnType(Method method) {
        return new TypeDescription.ForLoadedType(method.getDeclaringClass()).getDeclaredMethods().filter(ElementMatchers.is(method)).getOnly().getReturnType();
    }

    @Override
    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return new TypeDescription.ForLoadedType(method.getDeclaringClass()).getDeclaredMethods().filter(ElementMatchers.is(method)).getOnly().getParameters().get(index).getType();
    }

    @Override
    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        return new TypeDescription.ForLoadedType(method.getDeclaringClass()).getDeclaredMethods().filter(ElementMatchers.is(method)).getOnly().getExceptionTypes().get(index);
    }

    @Override
    protected TypeDescription.Generic describeSuperClass(Class<?> type) {
        return new TypeDescription.ForLoadedType(type).getSuperClass();
    }

    @Override
    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return new TypeDescription.ForLoadedType(type).getInterfaces().get(index);
    }

    @Override
    @Test
    @Ignore("The Java reflection API suffers a bug that affects parsing of type variable bounds")
    public void testTypeVariableU() throws Exception {
        super.testTypeVariableU();
    }

    @Override
    @Test
    @Ignore("The Java reflection API suffers a bug that affects parsing of type variable bounds")
    public void testTypeVariableV() throws Exception {
        super.testTypeVariableV();
    }

    @Override
    @Test
    @Ignore("The Java reflection API suffers a bug that affects parsing of type variable bounds")
    public void testTypeVariableW() throws Exception {
        super.testTypeVariableW();
    }

    @Override
    @Test
    @Ignore("The Java reflection API suffers a bug that affects parsing of type variable bounds")
    public void testTypeVariableX() throws Exception {
        super.testTypeVariableX();
    }

    @Override
    @Test
    @Ignore("The Java reflection API does not currently support owner types")
    public void testTypeAnnotationOwnerType() throws Exception {
        super.testTypeAnnotationOwnerType();
    }

    @Override
    @Test
    @Ignore("The Java reflection API does not currently support generic receiver types")
    public void testGenericTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericTypeAnnotationReceiverTypeOnMethod();
    }

    @Override
    @Test
    @Ignore("The Java reflection API does not currently support generic receiver types")
    public void testGenericNestedTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericNestedTypeAnnotationReceiverTypeOnMethod();
    }

    @Override
    @Test
    @Ignore("The Java reflection API does not currently support generic receiver types")
    public void testGenericNestedTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        super.testGenericNestedTypeAnnotationReceiverTypeOnConstructor();
    }

    @Override
    @Test
    @Ignore("The Java reflection API does not currently support generic receiver types")
    public void testGenericInnerTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        super.testGenericInnerTypeAnnotationReceiverTypeOnConstructor();
    }

    @Override
    @Test
    @Ignore("The Java reflection API does not currently support generic receiver types")
    public void testGenericInnerTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericInnerTypeAnnotationReceiverTypeOnMethod();
    }

    @Override
    @Test
    @Ignore("The Java reflection API does not currently support nested non-generic types")
    public void testTypeAnnotationNonGenericInnerType() throws Exception {
        super.testTypeAnnotationNonGenericInnerType();
    }

    @Test
    public void testNameEqualityNonAnonymous() throws Exception {
        assertThat(TypeDescription.ForLoadedType.getName(Object.class), is(Object.class.getName()));
    }

    @Test
    public void testLazyResolution() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(Foo.class),
                null,
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        new TypeDescription.ForLoadedType(classLoader.loadClass(Foo.class.getName()));
    }

    public static class Foo {

        public Bar bar() {
            return new Bar();
        }
    }

    public static class Bar {
        /* empty */
    }
}
