package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Origin;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;

public class MethodDelegationOriginTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", TYPE = "TYPE";

    private static final String ORIGIN_METHOD_HANDLE = "net.bytebuddy.test.precompiled.OriginMethodHandle";

    private static final String ORIGIN_METHOD_TYPE = "net.bytebuddy.test.precompiled.OriginMethodType";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

    @Test
    public void testOriginClass() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(OriginClass.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf(Class.class));
        assertEquals(Foo.class, instance.foo());
    }

    @Test
    public void testOriginMethodWithoutCache() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(OriginMethod.class));
        Foo instance = loaded.getLoaded().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, not(sameInstance(instance.foo())));
    }

    @Test
    public void testOriginMethodWithCache() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(OriginMethodWithCache.class));
        Foo instance = loaded.getLoaded().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, sameInstance(instance.foo()));
    }

    @Test
    public void testOriginString() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(OriginString.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf(String.class));
        assertThat(instance.foo(),
                is((Object) new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO)).getUniqueSignature()));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginMethodHandle() throws Throwable {
        Class<?> originMethodHandle = classLoader.loadClass(ORIGIN_METHOD_HANDLE);
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(originMethodHandle));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf((Class<?>) originMethodHandle.getDeclaredField(TYPE).get(null)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginMethodType() throws Throwable {
        Class<?> originMethodType = classLoader.loadClass(ORIGIN_METHOD_TYPE);
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(originMethodType));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf((Class<?>) originMethodType.getDeclaredField(TYPE).get(null)));
    }

    @Test(expected = IllegalStateException.class)
    public void testOriginIllegal() throws Exception {
        instrument(Foo.class, MethodDelegation.to(OriginIllegal.class));
    }

    public static class Foo {

        public Object foo() {
            return null;
        }
    }

    public static class OriginClass {

        public static Object foo(@Origin Class<?> type) {
            return type;
        }
    }

    public static class OriginMethod {

        public static Object foo(@Origin Method method) {
            return method;
        }
    }

    public static class OriginMethodWithCache {

        public static Object foo(@Origin(cacheMethod = true) Method method) {
            return method;
        }
    }

    public static class OriginString {

        public static Object foo(@Origin String string) {
            return string;
        }
    }

    public static class OriginIllegal {

        public static Object foo(@Origin Object object) {
            return object;
        }
    }
}
