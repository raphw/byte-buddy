package net.bytebuddy.dynamic;

import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NexusTest {

    private static final String FOO = "foo";

    private static final int BAR = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private DynamicType.Builder<?> builder;

    @Mock
    private DynamicType dynamicType;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Mock
    private ClassLoader classLoader;

    @Test
    public void testNexusIsPublic() throws Exception {
        assertThat(Modifier.isPublic(Nexus.class.getModifiers()), is(true));
    }

    @Test
    public void testNexusHasNoDeclaringType() throws Exception {
        assertThat(Nexus.class.getDeclaringClass(), nullValue(Class.class));
    }

    @Test
    public void testNexusHasNoDeclaredTypes() throws Exception {
        assertThat(Nexus.class.getDeclaredClasses().length, is(0));
    }

    @Test
    public void testNexusAccessorNonActive() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Nexus.class,
                        NexusAccessor.class,
                        NexusAccessor.Dispatcher.class,
                        NexusAccessor.Dispatcher.Available.class,
                        NexusAccessor.Dispatcher.Unavailable.class),
                null,
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Field duplicateInitializers = classLoader.loadClass(Nexus.class.getName()).getDeclaredField("TYPE_INITIALIZERS");
        duplicateInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
        Field actualInitializers = Nexus.class.getDeclaredField("TYPE_INITIALIZERS");
        actualInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(0));
        Class<?> accessor = classLoader.loadClass(NexusAccessor.class.getName());
        ClassLoader qux = mock(ClassLoader.class);
        when(loadedTypeInitializer.isAlive()).thenReturn(false);
        assertThat(accessor
                .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, LoadedTypeInitializer.class)
                .invoke(accessor.getEnumConstants()[0], FOO, qux, BAR, loadedTypeInitializer), nullValue(Object.class));
        try {
            assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
            assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(0));
        } finally {
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) actualInitializers.get(null)).remove(constructor.newInstance(FOO, qux, BAR));
            assertThat(value, nullValue());
        }
    }

    @Test
    public void testNexusAccessorClassLoaderBoundary() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Nexus.class,
                        NexusAccessor.class,
                        NexusAccessor.Dispatcher.class,
                        NexusAccessor.Dispatcher.Available.class,
                        NexusAccessor.Dispatcher.Unavailable.class),
                null,
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Field duplicateInitializers = classLoader.loadClass(Nexus.class.getName()).getDeclaredField("TYPE_INITIALIZERS");
        duplicateInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
        Field actualInitializers = Nexus.class.getDeclaredField("TYPE_INITIALIZERS");
        actualInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(0));
        Class<?> accessor = classLoader.loadClass(NexusAccessor.class.getName());
        ClassLoader qux = mock(ClassLoader.class);
        when(loadedTypeInitializer.isAlive()).thenReturn(true);
        assertThat(accessor
                .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, LoadedTypeInitializer.class)
                .invoke(accessor.getEnumConstants()[0], FOO, qux, BAR, loadedTypeInitializer), nullValue(Object.class));
        try {
            assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
            assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(1));
        } finally {
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) actualInitializers.get(null)).remove(constructor.newInstance(FOO, qux, BAR));
            assertThat(value, is((Object) loadedTypeInitializer));
        }
    }

    @Test
    public void testNexusAccessorClassLoaderNoResource() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Nexus.class,
                        NexusAccessor.class,
                        NexusAccessor.Dispatcher.class,
                        NexusAccessor.Dispatcher.Available.class,
                        NexusAccessor.Dispatcher.Unavailable.class),
                null,
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Field duplicateInitializers = classLoader.loadClass(Nexus.class.getName()).getDeclaredField("TYPE_INITIALIZERS");
        duplicateInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
        Field actualInitializers = Nexus.class.getDeclaredField("TYPE_INITIALIZERS");
        actualInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(0));
        Class<?> accessor = classLoader.loadClass(NexusAccessor.class.getName());
        ClassLoader qux = mock(ClassLoader.class);
        when(loadedTypeInitializer.isAlive()).thenReturn(true);
        assertThat(accessor
                .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, LoadedTypeInitializer.class)
                .invoke(accessor.getEnumConstants()[0], FOO, qux, BAR, loadedTypeInitializer), nullValue(Object.class));
        try {
            assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
            assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(1));
        } finally {
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) actualInitializers.get(null)).remove(constructor.newInstance(FOO, qux, BAR));
            assertThat(value, is((Object) loadedTypeInitializer));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testUnavailableDispatcherThrowsException() throws Exception {
        new NexusAccessor.Dispatcher.Unavailable(new Exception()).register(FOO, classLoader, BAR, loadedTypeInitializer);
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Class<?>> types = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(Nexus.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return types.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(NexusAccessor.class).apply();
        final Iterator<Method> methods = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(NexusAccessor.Dispatcher.Available.class)
                .create(new ObjectPropertyAssertion.Creator<Method>() {
                    @Override
                    public Method create() {
                        return methods.next();
                    }
                }).apply();
        ObjectPropertyAssertion.of(NexusAccessor.Dispatcher.Unavailable.class).apply();
        ObjectPropertyAssertion.of(NexusAccessor.InitializationAppender.class).apply();
    }
}
