package net.bytebuddy;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeCacheTest {

    @Test
    public void testCache() throws Exception {
        TypeCache<Object> typeCache = new TypeCache<Object>(TypeCache.Sort.WEAK);
        Object key = new Object();
        assertThat(typeCache.find(ClassLoader.getSystemClassLoader(), key), nullValue(Class.class));
        assertThat(typeCache.insert(ClassLoader.getSystemClassLoader(), key, Void.class), is((Object) Void.class));
        assertThat(typeCache.find(ClassLoader.getSystemClassLoader(), key), is((Object) Void.class));
        assertThat(typeCache.find(mock(ClassLoader.class), key), nullValue(Class.class));
        typeCache.clear();
        assertThat(typeCache.find(ClassLoader.getSystemClassLoader(), key), nullValue(Class.class));
    }

    @Test
    public void testCacheInline() throws Exception {
        TypeCache<Object> typeCache = new TypeCache.WithInlineExpunction<Object>(TypeCache.Sort.WEAK);
        Object key = new Object();
        assertThat(typeCache.find(ClassLoader.getSystemClassLoader(), key), nullValue(Class.class));
        assertThat(typeCache.insert(ClassLoader.getSystemClassLoader(), key, Void.class), is((Object) Void.class));
        assertThat(typeCache.find(ClassLoader.getSystemClassLoader(), key), is((Object) Void.class));
        assertThat(typeCache.find(mock(ClassLoader.class), key), nullValue(Class.class));
        typeCache.clear();
        assertThat(typeCache.find(ClassLoader.getSystemClassLoader(), key), nullValue(Class.class));
    }

    @Test
    public void testCacheNullLoader() throws Exception {
        TypeCache<Object> typeCache = new TypeCache<Object>(TypeCache.Sort.WEAK);
        Object key = new Object();
        assertThat(typeCache.find(null, key), nullValue(Class.class));
        assertThat(typeCache.insert(null, key, Void.class), is((Object) Void.class));
        assertThat(typeCache.find(null, key), is((Object) Void.class));
        assertThat(typeCache.find(mock(ClassLoader.class), key), nullValue(Class.class));
        typeCache.clear();
        assertThat(typeCache.find(null, key), nullValue(Class.class));
    }

    @Test
    public void testCacheCollection() throws Exception {
        TypeCache<Object> typeCache = new TypeCache<Object>(TypeCache.Sort.WEAK);
        Object key = new Object();
        ClassLoader classLoader = mock(ClassLoader.class);
        assertThat(typeCache.find(classLoader, key), nullValue(Class.class));
        assertThat(typeCache.insert(classLoader, key, Void.class), is((Object) Void.class));
        assertThat(typeCache.find(classLoader, key), is((Object) Void.class));
        classLoader = null; // Make eligible for GC
        for (int index = 0; index < 2; index++) {
            System.gc();
            Thread.sleep(50L);
        }
        typeCache.expungeStaleEntries();
        assertThat(typeCache.cache.isEmpty(), is(true));
    }

    @Test
    public void testCacheTypeCollection() throws Exception {
        TypeCache<Object> typeCache = new TypeCache<Object>(TypeCache.Sort.WEAK);
        Object key = new Object();
        ClassLoader classLoader = mock(ClassLoader.class);
        assertThat(typeCache.find(classLoader, key), nullValue(Class.class));
        Class<?> type = new ByteBuddy().subclass(Object.class)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(typeCache.insert(classLoader, key, type), is((Object) type));
        assertThat(typeCache.find(classLoader, key), is((Object) type));
        type = null; // Make eligible for GC
        for (int index = 0; index < 2; index++) {
            System.gc();
            Thread.sleep(50L);
        }
        assertThat(typeCache.find(classLoader, key), nullValue(Class.class));
        assertThat(typeCache.insert(classLoader, key, Void.class), is((Object) Void.class));
        assertThat(typeCache.find(classLoader, key), is((Object) Void.class));
    }

    @Test
    public void testWeakReference() throws Exception {
        Reference<Class<?>> reference = TypeCache.Sort.WEAK.wrap(Void.class);
        assertThat(reference, instanceOf(WeakReference.class));
        assertThat(reference.get(), is((Object) Void.class));
    }

    @Test
    public void testSoftReference() throws Exception {
        Reference<Class<?>> reference = TypeCache.Sort.SOFT.wrap(Void.class);
        assertThat(reference, instanceOf(SoftReference.class));
        assertThat(reference.get(), is((Object) Void.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFindOrInsert() throws Exception {
        TypeCache<Object> typeCache = new TypeCache<Object>(TypeCache.Sort.WEAK);
        Object key = new Object();
        assertThat(typeCache.find(ClassLoader.getSystemClassLoader(), key), nullValue(Class.class));
        Callable<Class<?>> callable = mock(Callable.class);
        when(callable.call()).thenReturn((Class) Void.class);
        assertThat(typeCache.findOrInsert(ClassLoader.getSystemClassLoader(), key, callable, new Object()), is((Object) Void.class));
        verify(callable).call();
        assertThat(typeCache.findOrInsert(ClassLoader.getSystemClassLoader(), key, callable), is((Object) Void.class));
        assertThat(typeCache.findOrInsert(ClassLoader.getSystemClassLoader(), key, callable, new Object()), is((Object) Void.class));
        verifyNoMoreInteractions(callable);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testCreationException() throws Exception {
        TypeCache<Object> typeCache = new TypeCache<Object>(TypeCache.Sort.WEAK);
        Callable<Class<?>> callable = mock(Callable.class);
        when(callable.call()).thenThrow(RuntimeException.class);
        typeCache.findOrInsert(ClassLoader.getSystemClassLoader(), new Object(), callable, new Object());
    }

    @Test
    public void testSimpleKeyProperties() {
        assertThat(new TypeCache.SimpleKey(Object.class).hashCode(), is(new TypeCache.SimpleKey(Object.class).hashCode()));
        assertThat(new TypeCache.SimpleKey(Object.class), is(new TypeCache.SimpleKey(Object.class)));
        assertThat(new TypeCache.SimpleKey(Object.class).hashCode(), not(new TypeCache.SimpleKey(Void.class).hashCode()));
        assertThat(new TypeCache.SimpleKey(Object.class), not(new TypeCache.SimpleKey(Void.class)));
    }
}
