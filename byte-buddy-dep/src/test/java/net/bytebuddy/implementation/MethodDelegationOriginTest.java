package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationOriginTest {

    private static final String FOO = "foo", TYPE = "TYPE";

    private static final String ORIGIN_METHOD_HANDLE = "net.bytebuddy.test.precompiled.OriginMethodHandle";

    private static final String ORIGIN_METHOD_TYPE = "net.bytebuddy.test.precompiled.OriginMethodType";

    private static final String ORIGIN_EXECUTABLE = "net.bytebuddy.test.precompiled.OriginExecutable";

    private static final String ORIGIN_EXECUTABLE_CACHED = "net.bytebuddy.test.precompiled.OriginExecutableWithCache";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testOriginClass() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(OriginClass.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), instanceOf(Class.class));
        assertThat(instance.foo(), is((Object) Foo.class));
    }

    @Test
    public void testOriginMethodWithoutCache() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(OriginMethod.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, not(sameInstance(instance.foo())));
    }

    @Test
    public void testOriginMethodWithCache() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(OriginMethodWithCache.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, sameInstance(instance.foo()));
    }

    @Test
    public void testOriginMethodWithPrivilege() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(OriginMethodWithPrivilege.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(1));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, sameInstance(instance.foo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOriginConstructorWithoutCache() throws Exception {
        OriginConstructor originConstructor = new OriginConstructor();
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .constructor(ElementMatchers.any())
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(originConstructor)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, is((Constructor) loaded.getLoaded().getDeclaredConstructor()));
        Constructor<?> previous = originConstructor.constructor;
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, is((Constructor) loaded.getLoaded().getDeclaredConstructor()));
        assertThat(originConstructor.constructor, not(sameInstance((Constructor) previous)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOriginConstructorWithCache() throws Exception {
        OriginConstructorWithCache originConstructor = new OriginConstructorWithCache();
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .constructor(ElementMatchers.any())
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(originConstructor)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, is((Constructor) loaded.getLoaded().getDeclaredConstructor()));
        Constructor<?> previous = originConstructor.constructor;
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, sameInstance((Constructor) previous));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOriginConstructorWithPrivilege() throws Exception {
        OriginConstructorWithPrivilege originConstructor = new OriginConstructorWithPrivilege();
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .constructor(ElementMatchers.any())
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(originConstructor)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(1));
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, is((Constructor) loaded.getLoaded().getDeclaredConstructor()));
        Constructor<?> previous = originConstructor.constructor;
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(originConstructor.constructor, instanceOf(Constructor.class));
        assertThat(originConstructor.constructor, sameInstance((Constructor) previous));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testOriginExecutableOnMethodWithoutCache() throws Exception {
        Object origin = Class.forName(ORIGIN_EXECUTABLE).getDeclaredConstructor().newInstance();
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(origin))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, not(sameInstance(instance.foo())));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testOriginExecutableOnMethodWithCache() throws Exception {
        Object origin = Class.forName(ORIGIN_EXECUTABLE_CACHED).getDeclaredConstructor().newInstance();
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(origin))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Object method = instance.foo();
        assertThat(method, instanceOf(Method.class));
        assertThat(method, is((Object) Foo.class.getDeclaredMethod(FOO)));
        assertThat(method, sameInstance(instance.foo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testOriginExecutableConstructorWithoutCache() throws Exception {
        Object originConstructor = Class.forName(ORIGIN_EXECUTABLE).getDeclaredConstructor().newInstance();
        Field constructor = Class.forName(ORIGIN_EXECUTABLE).getDeclaredField("executable");
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .constructor(ElementMatchers.any())
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(originConstructor)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(constructor.get(originConstructor), instanceOf(Constructor.class));
        assertThat(constructor.get(originConstructor), is((Object) loaded.getLoaded().getDeclaredConstructor()));
        Object previous = constructor.get(originConstructor);
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(constructor.get(originConstructor), instanceOf(Constructor.class));
        assertThat(constructor.get(originConstructor), is((Object) loaded.getLoaded().getDeclaredConstructor()));
        assertThat(constructor.get(originConstructor), not(sameInstance(previous)));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testOriginExecutableConstructorWithCache() throws Exception {
        Object originConstructor = Class.forName(ORIGIN_EXECUTABLE_CACHED).getDeclaredConstructor().newInstance();
        Field constructor = Class.forName(ORIGIN_EXECUTABLE_CACHED).getDeclaredField("executable");
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .constructor(ElementMatchers.any())
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(originConstructor)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(constructor.get(originConstructor), instanceOf(Constructor.class));
        assertThat(constructor.get(originConstructor), is((Object) loaded.getLoaded().getDeclaredConstructor()));
        Object previous = constructor.get(originConstructor);
        loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(constructor.get(originConstructor), instanceOf(Constructor.class));
        assertThat(constructor.get(originConstructor), sameInstance(previous));
    }

    @Test
    public void testOriginString() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(OriginString.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), instanceOf(String.class));
        assertThat(instance.foo(), is((Object) Foo.class.getDeclaredMethod(FOO).toString()));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginMethodHandle() throws Throwable {
        Class<?> originMethodHandle = Class.forName(ORIGIN_METHOD_HANDLE);
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(originMethodHandle))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), instanceOf((Class<?>) originMethodHandle.getDeclaredField(TYPE).get(null)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginMethodType() throws Throwable {
        Class<?> originMethodType = Class.forName(ORIGIN_METHOD_TYPE);
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(originMethodType))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), instanceOf((Class<?>) originMethodType.getDeclaredField(TYPE).get(null)));
    }

    @Test(expected = IllegalStateException.class)
    public void testOriginIllegal() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(OriginIllegal.class))
                .make();
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

        public static Object foo(@Origin Method method) {
            return method;
        }
    }

    public static class OriginMethodWithPrivilege {

        public static Object foo(@Origin(privileged = true) Method method) {
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

        public void foo(@Origin Constructor<?> constructor) {
            this.constructor = constructor;
        }
    }

    public static class OriginConstructorWithPrivilege {

        private Constructor<?> constructor;

        public void foo(@Origin(privileged = true) Constructor<?> constructor) {
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
