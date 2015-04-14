package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JavaInstanceMethodTypeTest {

    private static final String BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfLoadedType() throws Exception {
        JavaInstance.MethodType methodType = JavaInstance.MethodType.of(void.class, Foo.class);
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Foo.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfMethod() throws Exception {
        JavaInstance.MethodType methodType = JavaInstance.MethodType.of(Foo.class.getDeclaredMethod(BAR, Void.class));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfStaticMethod() throws Exception {
        JavaInstance.MethodType methodType = JavaInstance.MethodType.of(Foo.class.getDeclaredMethod(QUX, Void.class));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfConstructor() throws Exception {
        JavaInstance.MethodType methodType = JavaInstance.MethodType.of(Foo.class.getDeclaredConstructor(Void.class));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfGetter() throws Exception {
        JavaInstance.MethodType methodType = JavaInstance.MethodType.ofGetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(Void.class)));
        assertThat(methodType.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    public void testMethodTypeOfStaticGetter() throws Exception {
        JavaInstance.MethodType methodType = JavaInstance.MethodType.ofGetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(Void.class)));
        assertThat(methodType.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfSetter() throws Exception {
        JavaInstance.MethodType methodType = JavaInstance.MethodType.ofSetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfStaticSetter() throws Exception {
        JavaInstance.MethodType methodType = JavaInstance.MethodType.ofSetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    public void testMethodTypeOfConstant() throws Exception {
        JavaInstance.MethodType methodType = JavaInstance.MethodType.ofConstant(new Foo(null));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodType.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeOfLoadedMethodType() throws Exception {
        Object loadedMethodType = JavaType.METHOD_TYPE.load().getDeclaredMethod("methodType", Class.class, Class[].class)
                .invoke(null, void.class, new Class<?>[]{Object.class});
        JavaInstance.MethodType methodType = JavaInstance.MethodType.of(loadedMethodType);
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Object.class)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(JavaInstance.MethodType.class).apply();
    }

    public static class Foo {

        static Void qux;

        Void bar;

        Foo(Void value) {
            /* empty*/
        }

        static void qux(Void value) {
            /* empty */
        }

        void bar(Void value) {
            /* empty */
        }
    }
}