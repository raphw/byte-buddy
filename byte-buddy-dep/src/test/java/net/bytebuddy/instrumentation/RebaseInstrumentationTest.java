package net.bytebuddy.instrumentation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import net.bytebuddy.utility.RandomString;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class RebaseInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final Object STATIC_METHOD = null;

    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;

    public static class Foo {

        public String bar() {
            return BAR;
        }
    }

    public static class Qux {

        public static String bar() {
            return BAR;
        }
    }

    public static class SuperInterceptor {

        public static String intercept(@SuperCall Callable<String> zuper) throws Exception {
            return FOO + zuper.call();
        }
    }

    @Test
    public void testFixedValueInstanceMethod() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .rebase(Foo.class)
                .method(named(BAR))
                .intercept(FixedValue.value(FOO))
                .make()
                .load(BOOTSTRAP_CLASS_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertEquals(Object.class, dynamicType.getSuperclass());
        assertThat(dynamicType.getName(), is(Foo.class.getName()));
        Method barMethod = dynamicType.getDeclaredMethod(BAR);
        assertThat((String) barMethod.invoke(dynamicType.newInstance()), is(FOO));
    }

    @Test
    public void testFixedValueStaticMethod() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .rebase(Qux.class)
                .method(named(BAR))
                .intercept(FixedValue.value(FOO))
                .make()
                .load(BOOTSTRAP_CLASS_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertEquals(Object.class, dynamicType.getSuperclass());
        assertThat(dynamicType.getName(), is(Qux.class.getName()));
        Method barMethod = dynamicType.getDeclaredMethod(BAR);
        assertThat((String) barMethod.invoke(STATIC_METHOD), is(FOO));
    }

    @Test
    public void testSuperCallInstanceMethod() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .rebase(Foo.class)
                .name(FOO + RandomString.make())
                .method(named(BAR))
                .intercept(MethodDelegation.to(SuperInterceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertEquals(Object.class, dynamicType.getSuperclass());
        Method barMethod = dynamicType.getDeclaredMethod(BAR);
        assertThat((String) barMethod.invoke(dynamicType.newInstance()), is(FOO + BAR));
    }

    @Test
    public void testSuperCallStaticMethod() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .rebase(Qux.class)
                .name(QUX + RandomString.make())
                .method(named(BAR))
                .intercept(MethodDelegation.to(SuperInterceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertEquals(Object.class, dynamicType.getSuperclass());
        Method barMethod = dynamicType.getDeclaredMethod(BAR);
        assertThat((String) barMethod.invoke(STATIC_METHOD), is(FOO + BAR));
    }
}
