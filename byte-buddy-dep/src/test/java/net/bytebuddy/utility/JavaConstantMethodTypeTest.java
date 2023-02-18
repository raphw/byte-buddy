package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JavaConstantMethodTypeTest {

    private static final String BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testMethodTypeOfLoadedType() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(void.class, Foo.class);
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Foo.class)));
    }

    @Test
    public void testMethodTypeOfMethod() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(Foo.class.getDeclaredMethod(BAR, Void.class));
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Foo.class, Void.class)));
        assertThat(methodType.getDescriptor(), is(Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Foo.class), Type.getType(Void.class))));
    }

    @Test
    public void testMethodTypeOfMethodSignature() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofSignature(Foo.class.getDeclaredMethod(BAR, Void.class));
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
        assertThat(methodType.getDescriptor(), is(Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Void.class))));
    }

    @Test
    public void testMethodTypeOfStaticMethod() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(Foo.class.getDeclaredMethod(QUX, Void.class));
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    public void testMethodTypeOfStaticMethodSignature() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofSignature(Foo.class.getDeclaredMethod(QUX, Void.class));
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    public void testMethodTypeOfConstructor() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.of(Foo.class.getDeclaredConstructor(Void.class));
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(Foo.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    public void testMethodTypeOfConstructorSignature() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofSignature(Foo.class.getDeclaredConstructor(Void.class));
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("cast")
    public void testMethodTypeOfGetter() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofGetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodType.getReturnType(), is((TypeDescription) TypeDescription.ForLoadedType.of(Void.class)));
        assertThat(methodType.getParameterTypes(), is(Collections.singletonList(TypeDescription.ForLoadedType.of(Foo.class))));
    }

    @Test
    @SuppressWarnings("cast")
    public void testMethodTypeOfStaticGetter() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofGetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodType.getReturnType(), is((TypeDescription) TypeDescription.ForLoadedType.of(Void.class)));
        assertThat(methodType.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    public void testMethodTypeOfSetter() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofSetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Foo.class, Void.class)));
    }

    @Test
    public void testMethodTypeOfStaticSetter() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofSetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("cast")
    public void testMethodTypeOfConstant() throws Exception {
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofConstant(new Foo(null));
        assertThat(methodType.getReturnType(), is((TypeDescription) TypeDescription.ForLoadedType.of(Foo.class)));
        assertThat(methodType.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeOfLoadedMethodType() throws Exception {
        Object loadedMethodType = JavaType.METHOD_TYPE.load().getDeclaredMethod("methodType", Class.class, Class[].class)
                .invoke(null, void.class, new Class<?>[]{Object.class});
        JavaConstant.MethodType methodType = JavaConstant.MethodType.ofLoaded(loadedMethodType);
        assertThat(methodType.getReturnType(), is(TypeDescription.ForLoadedType.of(void.class)));
        assertThat(methodType.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Object.class)));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(JavaConstant.MethodType.of(Foo.class.getDeclaredMethod(QUX, Void.class)).hashCode(),
                is(JavaConstant.MethodType.of(Foo.class.getDeclaredMethod(QUX, Void.class)).hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        assertThat(JavaConstant.MethodType.of(Foo.class.getDeclaredMethod(QUX, Void.class)),
                is(JavaConstant.MethodType.of(Foo.class.getDeclaredMethod(QUX, Void.class))));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(JavaConstant.MethodType.of(Foo.class.getDeclaredMethod(QUX, Void.class)).toString(),
                is("(Void)void"));
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