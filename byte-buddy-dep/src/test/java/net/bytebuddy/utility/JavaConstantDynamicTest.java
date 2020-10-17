package net.bytebuddy.utility;

import java.lang.reflect.Method;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class JavaConstantDynamicTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantFactoryLookupOnly() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getMethod("bootstrap",
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        Object[].class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantFactoryLookupAndStringOnly() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getMethod("bootstrap",
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Object[].class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantFactoryNoVarargs() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getMethod("bootstrap",
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Class.class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantFactoryVarargs() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getMethod("bootstrap",
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Class.class,
                        Object[].class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantFactoryNested() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getMethod("bootstrap",
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Class.class,
                        bootstrap), JavaConstant.Dynamic.bootstrap(BAR, bootstrap.getMethod("bootstrap",
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        Object[].class)))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantFactoryWithArguments() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO,
                        bootstrap.getMethod("bootstrap",
                                Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                                String.class,
                                Class.class,
                                int.class,
                                long.class,
                                float.class,
                                double.class,
                                String.class,
                                Class.class,
                                Class.forName("java.lang.invoke.MethodHandle"),
                                Class.forName("java.lang.invoke.MethodType")),
                        42, 42L, 42f, 42d, FOO,
                        TypeDescription.ForLoadedType.of(Object.class),
                        JavaConstant.MethodHandle.ofLoaded(methodHandle()),
                        JavaConstant.MethodType.ofLoaded(methodType()))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantConstructorLookupOnly() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getConstructor(
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        Object[].class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantConstructorLookupAndStringOnly() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getConstructor(
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Object[].class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantConstructorNoVarargs() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getConstructor(
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Class.class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantConstructorVarargs() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getConstructor(
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Class.class,
                        Object[].class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantConstructorNested() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getConstructor(
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Class.class,
                        bootstrap), JavaConstant.Dynamic.bootstrap(BAR, bootstrap.getConstructor(
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        Object[].class)))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantConstructorWithArguments() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(FOO,
                        bootstrap.getConstructor(
                                Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                                String.class,
                                Class.class,
                                int.class,
                                long.class,
                                float.class,
                                double.class,
                                String.class,
                                Class.class,
                                Class.forName("java.lang.invoke.MethodHandle"),
                                Class.forName("java.lang.invoke.MethodType")),
                        42, 42L, 42f, 42d, FOO,
                        TypeDescription.ForLoadedType.of(Object.class),
                        JavaConstant.MethodHandle.ofLoaded(methodHandle()),
                        JavaConstant.MethodType.ofLoaded(methodType()))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testNullConstant() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofNullConstant()))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), nullValue(Object.class));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testPrimitiveType() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofPrimitiveType(void.class)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance((Object) void.class));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testEnumeration() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofEnumeration(SampleEnum.INSTANCE)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance((Object) SampleEnum.INSTANCE));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testField() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofField(SampleClass.class.getDeclaredField("FOO"))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(SampleClass.FOO));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testFieldWithSelfDeclaredType() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofField(SampleClass.class.getDeclaredField("BAR"))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance((Object) SampleClass.BAR));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testConstructWithArguments() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofInvocation(bootstrap.getConstructor(
                        int.class,
                        long.class,
                        float.class,
                        double.class,
                        String.class,
                        Class.class,
                        Class.forName("java.lang.invoke.MethodHandle"),
                        Class.forName("java.lang.invoke.MethodType")),
                        42, 42L, 42f, 42d, FOO,
                        TypeDescription.ForLoadedType.of(Object.class),
                        JavaConstant.MethodHandle.ofLoaded(methodHandle()),
                        JavaConstant.MethodType.ofLoaded(methodType()))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testInvoke() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofInvocation(SampleClass.class.getMethod("make"))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(SampleClass.class));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testConstruct() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofInvocation(SampleClass.class.getConstructor())))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(SampleClass.class));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testInvokeWithArguments() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofInvocation(bootstrap.getMethod("make",
                        int.class,
                        long.class,
                        float.class,
                        double.class,
                        String.class,
                        Class.class,
                        Class.forName("java.lang.invoke.MethodHandle"),
                        Class.forName("java.lang.invoke.MethodType")),
                        42, 42L, 42f, 42d, FOO,
                        TypeDescription.ForLoadedType.of(Object.class),
                        JavaConstant.MethodHandle.ofLoaded(methodHandle()),
                        JavaConstant.MethodType.ofLoaded(methodType()))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrap));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testInvocationOfVarargsMethod() throws Exception {
        final Integer[] sourceIntegers = new Integer[] { Integer.valueOf(1), Integer.valueOf(2) };
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
            .intercept(FixedValue.value(toJavaConstant(sourceIntegers)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat((Integer[])baz.getDeclaredMethod(FOO).invoke(foo), equalTo(sourceIntegers));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testStaticFieldVarHandle() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofVarHandle(SampleClass.class.getDeclaredField("FOO"))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(Class.forName("java.lang.invoke.VarHandle")));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testNonStaticFieldVarHandle() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofVarHandle(SampleClass.class.getDeclaredField("qux"))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(Class.forName("java.lang.invoke.VarHandle")));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testArrayVarHandle() throws Exception {
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.ofArrayVarHandle(Object[].class)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(Class.forName("java.lang.invoke.VarHandle")));
    }

    @Test
    public void testTypeResolution() {
        assertThat(JavaConstant.Dynamic.ofNullConstant(), equalTo(JavaConstant.Dynamic.ofNullConstant()));
        assertThat(JavaConstant.Dynamic.ofNullConstant(), not(equalTo(JavaConstant.Dynamic.ofNullConstant().withType(TypeDescription.STRING))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTypeResolutionForVoid() throws Exception {
        JavaConstant.Dynamic.ofNullConstant().withType(void.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompatibleTypeForMethod() throws Exception {
        JavaConstant.Dynamic.ofInvocation(Object.class.getMethod("toString"), FOO).withType(Integer.class);
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testConstructorTypeResolutionCompatible() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        JavaConstant.Dynamic dynamic = JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getConstructor(
                Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                Object[].class));
        assertThat(dynamic.withType(bootstrap), equalTo((JavaConstant) dynamic));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(11)
    public void testConstructorTypeResolutionIncompatible() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        JavaConstant.Dynamic.bootstrap(FOO, bootstrap.getConstructor(
                Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                Object[].class)).withType(String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrimitiveNonPrimitive() throws Exception {
        JavaConstant.Dynamic.ofPrimitiveType(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFieldNonStatic() throws Exception {
        JavaConstant.Dynamic.ofField(SampleClass.class.getField("qux"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFieldNonFinal() throws Exception {
        JavaConstant.Dynamic.ofField(SampleClass.class.getField("baz"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeVoid() throws Exception {
        JavaConstant.Dynamic.ofInvocation(SampleClass.class.getMethod("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeNonStatic() throws Exception {
        JavaConstant.Dynamic.ofInvocation(SampleClass.class.getMethod("bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeWrongArguments() throws Exception {
        JavaConstant.Dynamic.ofInvocation(SampleClass.class.getConstructor(), "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayVarHandleNoArray() throws Exception {
        JavaConstant.Dynamic.ofArrayVarHandle(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBootstrapNonBootstrap() throws Exception {
        JavaConstant.Dynamic.bootstrap(FOO, SampleClass.class.getMethod("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(11)
    public void testEmptyName() throws Exception {
        JavaConstant.Dynamic.bootstrap("", Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap").getMethod("bootstrap",
                Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                Object[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(11)
    public void testNameWithDot() throws Exception {
        JavaConstant.Dynamic.bootstrap(".", Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap").getMethod("bootstrap",
                Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                Object[].class));
    }

    public static class Foo {

        public Object foo() {
            return null;
        }
    }

    public enum SampleEnum {
        INSTANCE;
    }

    public static class SampleClass {

        public static final Object FOO = new Object();

        public static final SampleClass BAR = new SampleClass();

        public final Object qux = new Object();

        public static Object baz = new Object();

        public static SampleClass make() {
            return new SampleClass();
        }

        public static void foo() {
            /* empty */
        }

        public Object bar() {
            return null;
        }

    }

    private static Object methodHandle() throws Exception {
        return Class.forName("java.lang.invoke.MethodHandles$Lookup")
                .getMethod("findConstructor", Class.class, Class.forName("java.lang.invoke.MethodType"))
                .invoke(Class.forName("java.lang.invoke.MethodHandles").getMethod("lookup").invoke(null), JavaConstantDynamicTest.class, methodType());
    }

    private static Object methodType() throws Exception {
        return Class.forName("java.lang.invoke.MethodType").getMethod("methodType", Class.class).invoke(null, void.class);
    }

    private static JavaConstant toJavaConstant(Integer[] integers) throws Exception {
        // This is convoluted and exists only to test issue #954.
        JavaConstant[] constants = new JavaConstant[integers.length];
        for (int i = 0; i < integers.length; i++) {
            constants[i] = toJavaConstant(integers[i]);
        }
        return JavaConstant.Dynamic.ofInvocation(JavaConstantDynamicTest.class.getDeclaredMethod("toIntegers", Integer[].class), constants);
    }

    private static JavaConstant toJavaConstant(Integer i) throws Exception {
        // This is convoluted and exists only to test issue #954.
        return JavaConstant.Dynamic.ofInvocation(Integer.class.getMethod("valueOf", String.class), i.toString());
    }

    public static Integer[] toIntegers(final Integer... varargs) {
        // This is convoluted and exists only to test issue #954.
        return varargs;
    }

}
