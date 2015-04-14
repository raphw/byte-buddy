package net.bytebuddy.utility;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaInstanceMethodHandleTest {

    private static final String BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfMethod() throws Exception {
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle.of(Foo.class.getDeclaredMethod(BAR, Void.class));
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.INVOKE_VIRTUAL));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfMethodSpecialInvocation() throws Exception {
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle.ofSpecial(Foo.class.getDeclaredMethod(BAR, Void.class), Foo.class);
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.INVOKE_SPECIAL));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfStaticMethod() throws Exception {
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle.of(Foo.class.getDeclaredMethod(QUX, Void.class));
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.INVOKE_STATIC));
        assertThat(methodHandle.getName(), is(QUX));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfConstructor() throws Exception {
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle.of(Foo.class.getDeclaredConstructor(Void.class));
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.INVOKE_SPECIAL_CONSTRUCTOR));
        assertThat(methodHandle.getName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfConstructorSpecialInvocation() throws Exception {
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle
                .of(new MethodDescription.ForLoadedConstructor(Foo.class.getDeclaredConstructor(Void.class)));
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.INVOKE_SPECIAL_CONSTRUCTOR));
        assertThat(methodHandle.getName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfGetter() throws Exception {
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle.ofGetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.GET_FIELD));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(Void.class)));
        assertThat(methodHandle.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    public void testMethodHandleOfStaticGetter() throws Exception {
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle.ofGetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.GET_STATIC_FIELD));
        assertThat(methodHandle.getName(), is(QUX));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(Void.class)));
        assertThat(methodHandle.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfSetter() throws Exception {
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle.ofSetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.PUT_FIELD));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfStaticSetter() throws Exception {
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle.ofSetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.PUT_STATIC_FIELD));
        assertThat(methodHandle.getName(), is(QUX));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleOfLoadedMethodHandle() throws Exception {
        Method publicLookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup");
        Object lookup = publicLookup.invoke(null);
        Method unreflect = Class.forName("java.lang.invoke.MethodHandles$Lookup").getDeclaredMethod("unreflect", Method.class);
        Object methodHandleLoaded = unreflect.invoke(lookup, Foo.class.getDeclaredMethod(BAR, Void.class));
        JavaInstance.MethodHandle methodHandle = JavaInstance.MethodHandle.of(methodHandleLoaded);
        assertThat(methodHandle.getHandleType(), is(JavaInstance.MethodHandle.HandleType.INVOKE_VIRTUAL));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedType(Void.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleLoadedIllegal() throws Exception {
        JavaInstance.MethodHandle.of(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleLoadedLookupIllegal() throws Exception {
        Method publicLookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup");
        Object lookup = publicLookup.invoke(null);
        Method unreflect = Class.forName("java.lang.invoke.MethodHandles$Lookup").getDeclaredMethod("unreflect", Method.class);
        Object methodHandleLoaded = unreflect.invoke(lookup, Foo.class.getDeclaredMethod(BAR, Void.class));
        JavaInstance.MethodHandle.of(methodHandleLoaded, new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalParameterThrowsException() throws Exception {
        JavaInstance.MethodHandle.HandleType.of(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStaticMethodNotSpecial() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(methodDescription.isStatic()).thenReturn(true);
        when(methodDescription.isSpecializableFor(typeDescription)).thenReturn(true);
        JavaInstance.MethodHandle.ofSpecial(methodDescription, typeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbstractMethodNotSpecial() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(methodDescription.isAbstract()).thenReturn(true);
        when(methodDescription.isSpecializableFor(typeDescription)).thenReturn(true);
        JavaInstance.MethodHandle.ofSpecial(methodDescription, typeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMethodNotSpecializable() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(methodDescription.isSpecializableFor(typeDescription)).thenReturn(false);
        JavaInstance.MethodHandle.ofSpecial(methodDescription, typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(JavaInstance.MethodHandle.class).apply();
        ObjectPropertyAssertion.of(JavaInstance.MethodHandle.HandleType.class).apply();
    }

    public static class Foo {

        public static Void qux;

        public Void bar;

        public Foo(Void value) {
            /* empty*/
        }

        public static void qux(Void value) {
            /* empty */
        }

        public void bar(Void value) {
            /* empty */
        }
    }
}
