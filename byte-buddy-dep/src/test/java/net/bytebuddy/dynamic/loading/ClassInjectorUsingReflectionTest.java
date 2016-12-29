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
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ClassInjectorUsingReflectionTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassInjector mockInjector;

    @Mock
    private TypeDescription typeDescription;

    private ClassLoader classLoader;

    private ClassInjector classInjector;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null);
        classInjector = new ClassInjector.UsingReflection(classLoader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBootstrapClassLoader() throws Exception {
        new ClassInjector.UsingReflection(ClassLoadingStrategy.BOOTSTRAP_LOADER);
    }

    @Test
    public void testInjection() throws Exception {
        classInjector.inject(Collections.<TypeDescription, byte[]>singletonMap(new TypeDescription.ForLoadedType(Foo.class), ClassFileExtraction.extract(Foo.class)));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    public void testDispatcherFaultyInitializationGetClass() throws Exception {
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Faulty(new Exception()).initialize().findClass(getClass().getClassLoader(),
                Object.class.getName()), is((Object) Object.class));
    }

    @Test
    public void testDispatcherFaultyInitializationGetClassInexistant() throws Exception {
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Faulty(new Exception()).initialize().findClass(getClass().getClassLoader(),
                FOO), nullValue(Class.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyInitializationDefineClass() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Faulty(new Exception()).initialize().defineClass(null,
                null,
                null,
                null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyInitializationGetPackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Faulty(new Exception()).initialize().getPackage(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDispatcherFaultyInitializationDefinePackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Faulty(new Exception()).initialize().definePackage(null,
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
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Resolved.ForLegacyVm(null,
                null,
                null,
                null,
                null,
                null).getClassLoadingLock(classLoader, FOO), is((Object) classLoader));
    }

    @Test
    public void testFaultyDispatcherGetLock() throws Exception {
        ClassLoader classLoader = mock(ClassLoader.class);
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Faulty(null).getClassLoadingLock(classLoader, FOO), is((Object) classLoader));
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
    public void testUnsafeDispatcher() throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafe.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Method defineClass = unsafe.getDeclaredMethod("defineClass",
                String.class,
                byte[].class,
                int.class,
                int.class,
                ClassLoader.class,
                ProtectionDomain.class);
        ClassInjector.UsingReflection.Dispatcher dispatcher = new ClassInjector.UsingReflection.Dispatcher.Resolved.UnsafeDispatcher(theUnsafe.get(null),
                defineClass);
        ClassLoader classLoader = new URLClassLoader(new URL[0], null);
        assertThat(dispatcher.findClass(classLoader, Bar.class.getName()), nullValue(Class.class));
        Class<?> type = dispatcher.defineClass(classLoader, Bar.class.getName(), ClassFileExtraction.extract(Bar.class), null);
        assertThat(dispatcher.findClass(classLoader, Bar.class.getName()), is((Object) type));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsafeDispatcherGetPackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Resolved.UnsafeDispatcher(null, null).getPackage(null, FOO);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsafeDispatcherDefinePackage() throws Exception {
        new ClassInjector.UsingReflection.Dispatcher.Resolved.UnsafeDispatcher(null, null).definePackage(null, FOO, null, null, null, null, null, null, null);
    }

    @Test
    public void testUnsafeDispatcherGetLock() throws Exception {
        ClassLoader classLoader = mock(ClassLoader.class);
        assertThat(new ClassInjector.UsingReflection.Dispatcher.Resolved.UnsafeDispatcher(null, null).getClassLoadingLock(classLoader, FOO), is((Object) classLoader));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.class).apply();
        final Iterator<Field> fields = Arrays.asList(String.class.getDeclaredFields()).iterator();
        final Iterator<Method> methods = Arrays.asList(String.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.Resolved.ForLegacyVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).create(new ObjectPropertyAssertion.Creator<Field>() {
            @Override
            public Field create() {
                return fields.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.Resolved.ForJava7CapableVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).create(new ObjectPropertyAssertion.Creator<Field>() {
            @Override
            public Field create() {
                return fields.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.Resolved.UnsafeDispatcher.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.Dispatcher.Faulty.class).apply();
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
