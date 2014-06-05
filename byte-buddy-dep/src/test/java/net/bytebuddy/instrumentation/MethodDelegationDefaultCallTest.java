package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.DefaultCall;
import net.bytebuddy.utility.Java8Rule;
import net.bytebuddy.utility.PrecompiledTypeClassLoader;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDeclaredBy;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.not;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationDefaultCallTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", QUX = "qux";

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";
    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";
    private static final String PREFERRING_INTERCEPTOR = "net.bytebuddy.test.precompiled.SingleDefaultMethodPreferringInterceptor";
    private static final String CONFLICTING_PREFERRING_INTERCEPTOR = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingPreferringInterceptor";

    @Rule
    public MethodRule java8Rule = new Java8Rule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

    @Test
    @Java8Rule.Enforce
    public void testRunnableDefaultCall() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(Object.class,
                MethodDelegation.to(RunnableClass.class),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) QUX));
    }

    @Test
    @Java8Rule.Enforce
    public void testCallableDefaultCall() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(Object.class,
                MethodDelegation.to(CallableClass.class),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @Java8Rule.Enforce
    public void testImplicitAmbiguousDefaultCallCannotBeBound() throws Exception {
        instrument(Object.class,
                MethodDelegation.to(CallableClass.class),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD), classLoader.loadClass(CONFLICTING_INTERFACE));
    }

    @Test
    @Java8Rule.Enforce
    public void testExplicitDefaultCall() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(Object.class,
                MethodDelegation.to(classLoader.loadClass(PREFERRING_INTERCEPTOR)),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD), classLoader.loadClass(CONFLICTING_INTERFACE));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test
    @Java8Rule.Enforce
    public void testExplicitDefaultCallToOtherInterface() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(Object.class,
                MethodDelegation.to(classLoader.loadClass(CONFLICTING_PREFERRING_INTERCEPTOR)),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD), classLoader.loadClass(CONFLICTING_INTERFACE));
        Object instance = loaded.getLoaded().newInstance();
        Method method = loaded.getLoaded().getMethod(FOO);
        assertThat(method.invoke(instance), is((Object) QUX));
    }

    @Test(expected = IllegalStateException.class)
    @Java8Rule.Enforce
    public void testIllegalDefaultCallThrowsException() throws Exception {
        instrument(Object.class,
                MethodDelegation.to(IllegalAnnotation.class),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD));
    }

    @Test
    public void testSerializableProxy() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(Object.class,
                MethodDelegation.to(SerializationCheck.class),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD));
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
