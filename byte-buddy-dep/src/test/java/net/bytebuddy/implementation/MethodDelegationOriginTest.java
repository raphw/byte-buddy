package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationOriginTest extends AbstractImplementationTest {

    private static final String FOO = "foo", TYPE = "TYPE";

    private static final String ORIGIN_METHOD_HANDLE = "net.bytebuddy.test.precompiled.OriginMethodHandle";

    private static final String ORIGIN_METHOD_TYPE = "net.bytebuddy.test.precompiled.OriginMethodType";

    private static final String ORIGIN_EXECUTABLE = "net.bytebuddy.test.precompiled.OriginExecutable";

    private static final String ORIGIN_EXECUTABLE_CACHED = "net.bytebuddy.test.precompiled.OriginExecutableWithCache";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testOriginClass() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(OriginClass.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf(Class.class));
        assertThat(instance.foo(), is((Object) Foo.class));
    }

    @Test
    public void testOriginMethodWithoutCache() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(OriginMethod.class));
        Foo instance = loaded.getLoaded().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, not(sameInstance(instance.foo())));
    }

    @Test
    public void testOriginMethodWithCache() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(OriginMethodWithCache.class));
        Foo instance = loaded.getLoaded().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, sameInstance(instance.foo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOriginConstructorWithoutCache() throws Exception {
        OriginConstructor originConstructor = new OriginConstructor();
        DynamicType.Loaded<Foo> loaded = implement(Foo.class,
                SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(originConstructor)),
                getClass().getClassLoader(),
                isConstructor());
        loaded.getLoaded().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, is((Constructor) loaded.getLoaded().getDeclaredConstructor()));
        Constructor<?> previous = originConstructor.constructor;
        loaded.getLoaded().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, is((Constructor) loaded.getLoaded().getDeclaredConstructor()));
        assertThat(originConstructor.constructor, not(sameInstance((Constructor) previous)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOriginConstructorWithCache() throws Exception {
        OriginConstructorWithCache originConstructor = new OriginConstructorWithCache();
        DynamicType.Loaded<Foo> loaded = implement(Foo.class,
                SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(originConstructor)),
                getClass().getClassLoader(),
                isConstructor());
        loaded.getLoaded().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, is((Constructor) loaded.getLoaded().getDeclaredConstructor()));
        Constructor<?> previous = originConstructor.constructor;
        loaded.getLoaded().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, sameInstance((Constructor) previous));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testOriginExecutableOnMethodWithoutCache() throws Exception {
        Object origin = Class.forName(ORIGIN_EXECUTABLE).newInstance();
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(origin));
        Foo instance = loaded.getLoaded().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, not(sameInstance(instance.foo())));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testOriginExecutableOnMethodWithCache() throws Exception {
        Object origin = Class.forName(ORIGIN_EXECUTABLE_CACHED).newInstance();
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(origin));
        Foo instance = loaded.getLoaded().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, sameInstance(instance.foo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testOriginExecutableConstructorWithoutCache() throws Exception {
        Object originConstructor = Class.forName(ORIGIN_EXECUTABLE).newInstance();
        Field constructor = Class.forName(ORIGIN_EXECUTABLE).getDeclaredField("executable");
        DynamicType.Loaded<Foo> loaded = implement(Foo.class,
                SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(originConstructor)),
                getClass().getClassLoader(),
                isConstructor());
        loaded.getLoaded().newInstance();
        assertThat(constructor.get(originConstructor), instanceOf(Constructor.class));
        assertThat(constructor.get(originConstructor), is((Object) loaded.getLoaded().getDeclaredConstructor()));
        Object previous = constructor.get(originConstructor);
        loaded.getLoaded().newInstance();
        assertThat(constructor.get(originConstructor), instanceOf(Constructor.class));
        assertThat(constructor.get(originConstructor), is((Object) loaded.getLoaded().getDeclaredConstructor()));
        assertThat(constructor.get(originConstructor), not(sameInstance(previous)));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testOriginExecutableConstructorWithCache() throws Exception {
        Object originConstructor = Class.forName(ORIGIN_EXECUTABLE_CACHED).newInstance();
        Field constructor = Class.forName(ORIGIN_EXECUTABLE_CACHED).getDeclaredField("executable");
        DynamicType.Loaded<Foo> loaded = implement(Foo.class,
                SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(originConstructor)),
                getClass().getClassLoader(),
                isConstructor());
        loaded.getLoaded().newInstance();
        assertThat(constructor.get(originConstructor), instanceOf(Constructor.class));
        assertThat(constructor.get(originConstructor), is((Object) loaded.getLoaded().getDeclaredConstructor()));
        Object previous = constructor.get(originConstructor);
        loaded.getLoaded().newInstance();
        assertThat(constructor.get(originConstructor), instanceOf(Constructor.class));
        assertThat(constructor.get(originConstructor), sameInstance(previous));
    }

    @Test
    public void testOriginString() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(OriginString.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf(String.class));
        assertThat(instance.foo(), is((Object) Foo.class.getDeclaredMethod(FOO).toString()));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginMethodHandle() throws Throwable {
        Class<?> originMethodHandle = Class.forName(ORIGIN_METHOD_HANDLE);
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(originMethodHandle));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf((Class<?>) originMethodHandle.getDeclaredField(TYPE).get(null)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginMethodType() throws Throwable {
        Class<?> originMethodType = Class.forName(ORIGIN_METHOD_TYPE);
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(originMethodType));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf((Class<?>) originMethodType.getDeclaredField(TYPE).get(null)));
    }

    @Test(expected = IllegalStateException.class)
    public void testOriginIllegal() throws Exception {
        implement(Foo.class, MethodDelegation.to(OriginIllegal.class));
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

        public static Object foo(@Origin(cache = false) Method method) {
            return method;
        }
    }

    public static class OriginMethodWithCache {

        public static Object foo(@Origin(cache = true) Method method) {
            return method;
        }
    }

    public static class OriginConstructor {

        private Constructor<?> constructor;

        public void foo(@Origin(cache = false) Constructor<?> constructor) {
            this.constructor = constructor;
        }
    }

    public static class OriginConstructorWithCache {

        private Constructor<?> constructor;

        public void foo(@Origin(cache = true) Constructor<?> constructor) {
            this.constructor = constructor;
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
