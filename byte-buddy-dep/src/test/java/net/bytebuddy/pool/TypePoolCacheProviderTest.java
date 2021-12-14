package net.bytebuddy.pool;

import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypePoolCacheProviderTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypePool.Resolution resolution;

    @Test
    public void testNoOp() throws Exception {
        assertThat(TypePool.CacheProvider.NoOp.INSTANCE.find(FOO), nullValue(TypePool.Resolution.class));
        assertThat(TypePool.CacheProvider.NoOp.INSTANCE.register(FOO, resolution), sameInstance(resolution));
        assertThat(TypePool.CacheProvider.NoOp.INSTANCE.find(FOO), nullValue(TypePool.Resolution.class));
        TypePool.CacheProvider.NoOp.INSTANCE.clear();
    }

    @Test
    public void testSimple() throws Exception {
        TypePool.CacheProvider simple = new TypePool.CacheProvider.Simple();
        assertThat(simple.find(FOO), nullValue(TypePool.Resolution.class));
        assertThat(simple.register(FOO, resolution), sameInstance(resolution));
        assertThat(simple.find(FOO), sameInstance(resolution));
        TypePool.Resolution resolution = mock(TypePool.Resolution.class);
        assertThat(simple.register(FOO, resolution), sameInstance(this.resolution));
        assertThat(simple.find(FOO), sameInstance(this.resolution));
        simple.clear();
        assertThat(simple.find(FOO), nullValue(TypePool.Resolution.class));
        assertThat(simple.register(FOO, resolution), sameInstance(resolution));
        assertThat(simple.find(FOO), sameInstance(resolution));
    }

    @Test
    public void testSimpleMap() throws Exception {
        ConcurrentMap<String, TypePool.Resolution> storage = new ConcurrentHashMap<String, TypePool.Resolution>();
        TypePool.CacheProvider.Simple cacheProvider = new TypePool.CacheProvider.Simple(storage);
        assertThat(cacheProvider.getStorage(), sameInstance(storage));
    }

    @Test
    public void testSimpleSoftlyReferenced() throws Exception {
        TypePool.CacheProvider simple = new TypePool.CacheProvider.Simple.UsingSoftReference();
        assertThat(simple.find(FOO), nullValue(TypePool.Resolution.class));
        assertThat(simple.register(FOO, resolution), sameInstance(resolution));
        assertThat(simple.find(FOO), sameInstance(resolution));
        TypePool.Resolution resolution = mock(TypePool.Resolution.class);
        assertThat(simple.register(FOO, resolution), sameInstance(this.resolution));
        assertThat(simple.find(FOO), sameInstance(this.resolution));
        simple.clear();
        assertThat(simple.find(FOO), nullValue(TypePool.Resolution.class));
        assertThat(simple.register(FOO, resolution), sameInstance(resolution));
        assertThat(simple.find(FOO), sameInstance(resolution));
    }

    @Test
    public void testDiscriminatingMatched() throws Exception {
        TypePool.CacheProvider matched = mock(TypePool.CacheProvider.class), unmatched = mock(TypePool.CacheProvider.class);
        TypePool.CacheProvider discriminating = new TypePool.CacheProvider.Discriminating(ElementMatchers.<String>is(FOO), matched, unmatched);
        when(matched.register(FOO, resolution)).thenReturn(resolution);
        assertThat(discriminating.register(FOO, resolution), sameInstance(resolution));
        verifyZeroInteractions(unmatched);
        discriminating.clear();
        verify(matched).clear();
        verify(unmatched).clear();
    }

    @Test
    public void testDiscriminatingUnmatched() throws Exception {
        TypePool.CacheProvider matched = mock(TypePool.CacheProvider.class), unmatched = mock(TypePool.CacheProvider.class);
        TypePool.CacheProvider discriminating = new TypePool.CacheProvider.Discriminating(ElementMatchers.<String>is(BAR), matched, unmatched);
        when(unmatched.register(FOO, resolution)).thenReturn(resolution);
        assertThat(discriminating.register(FOO, resolution), sameInstance(resolution));
        verifyZeroInteractions(matched);
        discriminating.clear();
        verify(matched).clear();
        verify(unmatched).clear();
    }
}
