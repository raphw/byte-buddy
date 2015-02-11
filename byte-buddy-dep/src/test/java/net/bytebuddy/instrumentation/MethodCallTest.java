package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

public class MethodCallTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testStaticMethodInvocationWithoutArguments() throws Exception {
        DynamicType.Loaded<StaticMethod> loaded = instrument(StaticMethod.class,
                MethodCall.invoke(StaticMethod.class.getDeclaredMethod(BAR)),
                StaticMethod.class.getClassLoader(),
                named(FOO));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        StaticMethod instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), is(BAR));
        assertNotEquals(StaticMethod.class, instance.getClass());
        assertThat(instance, instanceOf(StaticMethod.class));
    }

    public static class StaticMethod {

        public String foo() {
            return null;
        }

        public String bar() {
            return BAR;
        }
    }

    @Test
    public void testExternalStaticMethodInvocationWithoutArguments() throws Exception {
        DynamicType.Loaded<StaticMethod> loaded = instrument(StaticMethod.class,
                MethodCall.invoke(StaticExternalMethod.class.getDeclaredMethod(BAR)),
                StaticMethod.class.getClassLoader(),
                named(FOO));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        StaticMethod instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), is(BAR));
        assertNotEquals(StaticMethod.class, instance.getClass());
        assertThat(instance, instanceOf(StaticMethod.class));
    }

    public static class StaticExternalMethod {

        public static String bar() {
            return BAR;
        }
    }

    @Test
    public void testInstanceMethodInvocationWithoutArguments() throws Exception {
        DynamicType.Loaded<InstanceMethod> loaded = instrument(InstanceMethod.class,
                MethodCall.invoke(InstanceMethod.class.getDeclaredMethod(BAR)),
                InstanceMethod.class.getClassLoader(),
                named(FOO));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        InstanceMethod instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), is(BAR));
        assertNotEquals(InstanceMethod.class, instance.getClass());
        assertThat(instance, instanceOf(InstanceMethod.class));
    }

    public static class InstanceMethod {

        public String foo() {
            return null;
        }

        public String bar() {
            return BAR;
        }
    }

    @Test
    public void testSuperConstructorInvocationWithoutArguments() throws Exception {
        DynamicType.Loaded<SuperConstructorCall> loaded = instrument(SuperConstructorCall.class,
                MethodCall.invoke(Object.class.getDeclaredConstructor()),
                SuperConstructorCall.class.getClassLoader(),
                isConstructor());
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(0));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SuperConstructorCall instance = loaded.getLoaded().newInstance();
        assertNotEquals(SuperConstructorCall.class, instance.getClass());
        assertThat(instance, instanceOf(SuperConstructorCall.class));
    }

    public static class SuperConstructorCall {
        /* empty */
    }

    @Test
    public void testObjectConstruction() throws Exception {
        DynamicType.Loaded<SelfReference> loaded = instrument(SelfReference.class,
                MethodCall.construct(SelfReference.class.getDeclaredConstructor()));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SelfReference instance = loaded.getLoaded().newInstance();
        SelfReference created = instance.foo();
        assertEquals(SelfReference.class, created.getClass());
        assertNotEquals(SelfReference.class, instance.getClass());
        assertThat(instance, instanceOf(SelfReference.class));
        assertThat(created, not(instance));
    }

    public static class SelfReference {

        public SelfReference foo() {
            return null;
        }
    }

    @Test
    public void testSuperInvocation() throws Exception {
        DynamicType.Loaded<SuperMethodInvocation> loaded = instrument(SuperMethodInvocation.class,
                MethodCall.invokeSuper(),
                SuperMethodInvocation.class.getClassLoader(),
                takesArguments(0).and(named(FOO)));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SuperMethodInvocation instance = loaded.getLoaded().newInstance();
        assertNotEquals(SuperMethodInvocation.class, instance.getClass());
        assertThat(instance, instanceOf(SuperMethodInvocation.class));
        assertThat(instance.foo(), is(FOO));
    }

    public static class SuperMethodInvocation {

        public String foo() {
            return FOO;
        }
    }

    @Test
    public void testWithExplicitArgumentConstantPool() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = instrument(MethodCallWithExplicitArgument.class,
                MethodCall.invokeSuper().with(FOO));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithExplicitArgument instance = loaded.getLoaded().newInstance();
        assertNotEquals(MethodCallWithExplicitArgument.class, instance.getClass());
        assertThat(instance, instanceOf(MethodCallWithExplicitArgument.class));
        assertThat(instance.foo(BAR), is(FOO));
    }

    @Test
    public void testWithExplicitArgumentField() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = instrument(MethodCallWithExplicitArgument.class,
                MethodCall.invokeSuper().withReference(FOO));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        MethodCallWithExplicitArgument instance = loaded.getLoaded().newInstance();
        assertNotEquals(MethodCallWithExplicitArgument.class, instance.getClass());
        assertThat(instance, instanceOf(MethodCallWithExplicitArgument.class));
        assertThat(instance.foo(BAR), is(FOO));
    }

    @Test
    public void testWithParameter() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = instrument(MethodCallWithExplicitArgument.class,
                MethodCall.invokeSuper().withArgument(0));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithExplicitArgument instance = loaded.getLoaded().newInstance();
        assertNotEquals(MethodCallWithExplicitArgument.class, instance.getClass());
        assertThat(instance, instanceOf(MethodCallWithExplicitArgument.class));
        assertThat(instance.foo(BAR), is(BAR));
    }

    @Test
    public void testWithInstanceField() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = instrument(MethodCallWithExplicitArgument.class,
                MethodCall.invokeSuper().withInstanceField(String.class, FOO));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        MethodCallWithExplicitArgument instance = loaded.getLoaded().newInstance();
        Field field = instance.getClass().getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, FOO);
        assertNotEquals(MethodCallWithExplicitArgument.class, instance.getClass());
        assertThat(instance, instanceOf(MethodCallWithExplicitArgument.class));
        assertThat(instance.foo(BAR), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithTooBigParameter() throws Exception {
        instrument(MethodCallWithExplicitArgument.class, MethodCall.invokeSuper().withArgument(1));
    }

    public static class MethodCallWithExplicitArgument {

        public String foo(String value) {
            return value;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodTypeIncompatible() throws Exception {
        instrument(InstanceMethod.class,
                MethodCall.invoke(String.class.getDeclaredMethod("toLowerCase")),
                InstanceMethod.class.getClassLoader(),
                named(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentIncompatibleTooFew() throws Exception {
        instrument(InstanceMethod.class,
                MethodCall.invoke(StaticIncompatibleExternalMethod.class.getDeclaredMethod("bar", String.class)),
                InstanceMethod.class.getClassLoader(),
                named(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentIncompatibleTooMany() throws Exception {
        instrument(InstanceMethod.class,
                MethodCall.invoke(StaticIncompatibleExternalMethod.class.getDeclaredMethod("bar", String.class))
                .with(FOO, BAR),
                InstanceMethod.class.getClassLoader(),
                named(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentIncompatibleNotAssignable() throws Exception {
        instrument(InstanceMethod.class,
                MethodCall.invoke(StaticIncompatibleExternalMethod.class.getDeclaredMethod("bar", String.class))
                        .with(new Object()),
                InstanceMethod.class.getClassLoader(),
                named(FOO));
    }

    public static class StaticIncompatibleExternalMethod {

        public static String bar(String value) {
            return null;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructNonConstructorThrowsException() throws Exception {
        MethodCall.construct(mock(MethodDescription.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalIndex() throws Exception {
        MethodCall.invokeSuper().withArgument(-1);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodCall.class).apply();
        ObjectPropertyAssertion.of(MethodCall.WithoutSpecifiedTarget.class).apply();
        ObjectPropertyAssertion.of(MethodCall.Appender.class).apply();
        ObjectPropertyAssertion.of(MethodCall.MethodLocator.ForExplicitMethod.class).apply();
        ObjectPropertyAssertion.of(MethodCall.TargetHandler.ForStaticField.class).apply();
        ObjectPropertyAssertion.of(MethodCall.TargetHandler.ForInstanceField.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForBooleanConstant.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForByteConstant.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForCharacterConstant.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForDoubleConstant.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForStaticField.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForInstanceField.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForExistingField.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForFloatConstant.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForIntegerConstant.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForLongConstant.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForMethodParameter.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForShortConstant.class).apply();
        ObjectPropertyAssertion.of(MethodCall.ArgumentLoader.ForTextConstant.class).apply();
    }
}
