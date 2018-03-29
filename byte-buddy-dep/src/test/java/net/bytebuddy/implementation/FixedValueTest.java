package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class FixedValueTest {

    private static final String BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private Bar bar;

    private static Object makeMethodType(Class<?> returnType, Class<?>... parameterType) throws Exception {
        return JavaType.METHOD_TYPE.load().getDeclaredMethod("methodType", Class.class, Class[].class).invoke(null, returnType, parameterType);
    }

    private static Object makeMethodHandle() throws Exception {
        Object lookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup").invoke(null);
        return JavaType.METHOD_HANDLES_LOOKUP.load().getDeclaredMethod("findVirtual", Class.class, String.class, JavaType.METHOD_TYPE.load())
                .invoke(lookup, Qux.class, BAR, makeMethodType(Object.class));
    }

    @Before
    public void setUp() throws Exception {
        bar = new Bar();
    }

    @Test
    public void testTypeDescriptionConstantPool() throws Exception {
        Class<? extends Qux> qux = new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(FixedValue.value(TypeDescription.OBJECT))
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(qux.getDeclaredConstructor().newInstance().bar(), is((Object) Object.class));
    }

    @Test
    public void testClassConstantPool() throws Exception {
        Class<? extends Qux> qux = new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(FixedValue.value(Object.class))
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(qux.getDeclaredConstructor().newInstance().bar(), is((Object) Object.class));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeConstantPool() throws Exception {
        Class<? extends Qux> qux = new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(FixedValue.value(JavaConstant.MethodType.of(void.class, Object.class)))
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(qux.getDeclaredConstructor().newInstance().bar(), is(makeMethodType(void.class, Object.class)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeConstantPoolValue() throws Exception {
        Class<? extends Qux> qux = new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(FixedValue.value(makeMethodType(void.class, Object.class)))
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(qux.getDeclaredConstructor().newInstance().bar(), is(makeMethodType(void.class, Object.class)));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testMethodHandleConstantPool() throws Exception {
        Class<? extends Qux> qux = new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(FixedValue.value(JavaConstant.MethodHandle.of(Qux.class.getDeclaredMethod("bar"))))
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(JavaConstant.MethodHandle.ofLoaded(qux.getDeclaredConstructor().newInstance().bar()), is(JavaConstant.MethodHandle.ofLoaded(makeMethodHandle())));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testMethodHandleConstantPoolValue() throws Exception {
        Class<? extends Qux> qux = new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(FixedValue.value(makeMethodHandle()))
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(JavaConstant.MethodHandle.ofLoaded(qux.getDeclaredConstructor().newInstance().bar()), is(JavaConstant.MethodHandle.ofLoaded(makeMethodHandle())));
    }

    @Test
    public void testReferenceCall() throws Exception {
        new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(FixedValue.reference(bar))
                .make();
    }

    @Test
    public void testValueCall() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.reference(bar))
                .make();
    }

    @Test
    public void testNullValue() throws Exception {
        Class<? extends Foo> foo = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.nullValue())
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(foo.getDeclaredFields().length, is(0));
        assertThat(foo.getDeclaredMethods().length, is(1));
        assertThat(foo.getDeclaredMethod(BAR).invoke(foo.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testNullValueNonAssignable() throws Exception {
        new ByteBuddy()
                .subclass(FooBar.class)
                .method(isDeclaredBy(FooBar.class))
                .intercept(FixedValue.nullValue())
                .make();
    }

    @Test
    public void testThisValue() throws Exception {
        Class<? extends QuxBaz> quxbaz = new ByteBuddy()
                .subclass(QuxBaz.class)
                .method(isDeclaredBy(QuxBaz.class))
                .intercept(FixedValue.self())
                .make()
                .load(QuxBaz.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(quxbaz.getDeclaredFields().length, is(0));
        assertThat(quxbaz.getDeclaredMethods().length, is(1));
        QuxBaz self = quxbaz.getDeclaredConstructor().newInstance();
        assertThat(self.bar(), sameInstance((Object) self));
    }

    @Test(expected = IllegalStateException.class)
    public void testThisValueStatic() throws Exception {
        new ByteBuddy()
                .redefine(FooBarQuxBaz.class)
                .method(named("bar"))
                .intercept(FixedValue.self())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testThisValueNonAssignable() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.self())
                .make();
    }

    @Test
    public void testOriginType() throws Exception {
        Class<? extends Baz> baz = new ByteBuddy()
                .subclass(Baz.class)
                .method(isDeclaredBy(Baz.class))
                .intercept(FixedValue.originType())
                .make()
                .load(QuxBaz.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        assertThat(baz.getDeclaredMethod(BAR).invoke(baz.getDeclaredConstructor().newInstance()), is((Object) Baz.class));
    }

    @Test
    public void testArgument() throws Exception {
        Class<? extends FooQux> fooQux = new ByteBuddy()
                .subclass(FooQux.class)
                .method(isDeclaredBy(FooQux.class))
                .intercept(FixedValue.argument(1))
                .make()
                .load(FooQux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        assertThat(fooQux.getDeclaredFields().length, is(0));
        assertThat(fooQux.getDeclaredMethods().length, is(1));
        assertThat(fooQux.getDeclaredMethod(BAR, Integer.class, String.class)
                .invoke(fooQux.getDeclaredConstructor().newInstance(), 0, BAR), is((Object) BAR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArgumentNegative() throws Exception {
        FixedValue.argument(-1);
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentNotAssignable() throws Exception {
        new ByteBuddy()
                .subclass(FooQux.class)
                .method(isDeclaredBy(FooQux.class))
                .intercept(FixedValue.argument(0))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentNonExistent() throws Exception {
        new ByteBuddy()
                .subclass(FooQux.class)
                .method(isDeclaredBy(FooQux.class))
                .intercept(FixedValue.argument(2))
                .make();
    }

    public static class Foo extends CallTraceable {

        public Bar bar() {
            register(BAR);
            return new Bar();
        }
    }

    public static class Bar {
        /* empty */
    }

    public static class Qux extends CallTraceable {

        public Object bar() {
            register(BAR);
            return null;
        }
    }

    public static class Baz {

        public Class<?> bar() {
            return null;
        }
    }

    public static class FooBar {

        public void bar() {
            /* empty */
        }
    }

    public static class QuxBaz {

        public Object bar() {
            return null;
        }
    }

    public static class FooBarQuxBaz {

        public static Object bar() {
            return null;
        }
    }

    public static class FooQux {

        public String bar(Integer arg0, String arg1) {
            return null;
        }
    }
}
