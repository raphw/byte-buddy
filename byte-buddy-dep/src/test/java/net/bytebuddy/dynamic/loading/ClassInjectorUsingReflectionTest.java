package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ClassInjectorUsingReflectionTest {

    private static final String FOO = "foo", BAR = "bar";

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
    public void testInjection() throws Exception {
        new ClassInjector.UsingReflection(classLoader)
                .inject(Collections.<TypeDescription, byte[]>singletonMap(new TypeDescription.ForLoadedType(Foo.class), ClassFileExtraction.extract(Foo.class)));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    public void testDirectInjection() throws Exception {
        ClassInjector.UsingReflection.Dispatcher dispatcher = ClassInjector.UsingReflection.Dispatcher.Direct.make().initialize();
        assertThat(dispatcher.findClass(classLoader, Foo.class.getName()), nullValue(Class.class));
        assertThat(dispatcher.defineClass(classLoader, Foo.class.getName(), ClassFileExtraction.extract(Foo.class), null), notNullValue(Class.class));
        assertThat(dispatcher.getPackage(classLoader, Foo.class.getPackage().getName()), nullValue(Package.class));
        assertThat(dispatcher.definePackage(classLoader,
                Foo.class.getPackage().getName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null), notNullValue(Package.class));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    public void testIndirectInjection() throws Exception {
        ClassInjector.UsingReflection.Dispatcher dispatcher = ClassInjector.UsingReflection.Dispatcher.Indirect.make().initialize();
        assertThat(dispatcher.findClass(classLoader, Foo.class.getName()), nullValue(Class.class));
        assertThat(dispatcher.defineClass(classLoader, Foo.class.getName(), ClassFileExtraction.extract(Foo.class), null), notNullValue(Class.class));
        assertThat(dispatcher.getPackage(classLoader, Foo.class.getPackage().getName()), nullValue(Package.class));
        assertThat(dispatcher.definePackage(classLoader,
                Foo.class.getPackage().getName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null), notNullValue(Package.class));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    public void testDispatcherFaultyInitializationGetClass() throws Exception {
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Unavailable(new Exception()).initialize().findClass(getClass().getClassLoader(),
                Object.class.getName()), is((Object) Object.class));
    }

    @Test
    public void testDispatcherFaultyInitializationGetClassInexistant() throws Exception {
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Unavailable(new Exception()).initialize().findClass(getClass().getClassLoader(),
                FOO), nullValue(Class.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyInitializationDefineClass() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Unavailable(new Exception()).initialize().defineClass(null,
                null,
                null,
                null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyInitializationGetPackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Unavailable(new Exception()).initialize().getPackage(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyInitializationDefinePackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Unavailable(new Exception()).initialize().definePackage(null,
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
                null).getClassLoadingLock(classLoader, FOO), is((Object) classLoader));
    }

    @Test
    public void testFaultyDispatcherGetLock() throws Exception {
        ClassLoader classLoader = mock(ClassLoader.class);
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Unavailable(null).getClassLoadingLock(classLoader, FOO), is((Object) classLoader));
    }

    @Test
    public void testInjectionOrderNoPrematureAuxiliaryInjection() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(Bar.class, Interceptor.class),
                null,
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> type = new ByteBuddy().rebase(Bar.class)
                .method(named(BAR))
                .intercept(MethodDelegation.to(Interceptor.class)).make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test
    public void testAvailability() throws Exception {
        assertThat(ClassInjector.UsingReflection.isAvailable(), is(true));
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Unavailable(null).isAvailable(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.class).apply();
        final Iterator<Method> methods = Arrays.asList(String.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.Direct.ForLegacyVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.Direct.ForJava7CapableVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.Indirect.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.Unavailable.class).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.Unavailable.class).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.CreationAction.class).apply();
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
