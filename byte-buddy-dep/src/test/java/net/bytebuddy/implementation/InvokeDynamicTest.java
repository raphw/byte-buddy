package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class InvokeDynamicTest {

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

    private static final String BOOTSTRAP_CLASS = "net.bytebuddy.test.precompiled.DynamicInvokeBootstrap";

    private static final String SAMPLE_ENUM = BOOTSTRAP_CLASS + "$SampleEnum";

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
        for (Method method : Class.forName(BOOTSTRAP_CLASS).getDeclaredMethods()) {
            if (!method.getName().equals("bootstrap")) {
                continue;
            }
            DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                    .subclass(Simple.class)
                    .method(isDeclaredBy(Simple.class))
                    .intercept(InvokeDynamic.bootstrap(method).withoutArguments())
                    .make()
                    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
            assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(), is(FOO));
        }
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapConstructor() throws Exception {
        for (Constructor<?> constructor : Class.forName(BOOTSTRAP_CLASS).getDeclaredConstructors()) {
            DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                    .subclass(Simple.class)
                    .method(isDeclaredBy(Simple.class))
                    .intercept(InvokeDynamic.bootstrap(constructor).withoutArguments())
                    .make()
                    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
            assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(), is(FOO));
        }
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithArrayArgumentsWithoutArguments() throws Exception {
        Class<?> type = Class.forName(BOOTSTRAP_CLASS);
        Field field = type.getField("arguments");
        field.set(null, null);
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(type);
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapArrayArguments")).getOnly())
                        .withoutArguments())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(), is(FOO));
        Object[] arguments = (Object[]) field.get(null);
        assertThat(arguments.length, is(0));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testBootstrapWithArrayArgumentsWithArguments() throws Exception {
        Class<?> type = Class.forName(BOOTSTRAP_CLASS);
        Field field = type.getField("arguments");
        field.set(null, null);
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(type);
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapArrayArguments")).getOnly(),
                        INTEGER, LONG, FLOAT, DOUBLE, FOO, CLASS, makeMethodType(CLASS), makeMethodHandle())
                        .withoutArguments())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(), is(FOO));
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
        Class<?> type = Class.forName(BOOTSTRAP_CLASS);
        Field field = type.getField("arguments");
        field.set(null, null);
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(type);
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapExplicitArguments")).getOnly(),
                        INTEGER, LONG, FLOAT, DOUBLE, FOO, CLASS, makeMethodType(CLASS), makeMethodHandle())
                        .withoutArguments())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(), is(FOO));
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
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapExplicitArguments")).getOnly()).withoutArguments();
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testBootstrapOfMethodsWithParametersPrimitive() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        Object value = new Object();
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(FOO, String.class)
                        .withBooleanValue(BOOLEAN)
                        .withByteValue(BYTE)
                        .withShortValue(SHORT)
                        .withCharacterValue(CHARACTER)
                        .withIntegerValue(INTEGER)
                        .withLongValue(LONG)
                        .withFloatValue(FLOAT)
                        .withDoubleValue(DOUBLE)
                        .withType(TypeDescription.ForLoadedType.of(CLASS))
                        .withEnumeration(new EnumerationDescription.ForLoadedEnumeration(makeEnum()))
                        .withInstance(JavaConstant.MethodType.ofLoaded(makeMethodType(CLASS)), JavaConstant.MethodHandle.ofLoaded(makeMethodHandle()))
                        .withValue(FOO, CLASS, makeEnum(), makeMethodType(CLASS), makeMethodHandle(), value))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(),
                is("" + BOOLEAN + BYTE + SHORT + CHARACTER + INTEGER + LONG + FLOAT + DOUBLE + CLASS + makeEnum() + makeMethodType(CLASS)
                        + makeMethodHandle() + FOO + CLASS + makeEnum() + makeMethodType(CLASS) + makeMethodHandle() + value));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testBootstrapOfMethodsWithParametersWrapperConstantPool() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        Object value = new Object();
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(BAR, String.class)
                        .withValue(BOOLEAN, BYTE, SHORT, CHARACTER, INTEGER, LONG, FLOAT, DOUBLE, FOO,
                                CLASS, makeEnum(), makeMethodType(CLASS), makeMethodHandle(), value))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(1));
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(),
                is("" + BOOLEAN + BYTE + SHORT + CHARACTER + INTEGER + LONG + FLOAT + DOUBLE + FOO + CLASS + makeEnum()
                        + makeMethodType(CLASS) + makeMethodHandle() + value));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapOfMethodsWithParametersWrapperReference() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        Object value = new Object();
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(BAR, String.class)
                        .withReference(BOOLEAN, BYTE, SHORT, CHARACTER, INTEGER, LONG, FLOAT, DOUBLE, FOO, CLASS, makeEnum(), makeMethodType(CLASS))
                        .withReference(makeMethodHandle()).as(JavaType.METHOD_HANDLE.load()) // avoid direct method handle
                        .withReference(value))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(14));
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(),
                is("" + BOOLEAN + BYTE + SHORT + CHARACTER + INTEGER + LONG + FLOAT + DOUBLE + FOO + CLASS + makeEnum()
                        + makeMethodType(CLASS) + makeMethodHandle() + value));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithFieldCreation() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .defineField(FOO, String.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withField(FOO))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(1));
        Simple instance = dynamicType.getLoaded().getDeclaredConstructor().newInstance();
        Field field = dynamicType.getLoaded().getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, FOO);
        assertThat(instance.foo(), is(FOO));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithFieldExplicitType() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .defineField(FOO, Object.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withField(FOO).as(String.class)
                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(1));
        Simple instance = dynamicType.getLoaded().getDeclaredConstructor().newInstance();
        Field field = dynamicType.getLoaded().getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, FOO);
        assertThat(instance.foo(), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testBootstrapFieldNotExistent() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        new ByteBuddy()
                .subclass(Simple.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withField(FOO)
                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testBootstrapFieldNotAssignable() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        new ByteBuddy()
                .subclass(Simple.class)
                .defineField(FOO, Object.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withField(FOO).as(String.class))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithFieldUse() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        DynamicType.Loaded<SimpleWithField> dynamicType = new ByteBuddy()
                .subclass(SimpleWithField.class)
                .method(isDeclaredBy(SimpleWithField.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withField(FOO))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        SimpleWithField instance = dynamicType.getLoaded().getDeclaredConstructor().newInstance();
        Field field = SimpleWithField.class.getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, FOO);
        assertThat(instance.foo(), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithFieldUseInvisible() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        new ByteBuddy()
                .subclass(SimpleWithFieldInvisible.class)
                .method(isDeclaredBy(SimpleWithFieldInvisible.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withField(FOO))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithNullValue() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withNullValue(String.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(), nullValue(String.class));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithThisValue() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        DynamicType.Loaded<Simple> dynamicType = new ByteBuddy()
                .subclass(Simple.class)
                .method(isDeclaredBy(Simple.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(BAZ, String.class)
                        .withThis(Object.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        Simple simple = dynamicType.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(simple.foo(), is(simple.toString()));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithArgument() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        DynamicType.Loaded<SimpleWithArgument> dynamicType = new ByteBuddy()
                .subclass(SimpleWithArgument.class)
                .method(isDeclaredBy(SimpleWithArgument.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withArgument(0))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(FOO), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(7)
    public void testNegativeArgumentThrowsException() throws Exception {
        Class<?> type = Class.forName(BOOTSTRAP_CLASS);
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(type);
        InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                .invoke(QUX, String.class)
                .withArgument(-1);
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testNonExistentArgumentThrowsException() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        new ByteBuddy()
                .subclass(SimpleWithArgument.class)
                .method(isDeclaredBy(SimpleWithArgument.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withArgument(1))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testChainedInvocation() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        DynamicType.Loaded<SimpleWithArgument> dynamicType = new ByteBuddy()
                .subclass(SimpleWithArgument.class)
                .method(isDeclaredBy(SimpleWithArgument.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withArgument(0)
                        .andThen(FixedValue.value(BAZ)))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(FOO), is(BAZ));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testBootstrapWithImplicitArgument() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(BOOTSTRAP_CLASS));
        DynamicType.Loaded<SimpleWithArgument> dynamicType = new ByteBuddy()
                .subclass(SimpleWithArgument.class)
                .method(isDeclaredBy(SimpleWithArgument.class))
                .intercept(InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                        .invoke(QUX, String.class)
                        .withMethodArguments())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().foo(FOO), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(7)
    public void testArgumentCannotAssignIllegalInstanceType() throws Exception {
        Class<?> type = Class.forName(BOOTSTRAP_CLASS);
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(type);
        InvokeDynamic.bootstrap(typeDescription.getDeclaredMethods().filter(named("bootstrapSimple")).getOnly())
                .invoke(QUX, String.class)
                .withReference(new Object()).as(String.class);
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testLambdaMetaFactory() throws Exception {
        DynamicType.Loaded<FunctionalFactory> dynamicType = new ByteBuddy()
                .subclass(FunctionalFactory.class)
                .method(isDeclaredBy(FunctionalFactory.class))
                .intercept(InvokeDynamic.lambda(InvokeDynamicTest.class.getMethod("value"), Callable.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().make().call(), is(FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testLambdaMetaFactoryWithArgument() throws Exception {
        DynamicType.Loaded<FunctionalFactoryWithValue> dynamicType = new ByteBuddy()
                .subclass(FunctionalFactoryWithValue.class)
                .method(isDeclaredBy(FunctionalFactoryWithValue.class))
                .intercept(InvokeDynamic.lambda(InvokeDynamicTest.class.getMethod("value", String.class), Callable.class).withArgument(0))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().make(BAR).call(), is(FOO + BAR));
    }

    @Test(expected = LinkageError.class)
    @JavaVersionRule.Enforce(8)
    public void testLambdaMetaFactoryIllegalBinding() throws Exception {
        DynamicType.Loaded<FunctionalFactory> dynamicType = new ByteBuddy()
                .subclass(FunctionalFactory.class)
                .method(isDeclaredBy(FunctionalFactory.class))
                .intercept(InvokeDynamic.lambda(InvokeDynamicTest.class.getMethod("value"), Callable.class).withValue(FOO))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(dynamicType.getLoaded().getDeclaredFields().length, is(0));
        assertThat(dynamicType.getLoaded().getDeclaredConstructor().newInstance().make().call(), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLambdaMetaFactoryNoInterface() throws Exception {
        InvokeDynamic.lambda(InvokeDynamicTest.class.getMethod("value"), Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLambdaMetaFactoryNoFunctionalInterface() throws Exception {
        InvokeDynamic.lambda(InvokeDynamicTest.class.getMethod("value"), ExecutorService.class);
    }

    @SuppressWarnings("unchecked")
    private Enum<?> makeEnum() throws Exception {
        Class type = Class.forName(SAMPLE_ENUM);
        return Enum.valueOf(type, INSTANCE);
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

    public static class SimpleWithMethod {

        public Callable<String> foo(String arg) {
            return null;
        }

        private static String bar(String arg) {
            return arg;
        }
    }

    public interface FunctionalFactory {

        Callable<String> make();
    }

    public interface FunctionalFactoryWithValue {

        Callable<String> make(String argument);
    }

    public static String value() {
        return FOO;
    }

    public static String value(String argument) {
        return FOO + argument;
    }
}
