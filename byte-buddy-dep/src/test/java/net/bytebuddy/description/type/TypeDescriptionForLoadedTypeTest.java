package net.bytebuddy.description.type;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionForLoadedTypeTest extends AbstractTypeDescriptionTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    protected TypeDescription describe(Class<?> type) {
        return TypeDescription.ForLoadedType.of(type);
    }

    protected TypeDescription.Generic describeType(Field field) {
        return TypeDescription.ForLoadedType.of(field.getDeclaringClass()).getDeclaredFields().filter(ElementMatchers.is(field)).getOnly().getType();
    }

    protected TypeDescription.Generic describeReturnType(Method method) {
        return TypeDescription.ForLoadedType.of(method.getDeclaringClass()).getDeclaredMethods().filter(ElementMatchers.is(method)).getOnly().getReturnType();
    }

    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return TypeDescription.ForLoadedType.of(method.getDeclaringClass()).getDeclaredMethods().filter(ElementMatchers.is(method)).getOnly().getParameters().get(index).getType();
    }

    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        return TypeDescription.ForLoadedType.of(method.getDeclaringClass()).getDeclaredMethods().filter(ElementMatchers.is(method)).getOnly().getExceptionTypes().get(index);
    }

    protected TypeDescription.Generic describeSuperClass(Class<?> type) {
        return TypeDescription.ForLoadedType.of(type).getSuperClass();
    }

    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return TypeDescription.ForLoadedType.of(type).getInterfaces().get(index);
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
    public void testTypeVariableU() throws Exception {
        super.testTypeVariableU();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
    public void testTypeVariableV() throws Exception {
        super.testTypeVariableV();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
    public void testTypeVariableW() throws Exception {
        super.testTypeVariableW();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(16)
    public void testTypeVariableX() throws Exception {
        super.testTypeVariableX();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testTypeAnnotationOwnerType() throws Exception {
        super.testTypeAnnotationOwnerType();
    }

    @Test
    @Override
    @Ignore("The OpenJDK reflection API does not currently support generic receiver types")
    public void testGenericTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericTypeAnnotationReceiverTypeOnMethod();
    }

    @Test
    @Override
    @Ignore("The OpenJDK reflection API does not currently support generic receiver types")
    public void testGenericNestedTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericNestedTypeAnnotationReceiverTypeOnMethod();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testGenericNestedTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        super.testGenericNestedTypeAnnotationReceiverTypeOnConstructor();
    }

    @Test
    @Override
    @Ignore("The OpenJDK reflection API does not currently support generic receiver types")
    public void testGenericInnerTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        super.testGenericInnerTypeAnnotationReceiverTypeOnConstructor();
    }

    @Test
    @Override
    @Ignore("The OpenJDK reflection API does not currently support generic receiver types")
    public void testGenericInnerTypeAnnotationReceiverTypeOnMethod() throws Exception {
        super.testGenericInnerTypeAnnotationReceiverTypeOnMethod();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testTypeAnnotationNonGenericInnerType() throws Exception {
        super.testTypeAnnotationNonGenericInnerType();
    }

    @Test
    public void testNameEqualityNonAnonymous() throws Exception {
        assertThat(TypeDescription.ForLoadedType.getName(Object.class), is(Object.class.getName()));
    }

    @Test
    public void testLazyResolution() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassFileLocator.ForClassLoader.readToNames(Foo.class));
        TypeDescription.ForLoadedType.of(classLoader.loadClass(Foo.class.getName()));
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
