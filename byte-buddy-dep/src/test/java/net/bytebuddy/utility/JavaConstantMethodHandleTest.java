package net.bytebuddy.utility;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaConstantMethodHandleTest {

    private static final String BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfMethod() throws Exception {
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle.of(Foo.class.getDeclaredMethod(BAR, Void.class));
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.INVOKE_VIRTUAL));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
        assertThat(methodHandle.getDescriptor(), is(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(BAR, Void.class)).getDescriptor()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfMethodSpecialInvocation() throws Exception {
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle.ofSpecial(Foo.class.getDeclaredMethod(BAR, Void.class), Foo.class);
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.INVOKE_SPECIAL));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfStaticMethod() throws Exception {
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle.of(Foo.class.getDeclaredMethod(QUX, Void.class));
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.INVOKE_STATIC));
        assertThat(methodHandle.getName(), is(QUX));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfConstructor() throws Exception {
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle.of(Foo.class.getDeclaredConstructor(Void.class));
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.INVOKE_SPECIAL_CONSTRUCTOR));
        assertThat(methodHandle.getName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfConstructorSpecialInvocation() throws Exception {
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle
                .of(new MethodDescription.ForLoadedConstructor(Foo.class.getDeclaredConstructor(Void.class)));
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.INVOKE_SPECIAL_CONSTRUCTOR));
        assertThat(methodHandle.getName(), is(MethodDescription.CONSTRUCTOR_INTERNAL_NAME));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfGetter() throws Exception {
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle.ofGetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.GET_FIELD));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDefinition) new TypeDescription.ForLoadedType(Void.class)));
        assertThat(methodHandle.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    public void testMethodHandleOfStaticGetter() throws Exception {
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle.ofGetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.GET_STATIC_FIELD));
        assertThat(methodHandle.getName(), is(QUX));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is((TypeDefinition) new TypeDescription.ForLoadedType(Void.class)));
        assertThat(methodHandle.getParameterTypes(), is(Collections.<TypeDescription>emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfSetter() throws Exception {
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle.ofSetter(Foo.class.getDeclaredField(BAR));
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.PUT_FIELD));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodHandleOfStaticSetter() throws Exception {
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle.ofSetter(Foo.class.getDeclaredField(QUX));
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.PUT_STATIC_FIELD));
        assertThat(methodHandle.getName(), is(QUX));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testMethodHandleOfLoadedMethodHandle() throws Exception {
        Method publicLookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup");
        Object lookup = publicLookup.invoke(null);
        Method unreflected = Class.forName("java.lang.invoke.MethodHandles$Lookup").getDeclaredMethod("unreflect", Method.class);
        Object methodHandleLoaded = unreflected.invoke(lookup, Foo.class.getDeclaredMethod(BAR, Void.class));
        JavaConstant.MethodHandle methodHandle = JavaConstant.MethodHandle.ofLoaded(methodHandleLoaded);
        assertThat(methodHandle.getHandleType(), is(JavaConstant.MethodHandle.HandleType.INVOKE_VIRTUAL));
        assertThat(methodHandle.getName(), is(BAR));
        assertThat(methodHandle.getOwnerType(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodHandle.getReturnType(), is(TypeDescription.VOID));
        assertThat(methodHandle.getParameterTypes(), is((List<TypeDescription>) new TypeList.ForLoadedTypes(Void.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testMethodHandleLoadedIllegal() throws Exception {
        JavaConstant.MethodHandle.ofLoaded(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testMethodHandleLoadedLookupIllegal() throws Exception {
        Method publicLookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup");
        Object lookup = publicLookup.invoke(null);
        Method unreflect = Class.forName("java.lang.invoke.MethodHandles$Lookup").getDeclaredMethod("unreflect", Method.class);
        Object methodHandleLoaded = unreflect.invoke(lookup, Foo.class.getDeclaredMethod(BAR, Void.class));
        JavaConstant.MethodHandle.ofLoaded(methodHandleLoaded, new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalParameterThrowsException() throws Exception {
        JavaConstant.MethodHandle.HandleType.of(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStaticMethodNotSpecial() throws Exception {
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(methodDescription.isStatic()).thenReturn(true);
        when(methodDescription.isSpecializableFor(typeDescription)).thenReturn(true);
        JavaConstant.MethodHandle.ofSpecial(methodDescription, typeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbstractMethodNotSpecial() throws Exception {
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(methodDescription.isAbstract()).thenReturn(true);
        when(methodDescription.isSpecializableFor(typeDescription)).thenReturn(true);
        JavaConstant.MethodHandle.ofSpecial(methodDescription, typeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMethodNotSpecializable() throws Exception {
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(methodDescription.isSpecializableFor(typeDescription)).thenReturn(false);
        JavaConstant.MethodHandle.ofSpecial(methodDescription, typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(JavaConstant.MethodHandle.class).apply();
        ObjectPropertyAssertion.of(JavaConstant.MethodHandle.Dispatcher.CreationAction.class).apply();
        final Iterator<Method> methods = Arrays.asList(String.class.getDeclaredMethods()).iterator();
        final Iterator<Constructor<?>> constructors= Arrays.asList(String.class.getDeclaredConstructors()).iterator();
        ObjectPropertyAssertion.of(JavaConstant.MethodHandle.Dispatcher.ForJava7CapableVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).create(new ObjectPropertyAssertion.Creator<Constructor<?>>() {
            @Override
            public Constructor<?> create() {
                return constructors.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(JavaConstant.MethodHandle.Dispatcher.ForJava8CapableVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).create(new ObjectPropertyAssertion.Creator<Constructor<?>>() {
            @Override
            public Constructor<?> create() {
                return constructors.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(JavaConstant.MethodHandle.Dispatcher.ForLegacyVm.class).apply();
        ObjectPropertyAssertion.of(JavaConstant.MethodHandle.HandleType.class).apply();
    }

    @SuppressWarnings("unused")
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
