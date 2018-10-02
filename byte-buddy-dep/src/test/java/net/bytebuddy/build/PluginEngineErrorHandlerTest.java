package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.jar.Manifest;

import static org.mockito.Mockito.*;

public class PluginEngineErrorHandlerTest {

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

    @Test(expected = IllegalStateException.class)
    public void testFailingFailFast() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_FAST.onError(typeDescription, plugin, throwable);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFailingFailFastDoesNotSupportFailAfterType() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_FAST.onError(typeDescription, Collections.singletonList(throwable));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFailingFailFastDoesNotSupportFailLast() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_FAST.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingFailFastPluginError() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_FAST.onError(plugin, throwable);
    }

    @Test
    public void testFailingFailAfterTypeDoesNotFailFast() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_AFTER_TYPE.onError(typeDescription, plugin, throwable);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingFailAfterType() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_AFTER_TYPE.onError(typeDescription, Collections.singletonList(throwable));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFailingFailAfterTypeDoesNotSupportFailLast() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_AFTER_TYPE.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingFailAfterTypePluginError() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_AFTER_TYPE.onError(plugin, throwable);
    }

    @Test
    public void testFailingFailLastDoesNotFailFast() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_LAST.onError(typeDescription, plugin, throwable);
    }

    @Test
    public void testFailingFailLastDoesNotFailAfterType() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_LAST.onError(typeDescription, Collections.singletonList(throwable));
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingFailLast() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_LAST.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingFailLastPluginError() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_LAST.onError(plugin, throwable);
    }

    @Test
    public void testFailingDoesNotFailOnUnrelated() {
        Plugin.Engine.ErrorHandler.Failing.FAIL_FAST.onLiveInitializer(typeDescription, definingType);
        Plugin.Engine.ErrorHandler.Failing.FAIL_FAST.onUnresolved(FOO);
        Plugin.Engine.ErrorHandler.Failing.FAIL_FAST.onManifest(manifest);
        Plugin.Engine.ErrorHandler.Failing.FAIL_FAST.onResource(BAR);
        Plugin.Engine.ErrorHandler.Failing.FAIL_AFTER_TYPE.onLiveInitializer(typeDescription, definingType);
        Plugin.Engine.ErrorHandler.Failing.FAIL_AFTER_TYPE.onUnresolved(FOO);
        Plugin.Engine.ErrorHandler.Failing.FAIL_AFTER_TYPE.onManifest(manifest);
        Plugin.Engine.ErrorHandler.Failing.FAIL_AFTER_TYPE.onResource(BAR);
        Plugin.Engine.ErrorHandler.Failing.FAIL_LAST.onLiveInitializer(typeDescription, definingType);
        Plugin.Engine.ErrorHandler.Failing.FAIL_LAST.onUnresolved(FOO);
        Plugin.Engine.ErrorHandler.Failing.FAIL_LAST.onManifest(manifest);
        Plugin.Engine.ErrorHandler.Failing.FAIL_LAST.onResource(BAR);
    }

    @Test(expected = IllegalStateException.class)
    public void testEnforcingFailOnUnresolved() {
        Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED.onUnresolved(FOO);
    }

    @Test(expected = IllegalStateException.class)
    public void testEnforcingFailOnLiveInitializer() {
        Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS.onLiveInitializer(typeDescription, definingType);
    }

    @Test(expected = IllegalStateException.class)
    public void testEnforcingFailOnResource() {
        Plugin.Engine.ErrorHandler.Enforcing.CLASS_FILES_ONLY.onResource(BAR);
    }

    @Test(expected = IllegalStateException.class)
    public void testEnforcingFailOnNoManifest() {
        Plugin.Engine.ErrorHandler.Enforcing.MANIFEST_REQUIRED.onManifest(null);
    }

    @Test
    public void testEnforcingDoesNotFailOnUnrelated() {
        Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED.onError(typeDescription, plugin, throwable);
        Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED.onError(typeDescription, Collections.singletonList(throwable));
        Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED.onError(plugin, throwable);
        Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED.onLiveInitializer(typeDescription, definingType);
        Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED.onManifest(manifest);
        Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED.onResource(BAR);
        Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS.onError(typeDescription, plugin, throwable);
        Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS.onError(typeDescription, Collections.singletonList(throwable));
        Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS.onError(plugin, throwable);
        Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS.onUnresolved(FOO);
        Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS.onManifest(manifest);
        Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS.onResource(BAR);
        Plugin.Engine.ErrorHandler.Enforcing.CLASS_FILES_ONLY.onError(typeDescription, plugin, throwable);
        Plugin.Engine.ErrorHandler.Enforcing.CLASS_FILES_ONLY.onError(typeDescription, Collections.singletonList(throwable));
        Plugin.Engine.ErrorHandler.Enforcing.CLASS_FILES_ONLY.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        Plugin.Engine.ErrorHandler.Enforcing.CLASS_FILES_ONLY.onError(plugin, throwable);
        Plugin.Engine.ErrorHandler.Enforcing.CLASS_FILES_ONLY.onLiveInitializer(typeDescription, definingType);
        Plugin.Engine.ErrorHandler.Enforcing.CLASS_FILES_ONLY.onManifest(manifest);
        Plugin.Engine.ErrorHandler.Enforcing.CLASS_FILES_ONLY.onUnresolved(FOO);
        Plugin.Engine.ErrorHandler.Enforcing.MANIFEST_REQUIRED.onError(typeDescription, plugin, throwable);
        Plugin.Engine.ErrorHandler.Enforcing.MANIFEST_REQUIRED.onError(typeDescription, Collections.singletonList(throwable));
        Plugin.Engine.ErrorHandler.Enforcing.MANIFEST_REQUIRED.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        Plugin.Engine.ErrorHandler.Enforcing.MANIFEST_REQUIRED.onError(plugin, throwable);
        Plugin.Engine.ErrorHandler.Enforcing.MANIFEST_REQUIRED.onLiveInitializer(typeDescription, definingType);
        Plugin.Engine.ErrorHandler.Enforcing.MANIFEST_REQUIRED.onUnresolved(FOO);
        Plugin.Engine.ErrorHandler.Enforcing.MANIFEST_REQUIRED.onManifest(manifest);
        Plugin.Engine.ErrorHandler.Enforcing.MANIFEST_REQUIRED.onResource(BAR);
    }

    @Test
    public void testCompound() {
        Plugin.Engine.ErrorHandler delegate = mock(Plugin.Engine.ErrorHandler.class);
        Plugin.Engine.ErrorHandler errorHandler = new Plugin.Engine.ErrorHandler.Compound(delegate);
        errorHandler.onError(typeDescription, plugin, throwable);
        errorHandler.onError(typeDescription, Collections.singletonList(throwable));
        errorHandler.onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        errorHandler.onError(plugin, throwable);
        errorHandler.onLiveInitializer(typeDescription, definingType);
        errorHandler.onManifest(manifest);
        errorHandler.onUnresolved(FOO);
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(definingType);
        verifyZeroInteractions(plugin);
        verifyZeroInteractions(throwable);
        verify(delegate).onError(typeDescription, plugin, throwable);
        verify(delegate).onError(typeDescription, Collections.singletonList(throwable));
        verify(delegate).onError(Collections.singletonMap(typeDescription, Collections.singletonList(throwable)));
        verify(delegate).onError(plugin, throwable);
        verify(delegate).onLiveInitializer(typeDescription, definingType);
        verify(delegate).onManifest(manifest);
        verify(delegate).onUnresolved(FOO);
        verifyNoMoreInteractions(delegate);
    }
}
