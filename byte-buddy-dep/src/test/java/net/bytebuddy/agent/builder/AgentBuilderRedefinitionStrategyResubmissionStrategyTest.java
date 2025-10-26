package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AgentBuilderRedefinitionStrategyResubmissionStrategyTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Instrumentation instrumentation;

    @Mock
    private AgentBuilder.RedefinitionStrategy.ResubmissionScheduler resubmissionScheduler;

    @Mock
    private AgentBuilder.LocationStrategy locationStrategy;

    @Mock
    private AgentBuilder.PoolStrategy poolStrategy;

    @Mock
    private AgentBuilder.DescriptionStrategy descriptionStrategy;

    @Mock
    private AgentBuilder.FallbackStrategy fallbackStrategy;

    @Mock
    private AgentBuilder.Listener listener;

    @Mock
    private AgentBuilder.InstallationListener installationListener;

    @Mock
    private ResettableClassFileTransformer classFileTransformer;

    @Mock
    private AgentBuilder.CircularityLock circularityLock;

    @Mock
    private AgentBuilder.RawMatcher rawMatcher;

    @Mock
    private AgentBuilder.RedefinitionListenable.ResubmissionOnErrorMatcher resubmissionOnErrorMatcher;

    @Mock
    private AgentBuilder.RedefinitionListenable.ResubmissionImmediateMatcher resubmissionImmediateMatcher;

    @Mock
    private Throwable error;

    @Mock
    private AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator;

    @Mock
    private AgentBuilder.RedefinitionStrategy.Listener redefinitionListener;

    @Mock
    private TypePool typePool;

    @Mock
    private ClassFileLocator classFileLocator;

    @Before
    public void setUp() throws Exception {
        when(locationStrategy.classFileLocator(Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(classFileLocator);
        when(poolStrategy.typePool(classFileLocator, Foo.class.getClassLoader())).thenReturn(typePool);
        when(descriptionStrategy.apply(Foo.class.getName(),
                Foo.class,
                typePool,
                circularityLock,
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class))).thenReturn(TypeDescription.ForLoadedType.of(Foo.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformation() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(true);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verify(instrumentation).isModifiableClass(Foo.class);
        verify(instrumentation).retransformClasses(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>singletonList(Foo.class));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinition() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(true);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1, 2, 3}));
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verify(instrumentation).isModifiableClass(Foo.class);
        verify(instrumentation).redefineClasses(Mockito.argThat(new ArgumentMatcher<ClassDefinition>() {
            public boolean matches(ClassDefinition classDefinition) {
                return classDefinition.getDefinitionClass() == Foo.class && Arrays.equals(classDefinition.getDefinitionClassFile(), new byte[]{1, 2, 3});
            }
        }));
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>singletonList(Foo.class));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationNonModifiable() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(false);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(true);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verify(listener).onDiscovery(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(Foo.class), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onComplete(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionNonModifiable() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(false);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(true);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1, 2, 3}));
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verify(listener).onDiscovery(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(Foo.class), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onComplete(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationNonMatched() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(false);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(true);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verify(listener).onDiscovery(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(Foo.class), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onComplete(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionNonMatched() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(false);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(true);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1, 2, 3}));
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verify(listener).onDiscovery(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(Foo.class), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onComplete(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationAlreadyLoaded() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(false);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true, error);
        verifyNoMoreInteractions(listener);
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionAlreadyLoaded() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(false);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1, 2, 3}));
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true, error);
        verifyNoMoreInteractions(listener);
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationNonMatchedError() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(false);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionNonMatchedError() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenReturn(true);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(false);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1, 2, 3}));
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationError() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        RuntimeException runtimeException = new RuntimeException();
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenThrow(runtimeException);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(true);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verify(listener).onDiscovery(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true, runtimeException);
        verify(listener).onComplete(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionError() throws Exception {
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(redefinitionBatchAllocator.batch(Mockito.any(List.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(invocationOnMock.getArgument(0));
            }
        });
        RuntimeException runtimeException = new RuntimeException();
        when(rawMatcher.matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain())).thenThrow(runtimeException);
        when(resubmissionOnErrorMatcher.matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class))).thenReturn(true);
        when(resubmissionScheduler.isAlive()).thenReturn(true);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[]{1, 2, 3}));
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Installation installation = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled(
                resubmissionScheduler,
                resubmissionOnErrorMatcher,
                resubmissionImmediateMatcher).apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                redefinitionBatchAllocator,
                redefinitionListener);
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        installation.getListener().onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(resubmissionScheduler).isAlive();
        verify(resubmissionScheduler).schedule(argumentCaptor.capture());
        argumentCaptor.getValue().run();
        verifyNoMoreInteractions(resubmissionScheduler);
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(TypeDescription.ForLoadedType.of(Foo.class),
                Foo.class.getClassLoader(),
                JavaModule.ofType(Foo.class),
                Foo.class,
                Foo.class.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verify(redefinitionBatchAllocator).batch(Collections.<Class<?>>emptyList());
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, error);
        verify(listener).onDiscovery(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verify(listener).onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true, runtimeException);
        verify(listener).onComplete(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true);
        verifyNoMoreInteractions(listener);
        verify(resubmissionOnErrorMatcher).matches(error, Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class));
        verifyNoMoreInteractions(resubmissionOnErrorMatcher);
    }

    @Test
    public void testDisabledListener() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Disabled.INSTANCE.apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener).getListener(), sameInstance(listener));
    }

    @Test
    public void testDisabledInstallationListener() throws Exception {
        assertThat(AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Disabled.INSTANCE.apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                rawMatcher,
                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                redefinitionBatchAllocator,
                redefinitionListener).getInstallationListener(), sameInstance(installationListener));
    }

    @Test
    public void testLookupKeyBootstrapLoaderReference() throws Exception {
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey key = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        assertThat(key.hashCode(), is(0));
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey other = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey(new URLClassLoader(new URL[0]));
        System.gc();
        assertThat(key, not(is(other)));
        assertThat(key, is(new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, is((Object) new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, not(is((Object) new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
    }

    @Test
    public void testLookupKeyNonBootstrapReference() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey key = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey(classLoader);
        assertThat(key, is(new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey(classLoader)));
        assertThat(key.hashCode(), is(classLoader.hashCode()));
        assertThat(key, not(is(new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER))));
        assertThat(key, not(is((Object) new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
    }

    @Test
    public void testStorageKeyBootstrapLoaderReference() throws Exception {
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey key = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        assertThat(key.isBootstrapLoader(), is(true));
        assertThat(key.hashCode(), is(0));
        assertThat(key.get(), nullValue(ClassLoader.class));
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey other = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey(new URLClassLoader(new URL[0]));
        System.gc();
        assertThat(other.get(), nullValue(ClassLoader.class));
        assertThat(key, not(is(other)));
        assertThat(key, is(new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, is((Object) new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, not(is((Object) new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
    }

    @Test
    public void testStorageKeyNonBootstrapReference() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey key = new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey(classLoader);
        assertThat(key.isBootstrapLoader(), is(false));
        assertThat(key, is(new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey(classLoader)));
        assertThat(key.hashCode(), is(classLoader.hashCode()));
        assertThat(key.get(), is(classLoader));
        classLoader = null; // Make GC eligible.
        System.gc();
        assertThat(key.get(), nullValue(ClassLoader.class));
        assertThat(key, not(is(new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER))));
        assertThat(key, not(is((Object) new AgentBuilder.RedefinitionStrategy.ResubmissionStrategy.Enabled.LookupKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
        assertThat(key.isBootstrapLoader(), is(false));
    }

    @Test
    public void testSchedulerNoOp() throws Exception {
        Runnable runnable = mock(Runnable.class);
        AgentBuilder.RedefinitionStrategy.ResubmissionScheduler.NoOp.INSTANCE.schedule(runnable);
        verifyNoMoreInteractions(runnable);
        assertThat(AgentBuilder.RedefinitionStrategy.ResubmissionScheduler.NoOp.INSTANCE.isAlive(), is(false));
    }

    @Test
    public void testSchedulerAtFixedRate() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        Runnable runnable = mock(Runnable.class);
        new AgentBuilder.RedefinitionStrategy.ResubmissionScheduler.AtFixedRate(scheduledExecutorService, 42L, TimeUnit.SECONDS).schedule(runnable);
        verify(scheduledExecutorService).scheduleAtFixedRate(runnable, 42L, 42L, TimeUnit.SECONDS);
    }

    @Test
    public void testSchedulerAtFixedRateIsAlive() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        assertThat(new AgentBuilder.RedefinitionStrategy.ResubmissionScheduler.AtFixedRate(scheduledExecutorService, 42L, TimeUnit.SECONDS).isAlive(), is(true));
        verify(scheduledExecutorService).isShutdown();
    }

    @Test
    public void testSchedulerWithFixedDelay() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        assertThat(new AgentBuilder.RedefinitionStrategy.ResubmissionScheduler.WithFixedDelay(scheduledExecutorService, 42L, TimeUnit.SECONDS).isAlive(), is(true));
        verify(scheduledExecutorService).isShutdown();
    }

    private static class Foo {
        /* empty */
    }
}
