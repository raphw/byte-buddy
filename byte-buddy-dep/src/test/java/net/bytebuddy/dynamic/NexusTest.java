package net.bytebuddy.dynamic;

import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
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
                        NexusAccessor.Dispatcher.CreationAction.class,
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
                .invoke(accessor.getDeclaredConstructor().newInstance(), FOO, qux, BAR, loadedTypeInitializer), nullValue(Object.class));
        try {
            assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
            assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(0));
        } finally {
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, ReferenceQueue.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) actualInitializers.get(null)).remove(constructor.newInstance(FOO, qux, Nexus.NO_QUEUE, BAR));
            assertThat(value, nullValue());
        }
    }

    @Test
    public void testNexusAccessorClassLoaderBoundary() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Nexus.class,
                        NexusAccessor.class,
                        NexusAccessor.Dispatcher.class,
                        NexusAccessor.Dispatcher.CreationAction.class,
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
                .invoke(accessor.getDeclaredConstructor().newInstance(), FOO, qux, BAR, loadedTypeInitializer), nullValue(Object.class));
        try {
            assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
            assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(1));
        } finally {
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, ReferenceQueue.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) actualInitializers.get(null)).remove(constructor.newInstance(FOO, qux, Nexus.NO_QUEUE, BAR));
            assertThat(value, is((Object) loadedTypeInitializer));
        }
    }

    @Test
    public void testNexusAccessorClassLoaderNoResource() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Nexus.class,
                        NexusAccessor.class,
                        NexusAccessor.Dispatcher.class,
                        NexusAccessor.Dispatcher.CreationAction.class,
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
                .invoke(accessor.getDeclaredConstructor().newInstance(), FOO, qux, BAR, loadedTypeInitializer), nullValue(Object.class));
        try {
            assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
            assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(1));
        } finally {
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, ReferenceQueue.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) actualInitializers.get(null)).remove(constructor.newInstance(FOO, qux, Nexus.NO_QUEUE, BAR));
            assertThat(value, is((Object) loadedTypeInitializer));
        }
    }

    @Test
    public void testNexusClean() throws Exception {
        Field typeInitializers = ClassLoader.getSystemClassLoader().loadClass(Nexus.class.getName()).getDeclaredField("TYPE_INITIALIZERS");
        typeInitializers.setAccessible(true);
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        when(loadedTypeInitializer.isAlive()).thenReturn(true);
        assertThat(((Map<?, ?>) typeInitializers.get(null)).isEmpty(), is(true));
        ReferenceQueue<ClassLoader> referenceQueue = new ReferenceQueue<ClassLoader>();
        NexusAccessor nexusAccessor = new NexusAccessor(referenceQueue);
        nexusAccessor.register(FOO, classLoader, BAR, loadedTypeInitializer);
        assertThat(((Map<?, ?>) typeInitializers.get(null)).isEmpty(), is(false));
        classLoader = null;
        System.gc();
        NexusAccessor.clean(referenceQueue.remove(100L));
        assertThat(((Map<?, ?>) typeInitializers.get(null)).isEmpty(), is(true));
    }

    @Test
    public void testNexusAccessorIsAvailable() throws Exception {
        assertThat(NexusAccessor.isAlive(), is(true));
    }

    @Test
    public void testUnavailableState() throws Exception {
        assertThat(new NexusAccessor.Dispatcher.Unavailable(new Exception()).isAlive(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testUnavailableDispatcherRegisterThrowsException() throws Exception {
        new NexusAccessor.Dispatcher.Unavailable(new Exception()).register(FOO, classLoader, Nexus.NO_QUEUE, BAR, loadedTypeInitializer);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testUnavailableDispatcherCleanThrowsException() throws Exception {
        new NexusAccessor.Dispatcher.Unavailable(new Exception()).clean(mock(Reference.class));
    }

    @Test
    public void testNexusEquality() throws Exception {
        Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, ReferenceQueue.class, int.class);
        constructor.setAccessible(true);
        assertThat(constructor.newInstance(FOO, classLoader, Nexus.NO_QUEUE, BAR),
                is(constructor.newInstance(FOO, classLoader, Nexus.NO_QUEUE, BAR)));
        assertThat(constructor.newInstance(FOO, classLoader, Nexus.NO_QUEUE, BAR).hashCode(),
                is(constructor.newInstance(FOO, classLoader, Nexus.NO_QUEUE, BAR).hashCode()));

    }
}
