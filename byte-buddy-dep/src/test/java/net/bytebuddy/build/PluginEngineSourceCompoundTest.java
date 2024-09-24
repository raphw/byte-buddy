package net.bytebuddy.build;

import net.bytebuddy.dynamic.ClassFileLocator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PluginEngineSourceCompoundTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Plugin.Engine.Source left;

    @Mock
    private Plugin.Engine.Source right;

    @Mock
    private Plugin.Engine.Source.Origin leftOrigin;

    @Mock
    private Plugin.Engine.Source.Origin rightOrigin;

    @Mock
    private ClassFileLocator leftLocator;

    @Mock
    private ClassFileLocator rightLocator;

    @Mock
    private Plugin.Engine.Source.Element leftElement;

    @Mock
    private Plugin.Engine.Source.Element rightElement;

    @Before
    public void setUp() throws Exception {
        when(left.read()).thenReturn(leftOrigin);
        when(right.read()).thenReturn(rightOrigin);
        when(leftOrigin.toClassFileLocator(null)).thenReturn(leftLocator);
        when(rightOrigin.toClassFileLocator(null)).thenReturn(rightLocator);
        when(leftLocator.locate(Mockito.any(String.class))).thenReturn(new ClassFileLocator.Resolution.Illegal(FOO));
        when(rightLocator.locate(Mockito.any(String.class))).thenReturn(new ClassFileLocator.Resolution.Illegal(FOO));
        when(leftOrigin.iterator()).then(new Answer<Iterator<Plugin.Engine.Source.Element>>() {
            @Override
            public Iterator<Plugin.Engine.Source.Element> answer(InvocationOnMock invocation) {
                return Collections.singleton(leftElement).iterator();
            }
        });
        when(rightOrigin.iterator()).then(new Answer<Iterator<Plugin.Engine.Source.Element>>() {
            @Override
            public Iterator<Plugin.Engine.Source.Element> answer(InvocationOnMock invocation) {
                return Collections.singleton(rightElement).iterator();
            }
        });
    }

    @Test
    public void testEmptyCompound() throws Exception {
        assertThat(new Plugin.Engine.Source.Compound(Collections.<Plugin.Engine.Source>emptyList()).read(), sameInstance((Plugin.Engine.Source.Origin) Plugin.Engine.Source.Empty.INSTANCE));
    }

    @Test
    public void testClassFileLocator() throws Exception {
        assertThat(new Plugin.Engine.Source.Compound(Arrays.asList(left, right)).read().toClassFileLocator(null).locate(FOO).isResolved(), is(false));
        verify(leftLocator).locate(FOO);
        verify(rightLocator).locate(FOO);
    }

    @Test
    public void testManifest() throws Exception {
        assertThat(new Plugin.Engine.Source.Compound(Arrays.asList(left, right)).read().getManifest(), nullValue(Manifest.class));
        verify(leftOrigin).getManifest();
        verify(rightOrigin).getManifest();
    }

    @Test
    public void testIteration() throws Exception {
        Iterator<Plugin.Engine.Source.Element> iterator = new Plugin.Engine.Source.Compound(Arrays.asList(left, right)).read().iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(leftElement));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(rightElement));
        assertThat(iterator.hasNext(), is(false));

    }

    @Test
    public void testClose() throws Exception {
        new Plugin.Engine.Source.Compound(Arrays.asList(left, right)).read().close();
        verify(leftOrigin).close();
        verify(rightOrigin).close();
    }
}
