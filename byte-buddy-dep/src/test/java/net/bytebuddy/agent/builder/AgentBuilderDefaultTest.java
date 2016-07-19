package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class AgentBuilderDefaultTest {

    private static final String FOO = "foo";

    private static final byte[] QUX = new byte[]{1, 2, 3}, BAZ = new byte[]{4, 5, 6};

    private static final Class<?> REDEFINED = Foo.class, AUXILIARY = Bar.class, OTHER = Qux.class;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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
    private AgentBuilder.TypeLocator typeLocator;

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
    private ClassFileLocator classFileLocator;

    @Mock
    private TypePool.Resolution resolution;

    @Mock
    private AgentBuilder.Listener listener;

    @Mock
    private AgentBuilder.InstallationStrategy installationStrategy;

    private AccessControlContext accessControlContext;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(builder.make(TypeResolutionStrategy.Disabled.INSTANCE, typePool)).thenReturn((DynamicType.Unloaded) dynamicType);
        when(dynamicType.getTypeDescription()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeStrategy.builder(any(TypeDescription.class),
                eq(byteBuddy),
                any(ClassFileLocator.class),
                any(MethodNameTransformer.class))).thenReturn((DynamicType.Builder) builder);
        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>();
        loadedTypeInitializers.put(new TypeDescription.ForLoadedType(REDEFINED), loadedTypeInitializer);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(loadedTypeInitializers);
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(transformer.transform(builder, new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader())).thenReturn((DynamicType.Builder) builder);
        when(typeLocator.typePool(any(ClassFileLocator.class), any(ClassLoader.class))).thenReturn(typePool);
        when(typePool.describe(REDEFINED.getName())).thenReturn(resolution);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(initializationStrategy.dispatcher()).thenReturn(dispatcher);
        when(dispatcher.apply(builder)).thenReturn((DynamicType.Builder) builder);
        when(installationStrategy.onError(eq(instrumentation), any(ClassFileTransformer.class), any(Throwable.class))).then(new Answer<ClassFileTransformer>() {
            @Override
            public ClassFileTransformer answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (ClassFileTransformer) invocationOnMock.getArguments()[1];
            }
        });
        accessControlContext = AccessController.getContext();
    }

    @Test
    public void testSuccessfulWithoutExistingClass() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(dynamicType,
                REDEFINED.getClassLoader(),
                new AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory(
                        AgentBuilder.Default.BootstrapInjectionStrategy.Disabled.INSTANCE,
                        REDEFINED.getClassLoader(),
                        REDEFINED.getProtectionDomain(),
                        accessControlContext));
        verifyNoMoreInteractions(dispatcher);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verify(transformer).transform(builder, new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader());
        verifyNoMoreInteractions(transformer);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSuccessfulWithoutExistingClassConjunction() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(ElementMatchers.any()).and(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(dynamicType,
                REDEFINED.getClassLoader(),
                new AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory(
                        AgentBuilder.Default.BootstrapInjectionStrategy.Disabled.INSTANCE,
                        REDEFINED.getClassLoader(),
                        REDEFINED.getProtectionDomain(),
                        accessControlContext));
        verifyNoMoreInteractions(dispatcher);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSuccessfulWithoutExistingClassDisjunction() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(none()).or(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(dynamicType,
                REDEFINED.getClassLoader(),
                new AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory(
                        AgentBuilder.Default.BootstrapInjectionStrategy.Disabled.INSTANCE,
                        REDEFINED.getClassLoader(),
                        REDEFINED.getProtectionDomain(),
                        accessControlContext));
        verifyNoMoreInteractions(dispatcher);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSuccessfulWithExistingClass() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(dynamicType,
                REDEFINED.getClassLoader(),
                new AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory(
                        AgentBuilder.Default.BootstrapInjectionStrategy.Disabled.INSTANCE,
                        REDEFINED.getClassLoader(),
                        REDEFINED.getProtectionDomain(),
                        accessControlContext));
        verifyNoMoreInteractions(dispatcher);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSuccessfulWithRetransformationMatched() throws Exception {
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyZeroInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRetransformationWithNonRedefinable() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(false);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRetransformationWithNonMatched() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRetransformationWithNonMatchedListenerException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRetransformationWithNonMatchedListenerCompleteException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSuccessfulWithRetransformationMatchedChunked() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION_CHUNKED)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyZeroInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation).retransformClasses(OTHER);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testRetransformationChunkedOneFails() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(instrumentation).retransformClasses(OTHER);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION_CHUNKED)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyZeroInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation).retransformClasses(OTHER);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verify(installationStrategy).onError(eq(instrumentation), eq(classFileTransformer), any(Throwable.class));
        verifyNoMoreInteractions(installationStrategy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetransformationNotSupported() throws Exception {
        new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
    }

    @Test
    public void testSuccessfulWithRedefinitionMatched() throws Exception {
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyZeroInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).redefineClasses(any(ClassDefinition[].class));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(dispatcher);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithNonRedefinable() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(false);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithNonMatched() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithIgnoredType() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        @SuppressWarnings("unchecked")
        ElementMatcher<? super TypeDescription> ignoredTypes = mock(ElementMatcher.class);
        when(ignoredTypes.matches(new TypeDescription.ForLoadedType(REDEFINED))).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(ignoredTypes)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verify(ignoredTypes).matches(new TypeDescription.ForLoadedType(REDEFINED));
        verifyNoMoreInteractions(ignoredTypes);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithIgnoredClassLoader() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        @SuppressWarnings("unchecked")
        ElementMatcher<? super TypeDescription> ignoredTypes = mock(ElementMatcher.class);
        when(ignoredTypes.matches(new TypeDescription.ForLoadedType(REDEFINED))).thenReturn(true);
        @SuppressWarnings("unchecked")
        ElementMatcher<? super ClassLoader> ignoredClassLoaders = mock(ElementMatcher.class);
        when(ignoredClassLoaders.matches(REDEFINED.getClassLoader())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(ignoredTypes, ignoredClassLoaders)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verify(ignoredClassLoaders).matches(REDEFINED.getClassLoader());
        verifyNoMoreInteractions(ignoredClassLoaders);
        verify(ignoredTypes).matches(new TypeDescription.ForLoadedType(REDEFINED));
        verifyNoMoreInteractions(ignoredTypes);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithIgnoredTypeChainedConjunction() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        @SuppressWarnings("unchecked")
        ElementMatcher<? super TypeDescription> ignoredTypes = mock(ElementMatcher.class);
        when(ignoredTypes.matches(new TypeDescription.ForLoadedType(REDEFINED))).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(ElementMatchers.any()).and(ignoredTypes)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verify(ignoredTypes).matches(new TypeDescription.ForLoadedType(REDEFINED));
        verifyNoMoreInteractions(ignoredTypes);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithIgnoredTypeChainedDijunction() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        @SuppressWarnings("unchecked")
        ElementMatcher<? super TypeDescription> ignoredTypes = mock(ElementMatcher.class);
        when(ignoredTypes.matches(new TypeDescription.ForLoadedType(REDEFINED))).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none()).or(ignoredTypes)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verify(ignoredTypes).matches(new TypeDescription.ForLoadedType(REDEFINED));
        verifyNoMoreInteractions(ignoredTypes);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithNonMatchedListenerException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithNonMatchedListenerFinishedException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testSuccessfulWithRedefinitionMatchedChunked() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION_CHUNKED)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyZeroInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation, times(2)).redefineClasses(any(ClassDefinition[].class));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testRedefinitionChunkedOneFails() throws Exception {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED, OTHER});
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isModifiableClass(OTHER)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(instrumentation).redefineClasses(any(ClassDefinition[].class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION_CHUNKED)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyZeroInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).isModifiableClass(OTHER);
        verify(instrumentation, times(2)).redefineClasses(any(ClassDefinition[].class));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verify(typeMatcher).matches(new TypeDescription.ForLoadedType(OTHER), OTHER.getClassLoader(), JavaModule.ofType(OTHER), OTHER, OTHER.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verifyZeroInteractions(initializationStrategy);
        verify(installationStrategy).onError(eq(instrumentation), eq(classFileTransformer), any(Throwable.class));
        verifyNoMoreInteractions(installationStrategy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRedefinitionNotSupported() throws Exception {
        new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .with(accessControlContext)
                .disableNativeMethodPrefix()
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
    }

    @Test
    public void testTransformationWithError() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        RuntimeException exception = mock(RuntimeException.class);
        when(resolution.resolve()).thenThrow(exception);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX),
                nullValue(byte[].class));
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testIgnored() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX),
                nullValue(byte[].class));
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(initializationStrategy);
        verifyZeroInteractions(installationStrategy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyPrefixThrowsException() throws Exception {
        new AgentBuilder.Default(byteBuddy).enableNativeMethodPrefix("");
    }

    @Test
    public void testAuxiliaryTypeInitialization() throws Exception {
        when(dynamicType.getAuxiliaryTypes()).thenReturn(Collections.<TypeDescription, byte[]>singletonMap(new TypeDescription.ForLoadedType(AUXILIARY), QUX));
        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>();
        loadedTypeInitializers.put(new TypeDescription.ForLoadedType(REDEFINED), loadedTypeInitializer);
        LoadedTypeInitializer auxiliaryInitializer = mock(LoadedTypeInitializer.class);
        loadedTypeInitializers.put(new TypeDescription.ForLoadedType(AUXILIARY), auxiliaryInitializer);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(loadedTypeInitializers);
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(dynamicType,
                REDEFINED.getClassLoader(),
                new AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory(
                        AgentBuilder.Default.BootstrapInjectionStrategy.Disabled.INSTANCE,
                        REDEFINED.getClassLoader(),
                        REDEFINED.getProtectionDomain(),
                        accessControlContext));
        verifyNoMoreInteractions(dispatcher);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testRedefinitionConsiderationException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testRetransformationConsiderationException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testRedefinitionConsiderationExceptionListenerException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        doThrow(new RuntimeException()).when(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), exception);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testRetransformationConsiderationExceptionListenerException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        doThrow(new RuntimeException()).when(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), exception);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(listener).onError(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), exception);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testDecoratedTransformation() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .with(initializationStrategy)
                .with(typeLocator)
                .with(typeStrategy)
                .with(installationStrategy)
                .with(listener)
                .disableNativeMethodPrefix()
                .with(accessControlContext)
                .ignore(none())
                .type(typeMatcher).transform(transformer)
                .type(typeMatcher).transform(transformer).asDecorator()
                .installOn(instrumentation);
        assertThat(transform(classFileTransformer, JavaModule.ofType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName(), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED));
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verify(initializationStrategy).dispatcher();
        verifyNoMoreInteractions(initializationStrategy);
        verify(dispatcher).apply(builder);
        verify(dispatcher).register(dynamicType,
                REDEFINED.getClassLoader(),
                new AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory(
                        AgentBuilder.Default.BootstrapInjectionStrategy.Disabled.INSTANCE,
                        REDEFINED.getClassLoader(),
                        REDEFINED.getProtectionDomain(),
                        accessControlContext));
        verifyNoMoreInteractions(dispatcher);
        verify(typeMatcher, times(2)).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), JavaModule.ofType(REDEFINED), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(typeMatcher);
        verify(transformer, times(2)).transform(builder, new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader());
        verifyNoMoreInteractions(transformer);
        verifyZeroInteractions(installationStrategy);
    }

    @Test
    public void testBootstrapClassLoaderCapableInjectorFactoryReflection() throws Exception {
        AgentBuilder.Default.BootstrapInjectionStrategy bootstrapInjectionStrategy = mock(AgentBuilder.Default.BootstrapInjectionStrategy.class);
        ClassLoader classLoader = mock(ClassLoader.class);
        ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
        assertThat(new AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory(bootstrapInjectionStrategy,
                classLoader,
                protectionDomain,
                accessControlContext).resolve(), is((ClassInjector) new ClassInjector.UsingReflection(classLoader, protectionDomain, accessControlContext)));
        verifyZeroInteractions(bootstrapInjectionStrategy);
    }

    @Test
    public void testBootstrapClassLoaderCapableInjectorFactoryInstrumentation() throws Exception {
        AgentBuilder.Default.BootstrapInjectionStrategy bootstrapInjectionStrategy = mock(AgentBuilder.Default.BootstrapInjectionStrategy.class);
        ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
        ClassInjector classInjector = mock(ClassInjector.class);
        when(bootstrapInjectionStrategy.make(protectionDomain)).thenReturn(classInjector);
        assertThat(new AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory(bootstrapInjectionStrategy,
                null,
                protectionDomain,
                accessControlContext).resolve(), is(classInjector));
        verify(bootstrapInjectionStrategy).make(protectionDomain);
        verifyNoMoreInteractions(bootstrapInjectionStrategy);
    }

    @Test
    public void testEnabledBootstrapInjection() throws Exception {
        assertThat(new AgentBuilder.Default.BootstrapInjectionStrategy.Enabled(mock(File.class), mock(Instrumentation.class)).make(mock(ProtectionDomain.class)),
                instanceOf(ClassInjector.UsingInstrumentation.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testDisabledBootstrapInjection() throws Exception {
        AgentBuilder.Default.BootstrapInjectionStrategy.Disabled.INSTANCE.make(mock(ProtectionDomain.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecutingTransformerHandlesNullValue() throws Exception {
        assertThat(new AgentBuilder.Default.ExecutingTransformer(byteBuddy,
                typeLocator,
                typeStrategy,
                locationStrategy,
                listener,
                mock(AgentBuilder.Default.NativeMethodStrategy.class),
                accessControlContext,
                initializationStrategy,
                mock(AgentBuilder.Default.BootstrapInjectionStrategy.class),
                AgentBuilder.DescriptionStrategy.Default.HYBRID,
                mock(AgentBuilder.RawMatcher.class),
                mock(AgentBuilder.Default.Transformation.class))
                .transform(mock(ClassLoader.class),
                        FOO,
                        Object.class,
                        mock(ProtectionDomain.class),
                        new byte[0]), nullValue(byte[].class));
    }

    @Test
    public void testDisableClassFormatChanges() throws Exception {
        assertThat(new AgentBuilder.Default().disableClassFormatChanges(), is(new AgentBuilder.Default(new ByteBuddy()
                .with(Implementation.Context.Disabled.Factory.INSTANCE))
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE_DECLARED_ONLY)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Default.class).create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
            @Override
            public AccessControlContext create() {
                return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Ignoring.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transforming.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Simple.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Simple.Resolution.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Ignored.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Compound.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Resolution.Unresolved.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Resolution.Sort.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.BootstrapInjectionStrategy.Enabled.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.BootstrapInjectionStrategy.Disabled.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.ExecutingTransformer.class).create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
            @Override
            public AccessControlContext create() {
                return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Simple.Resolution.BootstrapClassLoaderCapableInjectorFactory.class)
                .create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
                    @Override
                    public AccessControlContext create() {
                        return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
                    }
                }).apply();
        final Iterator<Constructor<?>> iterator = Arrays.<Constructor<?>>asList(String.class.getDeclaredConstructors()).iterator();
        ObjectPropertyAssertion.of(AgentBuilder.Default.ExecutingTransformer.Factory.ForJava9CapableVm.class).create(new ObjectPropertyAssertion.Creator<Constructor<?>>() {
            @Override
            public Constructor<?> create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.ExecutingTransformer.Factory.ForLegacyVm.class).apply();
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
            return (byte[]) ClassFileTransformer.class.getDeclaredMethod("transform", Class.forName("java.lang.reflect.Module"), String.class, Class.class, ProtectionDomain.class, byte[].class)
                    .invoke(classFileTransformer, javaModule.unwrap(), typeName, type, protectionDomain, binaryRepresentation);
        } catch (Exception ignored) {
            return classFileTransformer.transform(classLoader, typeName, type, protectionDomain, binaryRepresentation);
        }
    }
}
