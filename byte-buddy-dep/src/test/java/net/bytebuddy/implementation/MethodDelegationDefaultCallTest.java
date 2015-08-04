package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.DefaultCall;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationDefaultCallTest extends AbstractImplementationTest {

    private static final String FOO = "foo", QUX = "qux";

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";

    private static final String PREFERRING_INTERCEPTOR = "net.bytebuddy.test.precompiled.SingleDefaultMethodPreferringInterceptor";

    private static final String CONFLICTING_PREFERRING_INTERCEPTOR = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingPreferringInterceptor";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(8)
    public void testRunnableDefaultCall() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                MethodDelegation.to(RunnableClass.class),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(SINGLE_DEFAULT_METHOD));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) QUX));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testCallableDefaultCall() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                MethodDelegation.to(CallableClass.class),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(SINGLE_DEFAULT_METHOD));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @JavaVersionRule.Enforce(8)
    public void testImplicitAmbiguousDefaultCallCannotBeBound() throws Exception {
        implement(Object.class,
                MethodDelegation.to(CallableClass.class),
                getClass().getClassLoader(),
                not(isDeclaredBy(Object.class)),
                Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testExplicitDefaultCall() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                MethodDelegation.to(Class.forName(PREFERRING_INTERCEPTOR)),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testExplicitDefaultCallToOtherInterface() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                MethodDelegation.to(Class.forName(CONFLICTING_PREFERRING_INTERCEPTOR)),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) QUX));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testIllegalDefaultCallThrowsException() throws Exception {
        implement(Object.class,
                MethodDelegation.to(IllegalAnnotation.class),
                getClass().getClassLoader(),
                not(isDeclaredBy(Object.class)),
                Class.forName(SINGLE_DEFAULT_METHOD));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testSerializableProxy() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                MethodDelegation.to(SerializationCheck.class),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(SINGLE_DEFAULT_METHOD));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    public static class RunnableClass {

        public static Object foo(@DefaultCall Runnable runnable) {
            assertThat(runnable, CoreMatchers.not(instanceOf(Serializable.class)));
            runnable.run();
            return QUX;
        }
    }

    public static class CallableClass {

        public static String bar(@DefaultCall Callable<String> callable) throws Exception {
            assertThat(callable, CoreMatchers.not(instanceOf(Serializable.class)));
            return callable.call();
        }
    }

    public static class IllegalAnnotation {

        public static String bar(@DefaultCall String value) throws Exception {
            return value;
        }
    }

    public static class SerializationCheck {

        public static String bar(@DefaultCall(serializableProxy = true) Callable<String> callable) throws Exception {
            assertThat(callable, instanceOf(Serializable.class));
            return callable.call();
        }
    }
}
