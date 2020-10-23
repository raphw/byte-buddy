package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.test.utility.LegacyGetPackageClassLoader;
import net.bytebuddy.test.utility.ClassReflectionInjectionAvailableRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ClassInjectorUsingReflectionTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule classInjectionAvailableRule = new ClassReflectionInjectionAvailableRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBootstrapClassLoader() throws Exception {
        new ClassInjector.UsingReflection(ClassLoadingStrategy.BOOTSTRAP_LOADER);
    }

    @Test
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testInjection() throws Exception {
        new ClassInjector.UsingReflection(classLoader).inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(Foo.class),
                        ClassFileLocator.ForClassLoader.read(Foo.class)));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testInjectionOnLegacyClassloader() throws Exception {
        ClassLoader classLoader = new LegacyGetPackageClassLoader();
        new ClassInjector.UsingReflection(classLoader.getParent()).inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(Foo.class),
            ClassFileLocator.ForClassLoader.read(Foo.class)));
        new ClassInjector.UsingReflection(classLoader).inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(Foo.class),
            ClassFileLocator.ForClassLoader.read(Foo.class)));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    @ClassReflectionInjectionAvailableRule.Enforce
    @JavaVersionRule.Enforce(atMost = 8)
    public void testDirectInjection() throws Exception {
        ClassInjector.UsingReflection.Dispatcher dispatcher = ClassInjector.UsingReflection.Dispatcher.Direct.make().initialize();
        assertThat(dispatcher.getDefinedPackage(classLoader, Foo.class.getPackage().getName()), nullValue(Package.class));
        assertThat(dispatcher.definePackage(classLoader,
                Foo.class.getPackage().getName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null), notNullValue(Package.class));
        assertThat(dispatcher.findClass(classLoader, Foo.class.getName()), nullValue(Class.class));
        assertThat(dispatcher.defineClass(classLoader, Foo.class.getName(), ClassFileLocator.ForClassLoader.read(Foo.class), null), notNullValue(Class.class));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    @ClassReflectionInjectionAvailableRule.Enforce
    @JavaVersionRule.Enforce(atMost = 10)
    public void testUnsafeInjection() throws Exception {
        ClassInjector.UsingReflection.Dispatcher dispatcher = ClassInjector.UsingReflection.Dispatcher.UsingUnsafeInjection.make().initialize();
        assertThat(dispatcher.getDefinedPackage(classLoader, Foo.class.getPackage().getName()), nullValue(Package.class));
        assertThat(dispatcher.definePackage(classLoader,
                Foo.class.getPackage().getName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null), notNullValue(Package.class));
        assertThat(dispatcher.findClass(classLoader, Foo.class.getName()), nullValue(Class.class));
        assertThat(dispatcher.defineClass(classLoader, Foo.class.getName(), ClassFileLocator.ForClassLoader.read(Foo.class), null), notNullValue(Class.class));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testUnsafeOverride() throws Exception {
        ClassInjector.UsingReflection.Dispatcher dispatcher = ClassInjector.UsingReflection.Dispatcher.UsingUnsafeOverride.make().initialize();
        assertThat(dispatcher.getDefinedPackage(classLoader, Foo.class.getPackage().getName()), nullValue(Package.class));
        assertThat(dispatcher.definePackage(classLoader,
                Foo.class.getPackage().getName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null), notNullValue(Package.class));
        assertThat(dispatcher.findClass(classLoader, Foo.class.getName()), nullValue(Class.class));
        assertThat(dispatcher.defineClass(classLoader, Foo.class.getName(), ClassFileLocator.ForClassLoader.read(Foo.class), null), notNullValue(Class.class));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    public void testDispatcherFaultyInitializationGetClass() throws Exception {
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Initializable.Unavailable("foo").initialize().findClass(getClass().getClassLoader(),
                Object.class.getName()), is((Object) Object.class));
    }

    @Test
    public void testDispatcherFaultyInitializationGetClassInexistent() throws Exception {
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Initializable.Unavailable("foo").initialize().findClass(getClass().getClassLoader(),
                FOO), nullValue(Class.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyInitializationDefineClass() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Initializable.Unavailable("foo").initialize().defineClass(null,
                null,
                null,
                null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyInitializationGetPackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Initializable.Unavailable("foo").initialize().getDefinedPackage(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyInitializationDefinePackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Initializable.Unavailable("foo").initialize().definePackage(null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyDispatcherDefineClass() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Unavailable(FOO).defineClass(null,
                null,
                null,
                null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyDispatcherGetPackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Unavailable(FOO).getDefinedPackage(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyDispatcherDefinePackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Unavailable(FOO).definePackage(null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    public void testLegacyDispatcherGetLock() throws Exception {
        ClassLoader classLoader = mock(ClassLoader.class);
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Direct.ForLegacyVm(null,
                null,
                null,
                null,
                null).getClassLoadingLock(classLoader, FOO), is((Object) classLoader));
    }

    @Test
    public void testFaultyDispatcherGetLock() throws Exception {
        ClassLoader classLoader = mock(ClassLoader.class);
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Unavailable(null).getClassLoadingLock(classLoader, FOO), is((Object) classLoader));
    }

    @Test
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testInjectionOrderNoPrematureAuxiliaryInjection() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileLocator.ForClassLoader.readToNames(Bar.class, Interceptor.class));
        Class<?> type = new ByteBuddy()
                .rebase(Bar.class)
                .method(named(BAR))
                .intercept(MethodDelegation.to(Interceptor.class)).make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testAvailability() throws Exception {
        assertThat(ClassInjector.UsingReflection.isAvailable(), is(true));
        assertThat(new ClassInjector.UsingReflection(ClassLoader.getSystemClassLoader()).isAlive(), is(true));
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Initializable.Unavailable(null).isAvailable(), is(false));
    }

    private static class Foo {
        /* Note: Foo is know to the system class loader but not to the bootstrap class loader */
    }

    public static class Bar {

        public String bar(String value) {
            return value;
        }
    }

    public static class Interceptor {

        @RuntimeType
        public static Object intercept(@Super(proxyType = TargetType.class) Object zuper,
                                       @AllArguments Object[] args,
                                       @Origin Method method) throws Throwable {
            args[0] = BAR;
            return method.invoke(zuper, args);
        }
    }
}
