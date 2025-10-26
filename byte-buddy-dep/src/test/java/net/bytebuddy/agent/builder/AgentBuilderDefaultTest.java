package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AgentBuilderDefaultTest {

    private static final String FOO = "foo";

    private static final byte[] QUX = new byte[]{1, 2, 3}, BAZ = new byte[]{4, 5, 6};

    private static final Class<?> REDEFINED = Foo.class, AUXILIARY = Bar.class, OTHER = Qux.class;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private Instrumentation instrumentation;

    @Mock
    private ByteBuddy byteBuddy;

    @Mock
    private DynamicType.Builder<?> builder;

    @Mock
    private DynamicType.Unloaded<?> dynamicType;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Mock
    private AgentBuilder.RawMatcher typeMatcher;

    @Mock
    private AgentBuilder.Transformer transformer;

    @Mock
    private AgentBuilder.PoolStrategy poolStrategy;

    @Mock
    private AgentBuilder.TypeStrategy typeStrategy;

    @Mock
    private AgentBuilder.LocationStrategy locationStrategy;

    @Mock
    private AgentBuilder.InitializationStrategy initializationStrategy;

    @Mock
    private AgentBuilder.InitializationStrategy.Dispatcher dispatcher;

    @Mock
    private TypePool typePool;

    @Mock
    private TypePool.Resolution resolution;

    @Mock
    private AgentBuilder.Listener listener;

    @Mock
    private AgentBuilder.InstallationListener installationListener;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(builder.make(TypeResolutionStrategy.Disabled.INSTANCE, typePool)).thenReturn((DynamicType.Unloaded) dynamicType);
        when(dynamicType.getTypeDescription()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeStrategy.builder(any(TypeDescription.class),
                eq(byteBuddy),
                any(ClassFileLocator.class),
                any(MethodNameTransformer.class),
                Mockito.<ClassLoader>any(),
                Mockito.<JavaModule>any(),
                Mockito.<ProtectionDomain>any())).thenReturn((DynamicType.Builder) builder);
        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>();
        loadedTypeInitializers.put(TypeDescription.ForLoadedType.of(REDEFINED), loadedTypeInitializer);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(loadedTypeInitializers);
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(transformer.transform(builder, TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED.getProtectionDomain()))
                .thenReturn((DynamicType.Builder) builder);
        when(poolStrategy.typePool(any(ClassFileLocator.class), any(ClassLoader.class))).thenReturn(typePool);
        when(poolStrategy.typePool(any(ClassFileLocator.class), any(ClassLoader.class), any(String.class))).thenReturn(typePool);
        when(typePool.describe(REDEFINED.getName())).thenReturn(resolution);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(initializationStrategy.dispatcher()).thenReturn(dispatcher);
        when(dispatcher.apply(builder)).thenReturn((DynamicType.Builder) builder);
    }

    @Test
    public void testSuccessfulWithoutExistingClass() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false, dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(eq(dynamicType),
                eq(REDEFINED.getClassLoader()),
                eq(REDEFINED.getProtectionDomain()),
                eq(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE));
        verifyNoMoreInteractions(dispatcher);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verify(transformer).transform(builder, TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(transformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithoutExistingClassConjunction() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(ElementMatchers.any()).and(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false, dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(eq(dynamicType),
                eq(REDEFINED.getClassLoader()),
                eq(REDEFINED.getProtectionDomain()),
                eq(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE));
        verifyNoMoreInteractions(dispatcher);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithoutExistingClassDisjunction() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(none()).or(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false, dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(eq(dynamicType),
                eq(REDEFINED.getClassLoader()),
                eq(REDEFINED.getProtectionDomain()),
                eq(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE));
        verifyNoMoreInteractions(dispatcher);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithExistingClass() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(eq(dynamicType),
                eq(REDEFINED.getClassLoader()),
                eq(REDEFINED.getProtectionDomain()),
                eq(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE));
        verifyNoMoreInteractions(dispatcher);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithExistingClassFallback() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(new RuntimeException());
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .with(AgentBuilder.FallbackStrategy.Simple.ENABLED)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(eq(dynamicType),
                eq(REDEFINED.getClassLoader()),
                eq(REDEFINED.getProtectionDomain()),
                eq(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE));
        verifyNoMoreInteractions(dispatcher);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testResetDisabled() throws Exception {
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
    }

    @Test
    public void testResetObsolete() throws Exception {
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(false);
        assertThat(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED), is(false));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
    }

    @Test(expected = IllegalStateException.class)
    public void testResetRedefinitionUnsupported() throws Exception {
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.REDEFINITION);
    }

    @Test(expected = IllegalStateException.class)
    public void testResetRetransformationUnsupported() throws Exception {
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }

    @Test
    public void testResetRedefinitionWithError() throws Exception {
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        Throwable throwable = new RuntimeException();
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(throwable);
        assertThat(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.REDEFINITION), is(true));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testResetRetransformationWithError() throws Exception {
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        Throwable throwable = new RuntimeException();
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(throwable);
        assertThat(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION), is(true));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).isRetransformClassesSupported();
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testResetRedefinitionWithErrorFromFallback() throws Exception {
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(AgentBuilder.FallbackStrategy.Simple.ENABLED)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        Throwable throwable = new RuntimeException(), suppressed = new RuntimeException();
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(suppressed);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenThrow(throwable);
        assertThat(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.REDEFINITION), is(true));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testResetRetransformationWithErrorFromFallback() throws Exception {
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(AgentBuilder.FallbackStrategy.Simple.ENABLED)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        Throwable throwable = new RuntimeException(), suppressed = new RuntimeException();
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(suppressed);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenThrow(throwable);
        assertThat(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION), is(true));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testSuccessfulWithRetransformationMatched() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithRetransformationMatchedFallback() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(new RuntimeException());
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(AgentBuilder.FallbackStrategy.Simple.ENABLED)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithRetransformationMatchedAndReset() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        assertThat(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION), is(true));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verify(instrumentation, times(2)).getAllLoadedClasses();
        verify(instrumentation, times(2)).isModifiableClass(REDEFINED);
        verify(instrumentation, times(2)).retransformClasses(REDEFINED);
        verify(instrumentation, times(2)).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher, times(2)).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verify(installationListener).onReset(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithRetransformationMatchedFallbackAndReset() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(new RuntimeException());
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(AgentBuilder.FallbackStrategy.Simple.ENABLED)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        assertThat(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION), is(true));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verify(instrumentation, times(2)).getAllLoadedClasses();
        verify(instrumentation, times(2)).isModifiableClass(REDEFINED);
        verify(instrumentation, times(2)).retransformClasses(REDEFINED);
        verify(instrumentation, times(2)).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher, times(2)).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher, times(2)).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verify(installationListener).onReset(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRetransformationWithNonRedefinable() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(false);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRetransformationWithNonMatched() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRetransformationWithNonMatchedListenerException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        RuntimeException exception = new RuntimeException();
        doThrow(exception).when(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRetransformationWithNonMatchedListenerCompleteException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSuccessfulWithRetransformationMatchedChunked() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator = mock(AgentBuilder.RedefinitionStrategy.BatchAllocator.class);
        when(redefinitionBatchAllocator.batch(Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Arrays.asList(Collections.singletonList(REDEFINED), Collections.singletonList(OTHER)));
        AgentBuilder.RedefinitionStrategy.Listener redefinitionListener = mock(AgentBuilder.RedefinitionStrategy.Listener.class);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(redefinitionBatchAllocator)
                .with(redefinitionListener)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation).retransformClasses(OTHER);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(redefinitionBatchAllocator).batch(Arrays.asList(REDEFINED, OTHER));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(redefinitionListener).onBatch(0, Collections.<Class<?>>singletonList(REDEFINED), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onBatch(1, Collections.<Class<?>>singletonList(OTHER), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onComplete(2, Arrays.asList(REDEFINED, OTHER), Collections.<List<Class<?>>, Throwable>emptyMap());
        verifyNoMoreInteractions(redefinitionListener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationChunkedOneFails() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        Throwable throwable = new UnmodifiableClassException();
        doThrow(throwable).when(instrumentation).retransformClasses(OTHER);
        AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator = mock(AgentBuilder.RedefinitionStrategy.BatchAllocator.class);
        when(redefinitionBatchAllocator.batch(Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Arrays.asList(Collections.singletonList(REDEFINED), Collections.singletonList(OTHER)));
        AgentBuilder.RedefinitionStrategy.Listener redefinitionListener = mock(AgentBuilder.RedefinitionStrategy.Listener.class);
        when(redefinitionListener.onError(1, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER))).thenReturn((Iterable) Collections.emptyList());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(redefinitionBatchAllocator)
                .with(redefinitionListener)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation).retransformClasses(OTHER);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(redefinitionBatchAllocator).batch(Arrays.asList(REDEFINED, OTHER));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(redefinitionListener).onBatch(0, Collections.<Class<?>>singletonList(REDEFINED), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onBatch(1, Collections.<Class<?>>singletonList(OTHER), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onError(1, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onComplete(2, Arrays.asList(REDEFINED, OTHER), Collections.singletonMap(Collections.<Class<?>>singletonList(OTHER), throwable));
        verifyNoMoreInteractions(redefinitionListener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationChunkedOneFailsResubmit() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        Throwable throwable = new UnmodifiableClassException();
        doThrow(throwable).when(instrumentation).retransformClasses(OTHER);
        AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator = mock(AgentBuilder.RedefinitionStrategy.BatchAllocator.class);
        when(redefinitionBatchAllocator.batch(Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Arrays.asList(Collections.singletonList(REDEFINED), Collections.singletonList(OTHER)));
        AgentBuilder.RedefinitionStrategy.Listener redefinitionListener = mock(AgentBuilder.RedefinitionStrategy.Listener.class);
        when(redefinitionListener.onError(1, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Collections.singleton(Collections.singletonList(OTHER)));
        when(redefinitionListener.onError(2, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Collections.emptyList());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(redefinitionBatchAllocator)
                .with(redefinitionListener)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation, times(2)).retransformClasses(OTHER);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(redefinitionBatchAllocator).batch(Arrays.asList(REDEFINED, OTHER));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(redefinitionListener).onBatch(0, Collections.<Class<?>>singletonList(REDEFINED), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onBatch(1, Collections.<Class<?>>singletonList(OTHER), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onBatch(2, Collections.<Class<?>>singletonList(OTHER), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onError(1, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onError(2, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onComplete(3, Arrays.asList(REDEFINED, OTHER), Collections.singletonMap(Collections.<Class<?>>singletonList(OTHER), throwable));
        verifyNoMoreInteractions(redefinitionListener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRetransformationChunkedOneFailsEscalated() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        Throwable throwable = new RuntimeException();
        doThrow(throwable).when(instrumentation).retransformClasses(REDEFINED, OTHER);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_FAST)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation).retransformClasses(REDEFINED, OTHER);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onError(eq(instrumentation), eq(classFileTransformer), argThat(new CauseMatcher(throwable, 1)));
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testRetransformationPatchPreviousDoesNotMatch() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer previous = mock(ResettableClassFileTransformer.class);
        when(previous.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        when(previous.iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(Collections.<AgentBuilder.Transformer>emptySet().iterator());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .patchOn(instrumentation, previous);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(previous).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verify(previous).iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(previous);
    }

    @Test
    public void testRetransformationPatchPreviousDoesMatch() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer previous = mock(ResettableClassFileTransformer.class);
        when(previous.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        when(previous.iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(Collections.singleton(transformer).iterator());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .patchOn(instrumentation, previous);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        //verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(previous).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verify(previous).iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(previous);
    }

    @Test
    public void testRetransformationPatchOnlyPreviousDoesMatchEqual() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer previous = mock(ResettableClassFileTransformer.class);
        when(previous.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        when(previous.iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(Collections.singleton(transformer).iterator());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .patchOn(instrumentation, previous);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        //verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(previous).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verify(previous).iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(previous);
    }

    @Test
    public void testRetransformationPatchPreviousDoesMatchUnequal() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer previous = mock(ResettableClassFileTransformer.class);
        when(previous.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        when(previous.iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(Collections.singleton(mock(AgentBuilder.Transformer.class)).iterator());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .patchOn(instrumentation, previous);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        //verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(previous).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verify(previous).iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(previous);
    }

    @Test(expected = IllegalStateException.class)
    public void testRetransformationNotSupported() throws Exception {
        new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
    }

    @Test
    public void testSuccessfulWithRedefinitionMatched() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).redefineClasses(any(ClassDefinition.class));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(dispatcher);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithRedefinitionMatchedFallback() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(new RuntimeException());
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(AgentBuilder.FallbackStrategy.Simple.ENABLED)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).redefineClasses(any(ClassDefinition.class));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithRedefinitionMatchedAndReset() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        assertThat(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.REDEFINITION), is(true));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verify(instrumentation, times(2)).getAllLoadedClasses();
        verify(instrumentation, times(2)).isModifiableClass(REDEFINED);
        verify(instrumentation, times(2)).redefineClasses(any(ClassDefinition.class));
        verify(instrumentation, times(2)).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher, times(2)).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(dispatcher);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verify(installationListener).onReset(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSuccessfulWithRedefinitionMatchedFallbackAndReset() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(new RuntimeException());
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(AgentBuilder.FallbackStrategy.Simple.ENABLED)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        when(instrumentation.removeTransformer(classFileTransformer)).thenReturn(true);
        assertThat(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.REDEFINITION), is(true));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).removeTransformer(classFileTransformer);
        verify(instrumentation, times(2)).getAllLoadedClasses();
        verify(instrumentation, times(2)).isModifiableClass(REDEFINED);
        verify(instrumentation, times(2)).redefineClasses(any(ClassDefinition.class));
        verify(instrumentation, times(2)).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher, times(2)).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher, times(2)).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verify(installationListener).onReset(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRedefinitionWithNonRedefinable() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(false);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRedefinitionWithNonMatched() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRedefinitionWithIgnoredType() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        @SuppressWarnings("unchecked")
        ElementMatcher<? super TypeDescription> ignoredTypes = mock(ElementMatcher.class);
        when(ignoredTypes.matches(TypeDescription.ForLoadedType.of(REDEFINED))).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(ignoredTypes)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(ignoredTypes).matches(TypeDescription.ForLoadedType.of(REDEFINED));
        verifyNoMoreInteractions(ignoredTypes);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRedefinitionWithIgnoredClassLoader() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        @SuppressWarnings("unchecked")
        ElementMatcher<? super TypeDescription> ignoredTypes = mock(ElementMatcher.class);
        when(ignoredTypes.matches(TypeDescription.ForLoadedType.of(REDEFINED))).thenReturn(true);
        @SuppressWarnings("unchecked")
        ElementMatcher<? super ClassLoader> ignoredClassLoaders = mock(ElementMatcher.class);
        when(ignoredClassLoaders.matches(REDEFINED.getClassLoader())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(ignoredTypes, ignoredClassLoaders)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(ignoredClassLoaders).matches(REDEFINED.getClassLoader());
        verifyNoMoreInteractions(ignoredClassLoaders);
        verify(ignoredTypes).matches(TypeDescription.ForLoadedType.of(REDEFINED));
        verifyNoMoreInteractions(ignoredTypes);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRedefinitionWithIgnoredTypeChainedConjunction() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        @SuppressWarnings("unchecked")
        ElementMatcher<? super TypeDescription> ignoredTypes = mock(ElementMatcher.class);
        when(ignoredTypes.matches(TypeDescription.ForLoadedType.of(REDEFINED))).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(ElementMatchers.any()).and(ignoredTypes)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(ignoredTypes).matches(TypeDescription.ForLoadedType.of(REDEFINED));
        verifyNoMoreInteractions(ignoredTypes);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRedefinitionWithIgnoredTypeChainedDisjunction() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        @SuppressWarnings("unchecked")
        ElementMatcher<? super TypeDescription> ignoredTypes = mock(ElementMatcher.class);
        when(ignoredTypes.matches(TypeDescription.ForLoadedType.of(REDEFINED))).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none()).or(ignoredTypes)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(ignoredTypes).matches(TypeDescription.ForLoadedType.of(REDEFINED));
        verifyNoMoreInteractions(ignoredTypes);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRedefinitionWithNonMatchedListenerException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        RuntimeException exception = new RuntimeException();
        doThrow(exception).when(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testSkipRedefinitionWithNonMatchedListenerFinishedException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSuccessfulWithRedefinitionMatchedChunked() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator = mock(AgentBuilder.RedefinitionStrategy.BatchAllocator.class);
        when(redefinitionBatchAllocator.batch(Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Arrays.asList(Collections.singletonList(REDEFINED), Collections.singletonList(OTHER)));
        AgentBuilder.RedefinitionStrategy.Listener redefinitionListener = mock(AgentBuilder.RedefinitionStrategy.Listener.class);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(redefinitionBatchAllocator)
                .with(redefinitionListener)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation, times(2)).redefineClasses(any(ClassDefinition.class));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(redefinitionBatchAllocator).batch(Arrays.asList(REDEFINED, OTHER));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(redefinitionListener).onBatch(0, Collections.<Class<?>>singletonList(REDEFINED), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onBatch(1, Collections.<Class<?>>singletonList(OTHER), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onComplete(2, Arrays.asList(REDEFINED, OTHER), Collections.<List<Class<?>>, Throwable>emptyMap());
        verifyNoMoreInteractions(redefinitionListener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionChunkedOneFails() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        Throwable throwable = new ClassNotFoundException();
        doThrow(throwable).when(instrumentation).redefineClasses(argThat(new ClassRedefinitionMatcher(OTHER)));
        AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator = mock(AgentBuilder.RedefinitionStrategy.BatchAllocator.class);
        when(redefinitionBatchAllocator.batch(Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Arrays.asList(Collections.singletonList(REDEFINED), Collections.singletonList(OTHER)));
        AgentBuilder.RedefinitionStrategy.Listener redefinitionListener = mock(AgentBuilder.RedefinitionStrategy.Listener.class);
        when(redefinitionListener.onError(1, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER))).thenReturn((Iterable) Collections.emptyList());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(redefinitionBatchAllocator)
                .with(redefinitionListener)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation).redefineClasses(argThat(new ClassRedefinitionMatcher(REDEFINED)));
        verify(instrumentation).redefineClasses(argThat(new ClassRedefinitionMatcher(OTHER)));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(redefinitionBatchAllocator).batch(Arrays.asList(REDEFINED, OTHER));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(redefinitionListener).onBatch(0, Collections.<Class<?>>singletonList(REDEFINED), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onBatch(1, Collections.<Class<?>>singletonList(OTHER), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onError(1, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onComplete(2, Arrays.asList(REDEFINED, OTHER), Collections.singletonMap(Collections.<Class<?>>singletonList(OTHER), throwable));
        verifyNoMoreInteractions(redefinitionListener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionChunkedOneFailsResubmit() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        Throwable throwable = new ClassNotFoundException();
        doThrow(throwable).when(instrumentation).redefineClasses(argThat(new ClassRedefinitionMatcher(OTHER)));
        AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator = mock(AgentBuilder.RedefinitionStrategy.BatchAllocator.class);
        when(redefinitionBatchAllocator.batch(Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Arrays.asList(Collections.singletonList(REDEFINED), Collections.singletonList(OTHER)));
        AgentBuilder.RedefinitionStrategy.Listener redefinitionListener = mock(AgentBuilder.RedefinitionStrategy.Listener.class);
        when(redefinitionListener.onError(1, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Collections.singleton(Collections.singletonList(OTHER)));
        when(redefinitionListener.onError(2, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER)))
                .thenReturn((Iterable) Collections.emptyList());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(redefinitionBatchAllocator)
                .with(redefinitionListener)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation).redefineClasses(argThat(new ClassRedefinitionMatcher(REDEFINED)));
        verify(instrumentation, times(2)).redefineClasses(argThat(new ClassRedefinitionMatcher(OTHER)));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verifyNoMoreInteractions(installationListener);
        verify(redefinitionBatchAllocator).batch(Arrays.asList(REDEFINED, OTHER));
        verifyNoMoreInteractions(redefinitionBatchAllocator);
        verify(redefinitionListener).onBatch(0, Collections.<Class<?>>singletonList(REDEFINED), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onBatch(1, Collections.<Class<?>>singletonList(OTHER), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onBatch(2, Collections.<Class<?>>singletonList(OTHER), Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onError(1, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onError(2, Collections.<Class<?>>singletonList(OTHER), throwable, Arrays.asList(REDEFINED, OTHER));
        verify(redefinitionListener).onComplete(3, Arrays.asList(REDEFINED, OTHER), Collections.singletonMap(Collections.<Class<?>>singletonList(OTHER), throwable));
        verifyNoMoreInteractions(redefinitionListener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefinitionChunkedOneFailsEscalated() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        Throwable throwable = new RuntimeException();
        doThrow(throwable).when(instrumentation).redefineClasses(any(ClassDefinition.class), any(ClassDefinition.class));
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_FAST)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation).redefineClasses(any(ClassDefinition.class), any(ClassDefinition.class));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verify(installationListener).onError(eq(instrumentation), eq(classFileTransformer), argThat(new CauseMatcher(throwable, 1)));
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testRedefinitionPatchPreviousDoesNotMatch() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer previous = mock(ResettableClassFileTransformer.class);
        when(previous.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        when(previous.iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(Collections.<AgentBuilder.Transformer>emptySet().iterator());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .patchOn(instrumentation, previous);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).redefineClasses(any(ClassDefinition.class));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(previous).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verify(previous).iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(previous);
    }

    @Test
    public void testRedefinitionPatchPreviousDoesMatch() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer previous = mock(ResettableClassFileTransformer.class);
        when(previous.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        when(previous.iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(Collections.singleton(transformer).iterator());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .patchOn(instrumentation, previous);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        //verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(previous).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verify(previous).iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(previous);
    }

    @Test
    public void testRedefinitionPatchOnlyPreviousDoesMatchEqual() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer previous = mock(ResettableClassFileTransformer.class);
        when(previous.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        when(previous.iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(Collections.singleton(transformer).iterator());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .patchOn(instrumentation, previous);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        //verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(previous).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verify(previous).iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(previous);
    }

    @Test
    public void testRedefinitionPatchPreviousDoesMatchUnequal() throws Exception {
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ResettableClassFileTransformer previous = mock(ResettableClassFileTransformer.class);
        when(previous.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        when(previous.iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(Collections.singleton(mock(AgentBuilder.Transformer.class)).iterator());
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .patchOn(instrumentation, previous);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        //verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
        verify(previous).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verify(previous).iterator(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(previous);
    }

    @Test(expected = IllegalStateException.class)
    public void testRedefinitionNotSupported() throws Exception {
        new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
    }

    @Test(expected = IllegalStateException.class)
    public void testRedefinitionPatchPreviousNotRegistered() throws Exception {
        new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .type(typeMatcher).transform(transformer)
                .patchOn(instrumentation, mock(ResettableClassFileTransformer.class));
    }

    @Test
    public void testTransformationWithError() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        RuntimeException exception = new RuntimeException();
        when(resolution.resolve()).thenThrow(exception);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        try {
            transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX);
            fail();
        } catch (IllegalStateException catched) {
            assertThat(catched.getMessage(), containsString(REDEFINED.getName()));
        }
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false, exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testIgnored() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX),
                nullValue(byte[].class));
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(initializationStrategy);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyPrefixThrowsException() throws Exception {
        new AgentBuilder.Default(byteBuddy).enableNativeMethodPrefix("");
    }

    @Test
    public void testWithWarmUp() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .warmUp(REDEFINED)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false, dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), false);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(eq(dynamicType),
                eq(REDEFINED.getClassLoader()),
                eq(REDEFINED.getProtectionDomain()),
                eq(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE));
        verifyNoMoreInteractions(dispatcher);
        verify(typeMatcher).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verify(transformer).transform(builder, TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(transformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onBeforeWarmUp(Collections.<Class<?>>singleton(REDEFINED), classFileTransformer);
        verify(installationListener).onAfterWarmUp(argThat(new ArgumentMatcher<Map<Class<?>, byte[]>>() {
            public boolean matches(Map<Class<?>, byte[]> argument) {
                return argument.size() == 1
                        && argument.containsKey(REDEFINED)
                        && Arrays.equals(argument.get(REDEFINED), new byte[]{4, 5, 6});
            }
        }), eq(classFileTransformer), eq(true));
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testWithWarmUpNoTypes() throws Exception {
        AgentBuilder builder = new AgentBuilder.Default(byteBuddy);
        assertThat(builder.warmUp(), sameInstance(builder));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithWarmUpPrimitive() throws Exception {
        new AgentBuilder.Default(byteBuddy).warmUp(void.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithWarmUpArray() throws Exception {
        new AgentBuilder.Default(byteBuddy).warmUp(Object[].class);
    }

    @Test
    public void testAuxiliaryTypeInitialization() throws Exception {
        when(dynamicType.getAuxiliaryTypes()).thenReturn(Collections.<TypeDescription, byte[]>singletonMap(TypeDescription.ForLoadedType.of(AUXILIARY), QUX));
        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>();
        loadedTypeInitializers.put(TypeDescription.ForLoadedType.of(REDEFINED), loadedTypeInitializer);
        LoadedTypeInitializer auxiliaryInitializer = mock(LoadedTypeInitializer.class);
        loadedTypeInitializers.put(TypeDescription.ForLoadedType.of(AUXILIARY), auxiliaryInitializer);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(loadedTypeInitializers);
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(eq(dynamicType),
                eq(REDEFINED.getClassLoader()),
                eq(REDEFINED.getProtectionDomain()),
                eq(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE));
        verifyNoMoreInteractions(dispatcher);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testRedefinitionConsiderationException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testRetransformationConsiderationException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testRedefinitionConsiderationExceptionListenerException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        doThrow(new RuntimeException()).when(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, exception);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testRetransformationConsiderationExceptionListenerException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        doThrow(new RuntimeException()).when(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, exception);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testDecoratedTransformation() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(TypeDescription.ForLoadedType.of(REDEFINED));
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true, dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(eq(dynamicType),
                eq(REDEFINED.getClassLoader()),
                eq(REDEFINED.getProtectionDomain()),
                eq(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE));
        verifyNoMoreInteractions(dispatcher);
        verify(typeMatcher, times(2)).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verify(transformer, times(2)).transform(builder, TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(transformer);
        verify(installationListener).onBeforeInstall(instrumentation, classFileTransformer);
        verify(installationListener).onInstall(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(installationListener);
    }

    @Test
    public void testInactiveExecutingTransformerReturnsNullValue() throws Exception {
        assertThat(new AgentBuilder.Default.ExecutingTransformer(byteBuddy,
                listener,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                mock(ClassFileLocator.class),
                mock(AgentBuilder.Default.NativeMethodStrategy.class),
                initializationStrategy,
                mock(AgentBuilder.InjectionStrategy.class),
                AgentBuilder.LambdaInstrumentationStrategy.DISABLED,
                AgentBuilder.DescriptionStrategy.Default.HYBRID,
                mock(AgentBuilder.FallbackStrategy.class),
                mock(AgentBuilder.ClassFileBufferStrategy.class),
                mock(AgentBuilder.InstallationListener.class),
                mock(AgentBuilder.RawMatcher.class),
                mock(AgentBuilder.RedefinitionStrategy.ResubmissionEnforcer.class),
                Collections.<AgentBuilder.Default.Transformation>emptyList(),
                new AgentBuilder.CircularityLock.Default()).transform(mock(ClassLoader.class),
                        FOO,
                        Object.class,
                        mock(ProtectionDomain.class),
                        new byte[0]), nullValue(byte[].class));
    }

    @Test(expected = IllegalStateException.class)
    public void testExecutingTransformerReturnsRequiresLock() throws Exception {
        new AgentBuilder.Default()
                .with(mock(AgentBuilder.CircularityLock.class))
                .installOn(mock(Instrumentation.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecutingTransformerDoesNotRecurse() throws Exception {
        final AgentBuilder.Default.ExecutingTransformer executingTransformer = new AgentBuilder.Default.ExecutingTransformer(byteBuddy,
                listener,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                mock(ClassFileLocator.class),
                mock(AgentBuilder.Default.NativeMethodStrategy.class),
                initializationStrategy,
                mock(AgentBuilder.InjectionStrategy.class),
                AgentBuilder.LambdaInstrumentationStrategy.DISABLED,
                AgentBuilder.DescriptionStrategy.Default.HYBRID,
                mock(AgentBuilder.FallbackStrategy.class),
                mock(AgentBuilder.ClassFileBufferStrategy.class),
                mock(AgentBuilder.InstallationListener.class),
                mock(AgentBuilder.RawMatcher.class),
                mock(AgentBuilder.RedefinitionStrategy.ResubmissionEnforcer.class),
                Collections.<AgentBuilder.Default.Transformation>emptyList(),
                new AgentBuilder.Default.CircularityLock.Default());
        final ClassLoader classLoader = mock(ClassLoader.class);
        final ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                assertThat(executingTransformer.transform(classLoader,
                        FOO,
                        Object.class,
                        protectionDomain,
                        new byte[0]), nullValue(byte[].class));
                return null;
            }
        }).when(listener).onComplete(FOO, classLoader, JavaModule.UNSUPPORTED, true);
        assertThat(executingTransformer.transform(classLoader,
                FOO,
                Object.class,
                protectionDomain,
                new byte[0]), nullValue(byte[].class));
        verify(listener).onComplete(FOO, classLoader, JavaModule.UNSUPPORTED, true);
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testExecutingTransformerDoesNotRecurseWithModules() throws Exception {
        final AgentBuilder.Default.ExecutingTransformer executingTransformer = new AgentBuilder.Default.ExecutingTransformer(byteBuddy,
                listener,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                mock(ClassFileLocator.class),
                mock(AgentBuilder.Default.NativeMethodStrategy.class),
                initializationStrategy,
                mock(AgentBuilder.InjectionStrategy.class),
                AgentBuilder.LambdaInstrumentationStrategy.DISABLED,
                AgentBuilder.DescriptionStrategy.Default.HYBRID,
                mock(AgentBuilder.FallbackStrategy.class),
                mock(AgentBuilder.ClassFileBufferStrategy.class),
                mock(AgentBuilder.InstallationListener.class),
                mock(AgentBuilder.RawMatcher.class),
                mock(AgentBuilder.RedefinitionStrategy.ResubmissionEnforcer.class),
                Collections.<AgentBuilder.Default.Transformation>emptyList(),
                new AgentBuilder.CircularityLock.Default());
        final ClassLoader classLoader = mock(ClassLoader.class);
        final ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
        doAnswer(new Answer<Void>() {

            public Void answer(InvocationOnMock invocation) {
                assertThat(executingTransformer.transform(JavaModule.ofType(Object.class).unwrap(),
                        classLoader,
                        FOO,
                        Object.class,
                        protectionDomain,
                        new byte[0]), nullValue(byte[].class));
                return null;
            }
        }).when(listener).onComplete(FOO, classLoader, JavaModule.ofType(Object.class), true);
        assertThat(executingTransformer.transform(JavaModule.ofType(Object.class).unwrap(),
                classLoader,
                FOO,
                Object.class,
                protectionDomain,
                new byte[0]), nullValue(byte[].class));
        verify(listener).onComplete(FOO, classLoader, JavaModule.ofType(Object.class), true);
    }

    @Test
    public void testIgnoredTypeMatcherOnlyAppliedOnceWithMultipleTransformations() throws Exception {
        AgentBuilder.RawMatcher ignored = mock(AgentBuilder.RawMatcher.class);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(ignored)
                .type(typeMatcher).transform(transformer)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), nullValue(byte[].class));
        verify(listener).onDiscovery(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), true);
        verifyNoMoreInteractions(listener);
        verify(ignored).matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(ignored);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTransformerApplicationOrder() throws Exception {
        AgentBuilder.RawMatcher ignored = mock(AgentBuilder.RawMatcher.class);
        AgentBuilder.Transformer first = mock(AgentBuilder.Transformer.class), second = mock(AgentBuilder.Transformer.class);
        when(first.transform(builder, TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED.getProtectionDomain()))
                .thenReturn((DynamicType.Builder) builder);
        when(second.transform(builder, TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED.getProtectionDomain()))
                .thenReturn((DynamicType.Builder) builder);
        when(typeMatcher.matches(TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(poolStrategy)
                .with(typeStrategy)
                .with(installationListener)
                .with(listener)
                .disableNativeMethodPrefix()
                .ignore(ignored)
                .type(typeMatcher).transform(first)
                .type(typeMatcher).transform(second)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), equalTo(new byte[]{4, 5, 6}));
        InOrder inOrder = inOrder(first, second);
        inOrder.verify(first).transform(builder, TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED.getProtectionDomain());
        inOrder.verify(second).transform(builder, TypeDescription.ForLoadedType.of(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED.getProtectionDomain());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testDisableClassFormatChanges() throws Exception {
        assertThat(new AgentBuilder.Default().disableClassFormatChanges(), hasPrototype(new AgentBuilder.Default(new ByteBuddy()
                .with(Implementation.Context.Disabled.Factory.INSTANCE))
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE_FROZEN)));
    }

    @Test
    public void testBuildPlugin() throws Exception {
        Plugin plugin = mock(Plugin.class);
        assertThat(AgentBuilder.Default.of(plugin), hasPrototype((AgentBuilder) new AgentBuilder.Default()
                .with(new AgentBuilder.TypeStrategy.ForBuildEntryPoint(EntryPoint.Default.REBASE))
                .type(plugin)
                .transform(new AgentBuilder.Transformer.ForBuildPlugin(plugin))));
    }

    @Test
    public void testBuildPluginWithEntryPoint() throws Exception {
        Plugin plugin = mock(Plugin.class);
        EntryPoint entryPoint = mock(EntryPoint.class);
        ByteBuddy byteBuddy = mock(ByteBuddy.class);
        when(entryPoint.byteBuddy(ClassFileVersion.ofThisVm())).thenReturn(byteBuddy);
        assertThat(AgentBuilder.Default.of(entryPoint, plugin), hasPrototype((AgentBuilder) new AgentBuilder.Default(byteBuddy)
                .with(new AgentBuilder.TypeStrategy.ForBuildEntryPoint(entryPoint))
                .type(plugin)
                .transform(new AgentBuilder.Transformer.ForBuildPlugin(plugin))));
    }

    @Test(expected = IllegalStateException.class)
    public void testRetransformationDisabledNotEnabledAllocator() throws Exception {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.DISABLED)
                .with(mock(AgentBuilder.RedefinitionStrategy.BatchAllocator.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testRetransformationDisabledNotEnabledListener() throws Exception {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.DISABLED)
                .with(mock(AgentBuilder.RedefinitionStrategy.Listener.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testRetransformationDisabledNotEnabledResubmission() throws Exception {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.DISABLED)
                .withResubmission(mock(AgentBuilder.RedefinitionStrategy.ResubmissionScheduler.class));
    }

    public static class Foo {
        /* empty */
    }

    public static class Bar {
        /* empty */
    }

    public static class Qux {
        /* empty */
    }

    private static byte[] transform(ClassFileTransformer classFileTransformer,
                                    JavaModule javaModule,
                                    ClassLoader classLoader,
                                    String typeName,
                                    Class<?> type,
                                    ProtectionDomain protectionDomain,
                                    byte[] binaryRepresentation) throws Exception {
        try {
            return (byte[]) ClassFileTransformer.class.getDeclaredMethod("transform", Class.forName("java.lang.Module"), ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class)
                    .invoke(classFileTransformer, javaModule.unwrap(), classLoader, typeName, type, protectionDomain, binaryRepresentation);
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getTargetException();
        } catch (Exception ignored) {
            return classFileTransformer.transform(classLoader, typeName, type, protectionDomain, binaryRepresentation);
        }
    }

    private static class ClassRedefinitionMatcher implements ArgumentMatcher<ClassDefinition> {

        private final Class<?> type;

        private ClassRedefinitionMatcher(Class<?> type) {
            this.type = type;
        }

        public boolean matches(ClassDefinition classDefinition) {
            return classDefinition.getDefinitionClass() == type;
        }
    }

    private static class CauseMatcher implements ArgumentMatcher<Throwable> {

        private final Throwable throwable;

        private final int nesting;

        private CauseMatcher(Throwable throwable, int nesting) {
            this.throwable = throwable;
            this.nesting = nesting;
        }

        public boolean matches(Throwable item) {
            for (int index = 0; index < nesting; index++) {
                item = item.getCause();
            }
            return throwable.equals(item);
        }
    }
}
