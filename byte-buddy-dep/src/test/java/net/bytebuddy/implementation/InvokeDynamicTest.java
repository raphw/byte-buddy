package net.bytebuddy.implementation;

import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
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

public class InvokeDynamicTest extends AbstractImplementationTest {

    public static final String INSTANCE = "INSTANCE";

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final boolean BOOLEAN = true;

    private static final byte BYTE = 42;

    private static final short SHORT = 42;

    private static final char CHARACTER = 42;

    private static final int INTEGER = 42;

    private static final long LONG = 42L;

    private static final float FLOAT = 42f;

    private static final double DOUBLE = 42d;

    private static final Class<?> CLASS = Object.class;

    private static final String STANDARD_ARGUMENT_BOOTSTRAP = "net.bytebuddy.test.precompiled.StandardArgumentBootstrap";

    private static final String PARAMETER_BOOTSTRAP = "net.bytebuddy.test.precompiled.ParameterBootstrap";

    private static final String ARGUMENT_BOOTSTRAP = "net.bytebuddy.test.precompiled.ArgumentBootstrap";

    public static final String SAMPLE_ENUM = ARGUMENT_BOOTSTRAP + "$SampleEnum";

    private static final String BOOTSTRAP_EXPLICIT_ARGUMENTS = "bootstrapExplicitArguments";

    private static final String BOOTSTRAP_ARRAY_ARGUMENTS = "bootstrapArrayArguments";

    private static final String ARGUMENTS_FIELD_NAME = "arguments";

    private static final String BOOTSTRAP = "bootstrap";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private static Object makeMethodType(Class<?> returnType, Class<?>... parameterType) throws Exception {
        return JavaType.METHOD_TYPE.load().getDeclaredMethod("methodType", Class.class, Class[].class).invoke(null, returnType, parameterType);
    }

