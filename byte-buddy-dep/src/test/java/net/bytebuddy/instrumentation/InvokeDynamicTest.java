package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class InvokeDynamicTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";
    private static final boolean BOOLEAN = true;
    private static final byte BYTE = 42;
    private static final short SHORT = 42;
    private static final char CHARACTER = 42;
    private static final int INTEGER = 42;
    private static final long LONG = 42L;
    private static final float FLOAT = 42f;
    private static final double DOUBLE = 42d;

    private static final String STANDARD_ARGUMENT_BOOTSTRAP = "net.bytebuddy.test.precompiled.StandardArgumentBootstrap";
    private static final String PARAMETER_BOOTSTRAP = "net.bytebuddy.test.precompiled.ParameterBootstrap";
    private static final String ARGUMENT_BOOTSTRAP = "net.bytebuddy.test.precompiled.ArgumentBootstrap";

    private static final String BOOTSTRAP_EXPLICIT_ARGUMENTS = "bootstrapExplicitArguments";
    private static final String BOOTSTRAP_ARRAY_ARGUMENTS = "bootstrapArrayArguments";

    private static final String ARGUMENTS_FIELD_NAME = "arguments";
    private static final String BOOTSTRAP = "bootstrap";

    @Rule
    public MethodRule java7Rule = new JavaVersionRule(7);

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapMethod() throws Exception {
        for (Method method : classLoader.loadClass(STANDARD_ARGUMENT_BOOTSTRAP).getDeclaredMethods()) {
            if (method.getName().equals(FOO)) {
                continue;
            }
            DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                    InvokeDynamic.bootstrap(method).withoutImplicitArguments(),
                    classLoader,
                    isDeclaredBy(Simple.class));
            assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        }
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapConstructor() throws Exception {
        for (Constructor<?> constructor : classLoader.loadClass(STANDARD_ARGUMENT_BOOTSTRAP).getDeclaredConstructors()) {
            DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                    InvokeDynamic.bootstrap(constructor).withoutImplicitArguments(),
                    classLoader,
                    isDeclaredBy(Simple.class));
            assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        }
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapWithArrayArgumentsWithoutArguments() throws Exception {
        Class<?> type = classLoader.loadClass(PARAMETER_BOOTSTRAP);
        Field field = type.getDeclaredField(ARGUMENTS_FIELD_NAME);
        field.set(null, null);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP_ARRAY_ARGUMENTS)).getOnly())
                        .withoutImplicitArguments(),
                classLoader,
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        Object[] arguments = (Object[]) field.get(null);
        assertThat(arguments.length, is(0));
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapWithArrayArgumentsWithArguments() throws Exception {
        Class<?> type = classLoader.loadClass(PARAMETER_BOOTSTRAP);
        Field field = type.getDeclaredField(ARGUMENTS_FIELD_NAME);
        field.set(null, null);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP_ARRAY_ARGUMENTS)).getOnly(),
                        BOOLEAN, BYTE, SHORT, CHARACTER, INTEGER, LONG, FLOAT, DOUBLE, FOO)
                        .withoutImplicitArguments(),
                classLoader,
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        Object[] arguments = (Object[]) field.get(null);
        assertThat(arguments.length, is(9));
        assertThat(arguments[0], is((Object) (BOOLEAN ? 1 : 0)));
        assertThat(arguments[1], is((Object) Integer.valueOf(BYTE)));
        assertThat(arguments[2], is((Object) Integer.valueOf(SHORT)));
        assertThat(arguments[3], is((Object) Integer.valueOf(CHARACTER)));
        assertThat(arguments[4], is((Object) INTEGER));
        assertThat(arguments[5], is((Object) LONG));
        assertThat(arguments[6], is((Object) FLOAT));
        assertThat(arguments[7], is((Object) DOUBLE));
        assertThat(arguments[8], is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapWithExplicitArgumentsWithArguments() throws Exception {
        Class<?> type = classLoader.loadClass(PARAMETER_BOOTSTRAP);
        Field field = type.getDeclaredField(ARGUMENTS_FIELD_NAME);
        field.set(null, null);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP_EXPLICIT_ARGUMENTS)).getOnly(),
                        BOOLEAN, BYTE, SHORT, CHARACTER, INTEGER, LONG, FLOAT, DOUBLE, FOO)
                        .withoutImplicitArguments(),
                classLoader,
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        Object[] arguments = (Object[]) field.get(null);
        assertThat(arguments.length, is(9));
        assertThat(arguments[0], is((Object) (BOOLEAN ? 1 : 0)));
        assertThat(arguments[1], is((Object) Integer.valueOf(BYTE)));
        assertThat(arguments[2], is((Object) Integer.valueOf(SHORT)));
        assertThat(arguments[3], is((Object) Integer.valueOf(CHARACTER)));
        assertThat(arguments[4], is((Object) INTEGER));
        assertThat(arguments[5], is((Object) LONG));
        assertThat(arguments[6], is((Object) FLOAT));
        assertThat(arguments[7], is((Object) DOUBLE));
        assertThat(arguments[8], is((Object) FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce
    public void testBootstrapWithExplicitArgumentsWithoutArgumentsThrowsException() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(classLoader.loadClass(PARAMETER_BOOTSTRAP));
        InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP_EXPLICIT_ARGUMENTS)).getOnly()).withoutImplicitArguments();
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapOfMethodsWithParametersPrimitive() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        Object value = new Object();
        DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(FOO, String.class)
                        .withoutImplicitArguments()
                        .withBooleanValue(BOOLEAN)
                        .withByteValue(BYTE)
                        .withShortValue(SHORT)
                        .withCharacterValue(CHARACTER)
                        .withIntegerValue(INTEGER)
                        .withLongValue(LONG)
                        .withFloatValue(FLOAT)
                        .withDoubleValue(DOUBLE)
                        .withValue(FOO, value),
                classLoader,
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().newInstance().foo(),
                is("" + BOOLEAN + BYTE + SHORT + CHARACTER + INTEGER + LONG + FLOAT + DOUBLE + FOO + value));
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapOfMethodsWithParametersWrapperConstantPool() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        Object value = new Object();
        DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(BAR, String.class)
                        .withoutImplicitArguments()
                        .withValue(BOOLEAN, BYTE, SHORT, CHARACTER, INTEGER, LONG, FLOAT, DOUBLE, FOO, value),
                classLoader,
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(1));
        assertThat(dynamicType.getLoaded().newInstance().foo(),
                is("" + BOOLEAN + BYTE + SHORT + CHARACTER + INTEGER + LONG + FLOAT + DOUBLE + FOO + value));
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapOfMethodsWithParametersWrapperReference() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        Object value = new Object();
        DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(BAR, String.class)
                        .withoutImplicitArguments()
                        .withReference(BOOLEAN, BYTE, SHORT, CHARACTER, INTEGER, LONG, FLOAT, DOUBLE, FOO, value),
                classLoader,
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(10));
        assertThat(dynamicType.getLoaded().newInstance().foo(),
                is("" + BOOLEAN + BYTE + SHORT + CHARACTER + INTEGER + LONG + FLOAT + DOUBLE + FOO + value));
    }

    @Test
    public void testBootstrapWithFieldCreation() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withoutImplicitArguments()
                        .withInstanceField(FOO, String.class),
                classLoader,
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(1));
        Simple instance = dynamicType.getLoaded().newInstance();
        Field field = dynamicType.getLoaded().getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, FOO);
        assertThat(instance.foo(), is(FOO));
    }

    @Test
    public void testBootstrapWithFieldUse() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<SimpleWithField> dynamicType = instrument(SimpleWithField.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withoutImplicitArguments()
                        .withField(FOO),
                classLoader,
                isDeclaredBy(SimpleWithField.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        SimpleWithField instance = dynamicType.getLoaded().newInstance();
        Field field = SimpleWithField.class.getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, FOO);
        assertThat(instance.foo(), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testBootstrapWithFieldUseInvisible() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        instrument(SimpleWithFieldInvisible.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withoutImplicitArguments()
                        .withField(FOO),
                classLoader,
                isDeclaredBy(SimpleWithFieldInvisible.class));
    }

    @Test
    public void testBootstrapWithNullValue() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withoutImplicitArguments()
                        .withNullValue(String.class),
                classLoader,
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().newInstance().foo(), nullValue(String.class));
    }

    @Test
    public void testBootstrapWithThisValue() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(BAZ, String.class)
                        .withoutImplicitArguments()
                        .withThis(Object.class),
                classLoader,
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        Simple simple = dynamicType.getLoaded().newInstance();
        assertThat(simple.foo(), is(simple.toString()));
    }

    @Test
    public void testBootstrapWithArgument() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<SimpleWithArgument> dynamicType = instrument(SimpleWithArgument.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withoutImplicitArguments()
                        .withArgument(0),
                classLoader,
                isDeclaredBy(SimpleWithArgument.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().newInstance().foo(FOO), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeArgumentThrowsException() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                .invoke(QUX, String.class)
                .withoutImplicitArguments()
                .withArgument(-1);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonExistentArgumentThrowsException() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        instrument(SimpleWithArgument.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withoutImplicitArguments()
                        .withArgument(1),
                classLoader,
                isDeclaredBy(SimpleWithArgument.class));
    }

    @Test
    public void testChainedInvocation() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<SimpleWithArgument> dynamicType = instrument(SimpleWithArgument.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withoutImplicitArguments()
                        .withArgument(0)
                        .andThen(FixedValue.value(BAZ)),
                classLoader,
                isDeclaredBy(SimpleWithArgument.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().newInstance().foo(FOO), is(BAZ));
    }

    @Test
    public void testBootstrapWithImplicitArgument() throws Exception {
        Class<?> type = classLoader.loadClass(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<SimpleWithArgument> dynamicType = instrument(SimpleWithArgument.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withMethodArgumentsOnly(),
                classLoader,
                isDeclaredBy(SimpleWithArgument.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().newInstance().foo(FOO), is(FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InvokeDynamic.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.Appender.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.WithImplicitTarget.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.WithImplicitArguments.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Default.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Default.Target.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Target.Resolved.Simple.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Target.ForMethodDescription.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Default.NameProvider.ForExplicitName.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Default.ReturnTypeProvider.ForExplicitType.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForBooleanValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForByteValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForShortValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForCharacterValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForIntegerValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForLongValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForFloatValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForDoubleValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForExistingField.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForInstanceField.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForStaticField.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForThisInstance.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.Resolved.Simple.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ConstantPoolWrapper.WrappingArgumentProvider.class).apply();
    }

    public static class Simple {

        public String foo() {
            return null;
        }
    }

    public static class SimpleWithField {

        protected String foo;

        public String foo() {
            return null;
        }
    }

    public static class SimpleWithFieldInvisible {

        private String foo;

        public String foo() {
            return null;
        }
    }

    public static class SimpleWithArgument {

        public String foo(String arg) {
            return null;
        }
    }
}
