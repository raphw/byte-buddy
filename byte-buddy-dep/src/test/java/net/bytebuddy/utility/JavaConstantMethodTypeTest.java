package net.bytebuddy.utility;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JavaConstantMethodTypeTest {

    private static final String BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfLoadedType() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(void.class, Foo.class);
        assertThat(methodType.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Foo.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfMethod() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(Foo.class.getDeclaredMethod(BAR, Void.class));
        assertThat(methodType.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
        assertThat(methodType.getDescriptor(), is(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(BAR, Void.class)).getDescriptor()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfStaticMethod() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(Foo.class.getDeclaredMethod(QUX, Void.class));
        assertThat(methodType.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfConstructor() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(Foo.class.getDeclaredConstructor(Void.class));
        assertThat(methodType.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfGetter() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofGetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(Void.class)));
        assertThat(methodType.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    public void testMethodTypeOfStaticGetter() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofGetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(Void.class)));
        assertThat(methodType.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfSetter() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofSetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodType.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodTypeOfStaticSetter() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofSetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodType.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    public void testMethodTypeOfConstant() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofConstant(new Foo(null));
        assertThat(methodType.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodType.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeOfLoadedMethodType() throws Exception {
        Object loadedMethodType = JavaType.METHOD_TYPE.load().getDeclaredMethod("methodType", Class.class, Class[].class)
                .invoke(null, void.class, new Class<?>[]{Object.class});
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofLoaded(loadedMethodType);
        assertThat(methodType.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Object.class)));
    }

    @SuppressWarnings("unused")
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