    private static Object makeMethodHandle() throws Exception {
        Object lookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup").invoke(null);
        return JavaType.METHOD_HANDLES_LOOKUP.load().getDeclaredMethod("findVirtual", Class.class, String.class, JavaType.METHOD_TYPE.load())
                .invoke(lookup, Simple.class, FOO, makeMethodType(String.class));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapMethod() throws Exception {
        for (Method method : Class.forName(STANDARD_ARGUMENT_BOOTSTRAP).getDeclaredMethods()) {
            if (method.getName().equals(FOO)) {
                continue;
            }
            DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                    InvokeDynamic.bootstrap(method).withoutArguments(),
                    getClass().getClassLoader(),
                    isDeclaredBy(Simple.class));
            assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(), is(FOO));
        }
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapConstructor() throws Exception {
        for (Constructor<?> constructor : Class.forName(STANDARD_ARGUMENT_BOOTSTRAP).getDeclaredConstructors()) {
            DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                    InvokeDynamic.bootstrap(constructor).withoutArguments(),
                    getClass().getClassLoader(),
                    isDeclaredBy(Simple.class));
            assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(), is(FOO));
        }
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithArrayArgumentsWithoutArguments() throws Exception {
        Class<?> type = Class.forName(PARAMETER_BOOTSTRAP);
        Field field = type.getDeclaredField(ARGUMENTS_FIELD_NAME);
        field.set(null, null);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP_ARRAY_ARGUMENTS)).getOnly())
                        .withoutArguments(),
                getClass().getClassLoader(),
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(), is(FOO));
        Object[] arguments = (Object[]) field.get(null);
        assertThat(arguments.length, is(0));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testBootstrapWithArrayArgumentsWithArguments() throws Exception {
        Class<?> type = Class.forName(PARAMETER_BOOTSTRAP);
        Field field = type.getDeclaredField(ARGUMENTS_FIELD_NAME);
        field.set(null, null);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP_ARRAY_ARGUMENTS)).getOnly(),
                        INTEGER, LONG, FLOAT, DOUBLE, FOO, CLASS, makeMethodType(CLASS), makeMethodHandle())
                        .withoutArguments(),
                getClass().getClassLoader(),
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(), is(FOO));
        Object[] arguments = (Object[]) field.get(null);
        assertThat(arguments.length, is(8));
        assertThat(arguments[0], is((Object) INTEGER));
        assertThat(arguments[1], is((Object) LONG));
        assertThat(arguments[2], is((Object) FLOAT));
        assertThat(arguments[3], is((Object) DOUBLE));
        assertThat(arguments[4], is((Object) FOO));
        assertThat(arguments[5], is((Object) CLASS));
        assertThat(arguments[6], is(makeMethodType(CLASS)));
        assertThat(JavaConstant.MethodHandle.ofLoaded(arguments[7]), is(JavaConstant.MethodHandle.ofLoaded(makeMethodHandle())));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testBootstrapWithExplicitArgumentsWithArguments() throws Exception {
        Class<?> type = Class.forName(PARAMETER_BOOTSTRAP);
        Field field = type.getDeclaredField(ARGUMENTS_FIELD_NAME);
        field.set(null, null);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP_EXPLICIT_ARGUMENTS)).getOnly(),
                        INTEGER, LONG, FLOAT, DOUBLE, FOO, CLASS, makeMethodType(CLASS), makeMethodHandle())
                        .withoutArguments(),
                getClass().getClassLoader(),
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(), is(FOO));
        Object[] arguments = (Object[]) field.get(null);
        assertThat(arguments.length, is(8));
        assertThat(arguments[0], is((Object) INTEGER));
        assertThat(arguments[1], is((Object) LONG));
        assertThat(arguments[2], is((Object) FLOAT));
        assertThat(arguments[3], is((Object) DOUBLE));
        assertThat(arguments[4], is((Object) FOO));
        assertThat(arguments[5], is((Object) CLASS));
        assertThat(arguments[6], is(makeMethodType(CLASS)));
        assertThat(JavaConstant.MethodHandle.ofLoaded(arguments[7]), is(JavaConstant.MethodHandle.ofLoaded(makeMethodHandle())));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithExplicitArgumentsWithoutArgumentsThrowsException() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Class.forName(PARAMETER_BOOTSTRAP));
        InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP_EXPLICIT_ARGUMENTS)).getOnly()).withoutArguments();
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testBootstrapOfMethodsWithParametersPrimitive() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(Class.forName(ARGUMENT_BOOTSTRAP));
        Object value = new Object();
        DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(FOO, String.class)
                        .withBooleanValue(BOOLEAN)
                        .withByteValue(BYTE)
                        .withShortValue(SHORT)
                        .withCharacterValue(CHARACTER)
                        .withIntegerValue(INTEGER)
                        .withLongValue(LONG)
                        .withFloatValue(FLOAT)
                        .withDoubleValue(DOUBLE)
                        .withType(new TypeDescription.ForLoadedType(CLASS))
                        .withEnumeration(new EnumerationDescription.ForLoadedEnumeration(makeEnum()))
                        .withInstance(JavaConstant.MethodType.ofLoaded(makeMethodType(CLASS)), JavaConstant.MethodHandle.ofLoaded(makeMethodHandle()))
                        .withValue(FOO, CLASS, makeEnum(), makeMethodType(CLASS), makeMethodHandle(), value),
                getClass().getClassLoader(),
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(),
                is("" + BOOLEAN + BYTE + SHORT + CHARACTER + INTEGER + LONG + FLOAT + DOUBLE + CLASS + makeEnum() + makeMethodType(CLASS)
                        + makeMethodHandle() + FOO + CLASS + makeEnum() + makeMethodType(CLASS) + makeMethodHandle() + value));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testBootstrapOfMethodsWithParametersWrapperConstantPool() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        Object value = new Object();
        DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(BAR, String.class)
                        .withValue(BOOLEAN, BYTE, SHORT, CHARACTER, INTEGER, LONG, FLOAT, DOUBLE, FOO,
                                CLASS, makeEnum(), makeMethodType(CLASS), makeMethodHandle(), value),
                getClass().getClassLoader(),
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(1));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(),
                is("" + BOOLEAN + BYTE + SHORT + CHARACTER + INTEGER + LONG + FLOAT + DOUBLE + FOO + CLASS + makeEnum()
                        + makeMethodType(CLASS) + makeMethodHandle() + value));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapOfMethodsWithParametersWrapperReference() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        Object value = new Object();
        DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(BAR, String.class)
                        .withReference(BOOLEAN, BYTE, SHORT, CHARACTER, INTEGER, LONG, FLOAT, DOUBLE, FOO, CLASS, makeEnum(), makeMethodType(CLASS))
                        .withReference(makeMethodHandle()).as(JavaType.METHOD_HANDLE.load()) // avoid direct method handle
                        .withReference(value),
                getClass().getClassLoader(),
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(14));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(),
                is("" + BOOLEAN + BYTE + SHORT + CHARACTER + INTEGER + LONG + FLOAT + DOUBLE + FOO + CLASS + makeEnum()
                        + makeMethodType(CLASS) + makeMethodHandle() + value));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithFieldCreation() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withInstanceField(FOO, String.class),
                getClass().getClassLoader(),
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(1));
        Simple instance = dynamicType.getLoaded().getConstructor().newInstance();
        Field field = dynamicType.getLoaded().getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, FOO);
        assertThat(instance.foo(), is(FOO));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithFieldUse() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<SimpleWithField> dynamicType = implement(SimpleWithField.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withField(FOO),
                getClass().getClassLoader(),
                isDeclaredBy(SimpleWithField.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        SimpleWithField instance = dynamicType.getLoaded().getConstructor().newInstance();
        Field field = SimpleWithField.class.getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, FOO);
        assertThat(instance.foo(), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithFieldUseInvisible() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        implement(SimpleWithFieldInvisible.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withField(FOO),
                getClass().getClassLoader(),
                isDeclaredBy(SimpleWithFieldInvisible.class));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithNullValue() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withNullValue(String.class),
                getClass().getClassLoader(),
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(), nullValue(String.class));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithThisValue() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<Simple> dynamicType = implement(Simple.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(BAZ, String.class)
                        .withThis(Object.class),
                getClass().getClassLoader(),
                isDeclaredBy(Simple.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        Simple simple = dynamicType.getLoaded().getConstructor().newInstance();
        assertThat(simple.foo(), is(simple.toString()));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithArgument() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<SimpleWithArgument> dynamicType = implement(SimpleWithArgument.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withArgument(0),
                getClass().getClassLoader(),
                isDeclaredBy(SimpleWithArgument.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(FOO), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(7)
    public void testNegativeArgumentThrowsException() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                .invoke(QUX, String.class)
                .withArgument(-1);
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testNonExistentArgumentThrowsException() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        implement(SimpleWithArgument.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withArgument(1),
                getClass().getClassLoader(),
                isDeclaredBy(SimpleWithArgument.class));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testChainedInvocation() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<SimpleWithArgument> dynamicType = implement(SimpleWithArgument.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withArgument(0)
                        .andThen(FixedValue.value(BAZ)),
                getClass().getClassLoader(),
                isDeclaredBy(SimpleWithArgument.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(FOO), is(BAZ));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithImplicitArgument() throws Exception {
        Class<?> type = Class.forName(ARGUMENT_BOOTSTRAP);
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(type);
        DynamicType.Loaded<SimpleWithArgument> dynamicType = implement(SimpleWithArgument.class,
                InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named(BOOTSTRAP)).getOnly())
                        .invoke(QUX, String.class)
                        .withMethodArguments(),
                getClass().getClassLoader(),
                isDeclaredBy(SimpleWithArgument.class));
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getConstructor().newInstance().foo(FOO), is(FOO));
    }

    @SuppressWarnings("unchecked")
    private Enum<?> makeEnum() throws Exception {
        Class type = Class.forName(SAMPLE_ENUM);
        return Enum.valueOf(type, INSTANCE);
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
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.NameProvider.ForExplicitName.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.NameProvider.ForInterceptedMethod.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ReturnTypeProvider.ForInterceptedMethod.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ReturnTypeProvider.ForExplicitType.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForBooleanConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForByteConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForShortConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForCharacterConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForIntegerConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForLongConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForFloatConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForDoubleConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForStringConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForClassConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForEnumerationValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForExistingField.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForInstanceField.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForStaticField.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForThisInstance.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForJavaConstant.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForMethodParameter.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForExplicitTypedMethodParameter.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ConstantPoolWrapper.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ConstantPoolWrapper.WrappingArgumentProvider.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForInterceptedMethodParameters.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForInterceptedMethodInstanceAndParameters.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.Resolved.Simple.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.TerminationHandler.ForMethodReturn.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.TerminationHandler.ForChainedInvocation.class).apply();
    }

    public static class Simple {

        public String foo() {
            return null;
        }
    }

    public static class SimpleWithField {

        public String foo;

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
