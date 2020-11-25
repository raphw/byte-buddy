package net.bytebuddy.build;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class PluginEngineSourceFilteringTest {

    @Rule
    public MockitoRule mockitoRule = new MockitoRule(this);

    @Mock
    private Plugin.Engine.Source source;

    @Mock
    private Plugin.Engine.Source.Origin origin;

    @Mock
    private ElementMatcher<Plugin.Engine.Source.Element> matcher;

    @Mock
    private Plugin.Engine.Source.Element first, second, third;

    @Test
    public void testIteration() throws Exception {
        when(source.read()).thenReturn(origin);
        when(origin.iterator()).thenReturn(Arrays.asList(first, second, third).iterator());

        when(matcher.matches(first)).thenReturn(true);
        when(matcher.matches(third)).thenReturn(true);

        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.Filtering(source, matcher).read();
        Iterator<Plugin.Engine.Source.Element> iterator = origin.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(first));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(third));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testClose() throws Exception {
        when(source.read()).thenReturn(origin);

        new Plugin.Engine.Source.Filtering(source, matcher).read().close();
        verify(origin).close();
    }

    @Test
    public void testManifestFiltered() throws Exception {
        when(source.read()).thenReturn(origin);

        assertThat(new Plugin.Engine.Source.Filtering(source, matcher, false).read().getManifest(), nullValue(Manifest.class));
        verify(origin, never()).getManifest();
    }

    @Test
    public void testManifestNotFiltered() throws Exception {
        when(source.read()).thenReturn(origin);

        assertThat(new Plugin.Engine.Source.Filtering(source, matcher).read().getManifest(), nullValue(Manifest.class));
        verify(origin).getManifest();
    }

    @Test
    public void testClassFileLocator() throws Exception {
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);

        when(source.read()).thenReturn(origin);
        when(origin.getClassFileLocator()).thenReturn(classFileLocator);

        assertThat(new Plugin.Engine.Source.Filtering(source, matcher).read().getClassFileLocator(), is(classFileLocator));
    }
}
