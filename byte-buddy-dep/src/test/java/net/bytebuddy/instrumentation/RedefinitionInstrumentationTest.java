package net.bytebuddy.instrumentation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RedefinitionInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", TO_STRING = "toString";

    @Test
    public void testFixedValueInstanceMethod() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .redefine(Foo.class)
                .method(named(TO_STRING))
                .intercept(FixedValue.value(FOO))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertEquals(Object.class, dynamicType.getSuperclass());
        assertNotEquals(Foo.class, dynamicType);
        assertThat(dynamicType.getName(), is(Foo.class.getName()));
        Method barMethod = dynamicType.getDeclaredMethod(TO_STRING);
        assertThat((String) barMethod.invoke(dynamicType.newInstance()), is(FOO));
    }

    @Test
    public void testSuperCallInstanceMethod() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .redefine(Qux.class)
                .method(named(BAR))
                .intercept(MethodDelegation.to(SuperInterceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertEquals(Bar.class, dynamicType.getSuperclass());
        assertNotEquals(Qux.class, dynamicType);
        assertThat(dynamicType.getName(), is(Qux.class.getName()));
        Method barMethod = dynamicType.getDeclaredMethod(BAR);
        assertThat((String) barMethod.invoke(dynamicType.newInstance()), is(FOO + BAR));
    }

    @SuppressWarnings("unused")
    public static class Base {

        public String bar() {
            return BAZ;
        }
    }

    public static class Foo {

        @Override
        public String toString() {
            return BAR;
        }
    }

    public static class Bar {

        public String bar() {
            return BAR;
        }
    }

    public static class Qux extends Bar {

        @Override
        public String bar() {
            return QUX;
        }
    }

    public static class SuperInterceptor {

        public static String intercept(@SuperCall Callable<String> zuper) throws Exception {
            return FOO + zuper.call();
        }
    }
}
