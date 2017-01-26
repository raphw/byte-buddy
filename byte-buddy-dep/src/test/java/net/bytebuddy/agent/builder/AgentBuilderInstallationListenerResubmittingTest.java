package net.bytebuddy.agent.builder;

import com.sun.tools.javac.util.List;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaModule;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderInstallationListenerResubmittingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation instrumentation;

    @Mock
    private AgentBuilder.InstallationListener.Resubmitting.JobHandler jobHandler;

    @Mock
    private AgentBuilder.LocationStrategy locationStrategy;

    @Mock
    private AgentBuilder.Listener listener;

    @Mock
    private AgentBuilder.CircularityLock circularityLock;

    @Mock
    private AgentBuilder.RawMatcher rawMatcher;

    @Mock
    private AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator;

    @Mock
    private AgentBuilder.RedefinitionStrategy.Listener redefinitionListener;

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformation() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgumentAt(0, List.class));
            }
        });
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        AgentBuilder.InstallationListener.Resubmitting resubmitting = new AgentBuilder.InstallationListener.Resubmitting(jobHandler);
        resubmitting.onInstall(instrumentation,
                locationStrategy,
                this.listener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(jobHandler).handle(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verify(instrumentation).retransformClasses(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>singletonList(Foo.class));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verifyZeroInteractions(listener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinition() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgumentAt(0, List.class));
            }
        });
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        when(locationStrategy.classFileLocator(Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(classFileLocator);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1,2,3}));
        AgentBuilder.InstallationListener.Resubmitting resubmitting = new AgentBuilder.InstallationListener.Resubmitting(jobHandler);
        resubmitting.onInstall(instrumentation,
                locationStrategy,
                this.listener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                redefinitionBatchAllocator,
                redefinitionListener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(jobHandler).handle(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verify(instrumentation).redefineClasses(Mockito.argThat(new BaseMatcher<ClassDefinition[]>() {
            @Override
            public boolean matches(Object o) {
                return ((ClassDefinition) o).getDefinitionClass() == Foo.class
                        && Arrays.equals(((ClassDefinition) o).getDefinitionClassFile(), new byte[]{1,2,3});
            }

            @Override
            public void describeTo(Description description) {
            }
        }));
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>singletonList(Foo.class));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verifyZeroInteractions(listener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationNonModifiable() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(false);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgumentAt(0, List.class));
            }
        });
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        AgentBuilder.InstallationListener.Resubmitting resubmitting = new AgentBuilder.InstallationListener.Resubmitting(jobHandler);
        resubmitting.onInstall(instrumentation,
                locationStrategy,
                this.listener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(jobHandler).handle(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verifyZeroInteractions(listener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionNonModifiable() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(false);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgumentAt(0, List.class));
            }
        });
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        when(locationStrategy.classFileLocator(Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(classFileLocator);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1,2,3}));
        AgentBuilder.InstallationListener.Resubmitting resubmitting = new AgentBuilder.InstallationListener.Resubmitting(jobHandler);
        resubmitting.onInstall(instrumentation,
                locationStrategy,
                this.listener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                redefinitionBatchAllocator,
                redefinitionListener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(jobHandler).handle(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verifyZeroInteractions(listener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationNonMatched() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgumentAt(0, List.class));
            }
        });
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(false);
        AgentBuilder.InstallationListener.Resubmitting resubmitting = new AgentBuilder.InstallationListener.Resubmitting(jobHandler);
        resubmitting.onInstall(instrumentation,
                locationStrategy,
                this.listener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(jobHandler).handle(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verifyZeroInteractions(listener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionNonMatched() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgumentAt(0, List.class));
            }
        });
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(false);
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        when(locationStrategy.classFileLocator(Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(classFileLocator);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1,2,3}));
        AgentBuilder.InstallationListener.Resubmitting resubmitting = new AgentBuilder.InstallationListener.Resubmitting(jobHandler);
        resubmitting.onInstall(instrumentation,
                locationStrategy,
                this.listener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                redefinitionBatchAllocator,
                redefinitionListener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(jobHandler).handle(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verifyZeroInteractions(listener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationError() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgumentAt(0, List.class));
            }
        });
        RuntimeException runtimeException = new RuntimeException();
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenThrow(runtimeException);
        AgentBuilder.InstallationListener.Resubmitting resubmitting = new AgentBuilder.InstallationListener.Resubmitting(jobHandler);
        resubmitting.onInstall(instrumentation,
                locationStrategy,
                this.listener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(jobHandler).handle(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true, runtimeException);
        verify(listener).onComplete(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verifyNoMoreInteractions(listener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionError() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgumentAt(0, List.class));
            }
        });
        RuntimeException runtimeException = new RuntimeException();
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenThrow(runtimeException);
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        when(locationStrategy.classFileLocator(Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(classFileLocator);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1,2,3}));
        AgentBuilder.InstallationListener.Resubmitting resubmitting = new AgentBuilder.InstallationListener.Resubmitting(jobHandler);
        resubmitting.onInstall(instrumentation,
                locationStrategy,
                this.listener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                redefinitionBatchAllocator,
                redefinitionListener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(jobHandler).handle(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true, runtimeException);
        verify(listener).onComplete(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testLookupKeyBootstrapLoaderReference() throws Exception {
        AgentBuilder.InstallationListener.Resubmitting.LookupKey key = new AgentBuilder.InstallationListener.Resubmitting.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        assertThat(key.hashCode(), is(0));
        AgentBuilder.InstallationListener.Resubmitting.LookupKey other = new AgentBuilder.InstallationListener.Resubmitting.LookupKey(new URLClassLoader(new URL[0]));
        System.gc();
        assertThat(key, not(is(other)));
        assertThat(key, is(new AgentBuilder.InstallationListener.Resubmitting.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, is((Object) new AgentBuilder.InstallationListener.Resubmitting.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, not(is((Object) new AgentBuilder.InstallationListener.Resubmitting.StorageKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
    }

    @Test
    public void testLookupKeyNonBootstrapReference() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        AgentBuilder.InstallationListener.Resubmitting.LookupKey key = new AgentBuilder.InstallationListener.Resubmitting.LookupKey(classLoader);
        assertThat(key, is(new AgentBuilder.InstallationListener.Resubmitting.LookupKey(classLoader)));
        assertThat(key.hashCode(), is(classLoader.hashCode()));
        assertThat(key, not(is(new AgentBuilder.InstallationListener.Resubmitting.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER))));
        assertThat(key, not(is((Object) new AgentBuilder.InstallationListener.Resubmitting.StorageKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
    }

    @Test
    public void testStorageKeyBootstrapLoaderReference() throws Exception {
        AgentBuilder.InstallationListener.Resubmitting.StorageKey key = new AgentBuilder.InstallationListener.Resubmitting.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        assertThat(key.isBootstrapLoader(), is(true));
        assertThat(key.hashCode(), is(0));
        assertThat(key.get(), nullValue(ClassLoader.class));
        AgentBuilder.InstallationListener.Resubmitting.StorageKey other = new AgentBuilder.InstallationListener.Resubmitting.StorageKey(new URLClassLoader(new URL[0]));
        System.gc();
        assertThat(other.get(), nullValue(ClassLoader.class));
        assertThat(key, not(is(other)));
        assertThat(key, is(new AgentBuilder.InstallationListener.Resubmitting.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, is((Object) new AgentBuilder.InstallationListener.Resubmitting.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, not(is((Object) new AgentBuilder.InstallationListener.Resubmitting.LookupKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
    }

    @Test
    public void testStorageKeyNonBootstrapReference() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        AgentBuilder.InstallationListener.Resubmitting.StorageKey key = new AgentBuilder.InstallationListener.Resubmitting.StorageKey(classLoader);
        assertThat(key.isBootstrapLoader(), is(false));
        assertThat(key, is(new AgentBuilder.InstallationListener.Resubmitting.StorageKey(classLoader)));
        assertThat(key.hashCode(), is(classLoader.hashCode()));
        assertThat(key.get(), is(classLoader));
        classLoader = null; // Make GC eligible.
        System.gc();
        assertThat(key.get(), nullValue(ClassLoader.class));
        assertThat(key, not(is(new AgentBuilder.InstallationListener.Resubmitting.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER))));
        assertThat(key, not(is((Object) new AgentBuilder.InstallationListener.Resubmitting.LookupKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
        assertThat(key.isBootstrapLoader(), is(false));
    }

    @Test
    public void testJobHandlerAtFixedRate() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        Runnable runnable = mock(Runnable.class);
        new AgentBuilder.InstallationListener.Resubmitting.JobHandler.AtFixedRate(scheduledExecutorService, 42L, TimeUnit.SECONDS).handle(runnable);
        verify(scheduledExecutorService).scheduleAtFixedRate(runnable, 42L, 42L, TimeUnit.SECONDS);
    }

    @Test
    public void testJobHandlerWithFixedDelay() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        Runnable runnable = mock(Runnable.class);
        new AgentBuilder.InstallationListener.Resubmitting.JobHandler.WithFixedDelay(scheduledExecutorService, 42L, TimeUnit.SECONDS).handle(runnable);
        verify(scheduledExecutorService).scheduleWithFixedDelay(runnable, 42L, 42L, TimeUnit.SECONDS);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.InstallationListener.Resubmitting.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InstallationListener.Resubmitting.JobHandler.AtFixedRate.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InstallationListener.Resubmitting.JobHandler.WithFixedDelay.class).apply();
    }

    private static class Foo {
        /* empty */
    }

    private static class Bar {
        /* empty */
    }
}
