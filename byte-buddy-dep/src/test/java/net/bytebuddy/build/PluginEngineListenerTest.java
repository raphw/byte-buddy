package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.PrintStream;
import java.util.Collections;
import java.util.jar.Manifest;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class PluginEngineListenerTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription, definingType;

    @Mock
    private Plugin plugin;

    @Mock
    private Throwable throwable;

    @Mock
    private Manifest manifest;

    @Test
    public void testNoOp() {
        Plugin.Engine.Listener.NoOp.INSTANCE.onTransformation(typeDescription, plugin);
        Plugin.Engine.Listener.NoOp.INSTANCE.onTransformation(typeDescription, Collections.singletonList(plugin));
        Plugin.Engine.Listener.NoOp.INSTANCE.onIgnored(typeDescription, plugin);
        Plugin.Engine.Listener.NoOp.INSTANCE.onIgnored(typeDescription, Collections.singletonList(plugin));
        Plugin.Engine.Listener.NoOp.INSTANCE.onError(typeDescription, plugin, throwable);
        Plugin.Engine.Listener.NoOp.INSTANCE.onError(typeDescription, Collections.singletonList(throwable));
        Plugin.Engine.Listener.NoOp.INSTANCE.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        Plugin.Engine.Listener.NoOp.INSTANCE.onError(plugin, throwable);
        Plugin.Engine.Listener.NoOp.INSTANCE.onLiveInitializer(typeDescription, definingType);
        Plugin.Engine.Listener.NoOp.INSTANCE.onComplete(typeDescription);
        Plugin.Engine.Listener.NoOp.INSTANCE.onUnresolved(FOO);
        Plugin.Engine.Listener.NoOp.INSTANCE.onManifest(manifest);
        Plugin.Engine.Listener.NoOp.INSTANCE.onResource(BAR);
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(definingType);
        verifyZeroInteractions(plugin);
        verifyZeroInteractions(throwable);
    }

    @Test
    public void testAdapterNoOp() {
        Plugin.Engine.Listener listener = new Plugin.Engine.Listener.Adapter() {
            /* empty */
        };
        listener.onTransformation(typeDescription, plugin);
        listener.onTransformation(typeDescription, Collections.singletonList(plugin));
        listener.onIgnored(typeDescription, plugin);
        listener.onIgnored(typeDescription, Collections.singletonList(plugin));
        listener.onError(typeDescription, plugin, throwable);
        listener.onError(typeDescription, Collections.singletonList(throwable));
        listener.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        listener.onError(plugin, throwable);
        listener.onLiveInitializer(typeDescription, definingType);
        listener.onComplete(typeDescription);
        listener.onUnresolved(FOO);
        listener.onManifest(manifest);
        listener.onResource(BAR);
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(definingType);
        verifyZeroInteractions(plugin);
        verifyZeroInteractions(throwable);
    }

    @Test
    public void testStreamWriting() {
        PrintStream printStream = mock(PrintStream.class);
        Plugin.Engine.Listener listener = new Plugin.Engine.Listener.StreamWriting(printStream);
        listener.onTransformation(typeDescription, plugin);
        listener.onTransformation(typeDescription, Collections.singletonList(plugin));
        listener.onIgnored(typeDescription, plugin);
        listener.onIgnored(typeDescription, Collections.singletonList(plugin));
        listener.onError(typeDescription, plugin, throwable);
        listener.onError(typeDescription, Collections.singletonList(throwable));
        listener.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        listener.onError(plugin, throwable);
        listener.onLiveInitializer(typeDescription, definingType);
        listener.onComplete(typeDescription);
        listener.onUnresolved(FOO);
        listener.onManifest(manifest);
        listener.onResource(BAR);
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(definingType);
        verifyZeroInteractions(plugin);
        verify(throwable, times(2)).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
        verify(printStream).printf("[Byte Buddy] TRANSFORM %s for %s", typeDescription, plugin);
        verify(printStream).printf("[Byte Buddy] IGNORE %s for %s", typeDescription, plugin);
        verify(printStream).printf("[Byte Buddy] ERROR %s for %s", typeDescription, plugin);
        verify(printStream).printf("[Byte Buddy] ERROR %s", plugin);
        verify(printStream).printf("[Byte Buddy] LIVE %s on %s", typeDescription, definingType);
        verify(printStream).printf("[Byte Buddy] COMPLETE %s", typeDescription);
        verify(printStream).printf("[Byte Buddy] UNRESOLVED %s", FOO);
        verify(printStream).printf("[Byte Buddy] MANIFEST %b", true);
        verify(printStream).printf("[Byte Buddy] RESOURCE %s", BAR);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingDefaults() {
        assertThat(Plugin.Engine.Listener.StreamWriting.toSystemOut(), hasPrototype(new Plugin.Engine.Listener.StreamWriting(System.out)));
        assertThat(Plugin.Engine.Listener.StreamWriting.toSystemError(), hasPrototype(new Plugin.Engine.Listener.StreamWriting(System.err)));
        assertThat(Plugin.Engine.Listener.StreamWriting.toSystemOut().withTransformationsOnly(),
                hasPrototype((Plugin.Engine.Listener) new Plugin.Engine.Listener.WithTransformationsOnly(new Plugin.Engine.Listener.StreamWriting(System.out))));
        assertThat(Plugin.Engine.Listener.StreamWriting.toSystemError().withErrorsOnly(),
                hasPrototype((Plugin.Engine.Listener) new Plugin.Engine.Listener.WithErrorsOnly(new Plugin.Engine.Listener.StreamWriting(System.err))));
    }

    @Test
    public void testForErrorHandler() {
        Plugin.Engine.ErrorHandler delegate = mock(Plugin.Engine.ErrorHandler.class);
        Plugin.Engine.Listener listener = new Plugin.Engine.Listener.ForErrorHandler(delegate);
        listener.onTransformation(typeDescription, plugin);
        listener.onTransformation(typeDescription, Collections.singletonList(plugin));
        listener.onIgnored(typeDescription, plugin);
        listener.onIgnored(typeDescription, Collections.singletonList(plugin));
        listener.onError(typeDescription, plugin, throwable);
        listener.onError(typeDescription, Collections.singletonList(throwable));
        listener.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        listener.onError(plugin, throwable);
        listener.onLiveInitializer(typeDescription, definingType);
        listener.onComplete(typeDescription);
        listener.onUnresolved(FOO);
        listener.onManifest(manifest);
        listener.onResource(BAR);
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(definingType);
        verifyZeroInteractions(plugin);
        verifyZeroInteractions(throwable);
        verify(delegate).onError(typeDescription, plugin, throwable);
        verify(delegate).onError(typeDescription, Collections.singletonList(throwable));
        verify(delegate).onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        verify(delegate).onError(plugin, throwable);
        verify(delegate).onLiveInitializer(typeDescription, definingType);
        verify(delegate).onUnresolved(FOO);
        verify(delegate).onManifest(manifest);
        verify(delegate).onResource(BAR);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testWithTransformationsOnly() {
        Plugin.Engine.Listener delegate = mock(Plugin.Engine.Listener.class);
        Plugin.Engine.Listener listener = new Plugin.Engine.Listener.WithTransformationsOnly(delegate);
        listener.onTransformation(typeDescription, plugin);
        listener.onTransformation(typeDescription, Collections.singletonList(plugin));
        listener.onIgnored(typeDescription, plugin);
        listener.onIgnored(typeDescription, Collections.singletonList(plugin));
        listener.onError(typeDescription, plugin, throwable);
        listener.onError(typeDescription, Collections.singletonList(throwable));
        listener.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        listener.onError(plugin, throwable);
        listener.onLiveInitializer(typeDescription, definingType);
        listener.onComplete(typeDescription);
        listener.onUnresolved(FOO);
        listener.onManifest(manifest);
        listener.onResource(BAR);
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(definingType);
        verifyZeroInteractions(plugin);
        verifyZeroInteractions(throwable);
        verify(delegate).onTransformation(typeDescription, plugin);
        verify(delegate).onTransformation(typeDescription, Collections.singletonList(plugin));
        verify(delegate).onError(typeDescription, plugin, throwable);
        verify(delegate).onError(typeDescription, Collections.singletonList(throwable));
        verify(delegate).onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        verify(delegate).onError(plugin, throwable);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testWithErrorsOnly() {
        Plugin.Engine.Listener delegate = mock(Plugin.Engine.Listener.class);
        Plugin.Engine.Listener listener = new Plugin.Engine.Listener.WithErrorsOnly(delegate);
        listener.onTransformation(typeDescription, plugin);
        listener.onTransformation(typeDescription, Collections.singletonList(plugin));
        listener.onIgnored(typeDescription, plugin);
        listener.onIgnored(typeDescription, Collections.singletonList(plugin));
        listener.onError(typeDescription, plugin, throwable);
        listener.onError(typeDescription, Collections.singletonList(throwable));
        listener.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        listener.onError(plugin, throwable);
        listener.onLiveInitializer(typeDescription, definingType);
        listener.onComplete(typeDescription);
        listener.onUnresolved(FOO);
        listener.onManifest(manifest);
        listener.onResource(BAR);
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(definingType);
        verifyZeroInteractions(plugin);
        verifyZeroInteractions(throwable);
        verify(delegate).onError(typeDescription, plugin, throwable);
        verify(delegate).onError(typeDescription, Collections.singletonList(throwable));
        verify(delegate).onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        verify(delegate).onError(plugin, throwable);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testCompound() {
        Plugin.Engine.Listener delegate = mock(Plugin.Engine.Listener.class);
        Plugin.Engine.Listener listener = new Plugin.Engine.Listener.Compound(delegate);
        listener.onTransformation(typeDescription, plugin);
        listener.onTransformation(typeDescription, Collections.singletonList(plugin));
        listener.onIgnored(typeDescription, plugin);
        listener.onIgnored(typeDescription, Collections.singletonList(plugin));
        listener.onError(typeDescription, plugin, throwable);
        listener.onError(typeDescription, Collections.singletonList(throwable));
        listener.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        listener.onError(plugin, throwable);
        listener.onLiveInitializer(typeDescription, definingType);
        listener.onComplete(typeDescription);
        listener.onUnresolved(FOO);
        listener.onManifest(manifest);
        listener.onResource(BAR);
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(definingType);
        verifyZeroInteractions(plugin);
        verifyZeroInteractions(throwable);
        verify(delegate).onTransformation(typeDescription, plugin);
        verify(delegate).onTransformation(typeDescription, Collections.singletonList(plugin));
        verify(delegate).onIgnored(typeDescription, plugin);
        verify(delegate).onIgnored(typeDescription, Collections.singletonList(plugin));
        verify(delegate).onError(typeDescription, plugin, throwable);
        verify(delegate).onError(typeDescription, Collections.singletonList(throwable));
        verify(delegate).onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        verify(delegate).onError(plugin, throwable);
        verify(delegate).onLiveInitializer(typeDescription, definingType);
        verify(delegate).onComplete(typeDescription);
        verify(delegate).onUnresolved(FOO);
        verify(delegate).onManifest(manifest);
        verify(delegate).onResource(BAR);
        verifyNoMoreInteractions(delegate);
    }
}
