package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaConstant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class DynamicConstantTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantLookupOnly() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<?> bootstrapSample = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap$Sample");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(bootstrap.getMethod("bootstrap",
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        Object[].class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrapSample));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantLookupAndStringOnly() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<?> bootstrapSample = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap$Sample");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(bootstrap.getMethod("bootstrap",
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Object[].class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrapSample));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantNoVarargs() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<?> bootstrapSample = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap$Sample");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(bootstrap.getMethod("bootstrap",
                        Class.forName("java.lang.invoke.MethodHandles$Lookup"),
                        String.class,
                        Class.class))))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(baz.getDeclaredFields().length, is(0));
        assertThat(baz.getDeclaredMethods().length, is(1));
        Foo foo = baz.getDeclaredConstructor().newInstance();
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrapSample));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testDynamicConstantVarargs() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap");
        Class<?> bootstrapSample = Class.forName("net.bytebuddy.test.precompiled.DynamicConstantBootstrap$Sample");
        Class<? extends Foo> baz = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FixedValue.value(JavaConstant.Dynamic.bootstrap(bootstrap.getMethod("bootstrap",
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
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), instanceOf(bootstrapSample));
        assertThat(baz.getDeclaredMethod(FOO).invoke(foo), sameInstance(baz.getDeclaredMethod(FOO).invoke(foo)));
    }


    public static class Foo {

        public Object foo() {
            return null;
        }
    }
}
