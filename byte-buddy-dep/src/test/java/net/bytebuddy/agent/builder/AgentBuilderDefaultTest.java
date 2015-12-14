package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class AgentBuilderDefaultTest {

    private static final byte[] QUX = new byte[]{1, 2, 3}, BAZ = new byte[]{4, 5, 6};

    private static final Class<?> REDEFINED = Foo.class, AUXILIARY = Bar.class;

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
    private AgentBuilder.RawMatcher rawMatcher;

    @Mock
    private AgentBuilder.Transformer transformer;

    @Mock
    private AgentBuilder.BinaryLocator binaryLocator;

    @Mock
    private AgentBuilder.TypeStrategy typeStrategy;

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

    private AccessControlContext accessControlContext;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(builder.make()).thenReturn((DynamicType.Unloaded) dynamicType);
        when(dynamicType.getTypeDescription()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(typeStrategy.builder(any(TypeDescription.class),
                eq(byteBuddy),
                any(ClassFileLocator.class),
                any(MethodRebaseResolver.MethodNameTransformer.class))).thenReturn((DynamicType.Builder) builder);
        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>();
        loadedTypeInitializers.put(new TypeDescription.ForLoadedType(REDEFINED), loadedTypeInitializer);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(loadedTypeInitializers);
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(transformer.transform(builder, new TypeDescription.ForLoadedType(REDEFINED))).thenReturn((DynamicType.Builder) builder);
        when(binaryLocator.classFileLocator(REDEFINED.getClassLoader())).thenReturn(classFileLocator);
        when(binaryLocator.typePool(any(ClassFileLocator.class), any(ClassLoader.class))).thenReturn(typePool);
        when(typePool.describe(REDEFINED.getName())).thenReturn(resolution);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(initializationStrategy.dispatcher()).thenReturn(dispatcher);
        when(dispatcher.apply(builder)).thenReturn((DynamicType.Builder) builder);
        accessControlContext = AccessController.getContext();
    }

    @Test
    public void testSuccessfulWithoutExistingClass() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(classFileTransformer.transform(REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName());
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
    }

    @Test
    public void testSuccessfulWithExistingClass() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(classFileTransformer.transform(REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName());
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
    }

    @Test
    public void testSkipRetransformationWithNonRedefinable() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(false);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(rawMatcher);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test
    public void testSkipRetransformationWithNonMatched() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test
    public void testSkipRetransformationWithNonMatchedListenerException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test
    public void testSkipRetransformationWithNonMatchedListenerCompleteException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onComplete(REDEFINED.getName());
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test
    public void testSuccessfulWithRetransformationMatched() throws Exception {
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain())).thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verifyZeroInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).retransformClasses(REDEFINED);
        verify(instrumentation).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetransformationNotSupported() throws Exception {
        new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
    }

    @Test
    public void testSkipRedefinitionWithNonRedefinable() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(false);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(rawMatcher);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithNonMatched() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithNonMatchedListenerException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test
    public void testSkipRedefinitionWithNonMatchedListenerFinishedException() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        doThrow(new RuntimeException()).when(listener).onComplete(REDEFINED.getName());
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test
    public void testSuccessfulWithRedefinitionMatched() throws Exception {
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(instrumentation).getAllLoadedClasses();
        verify(instrumentation).isModifiableClass(REDEFINED);
        verify(instrumentation).redefineClasses(any(ClassDefinition[].class));
        verify(instrumentation).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verify(rawMatcher).matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain());
        verifyNoMoreInteractions(rawMatcher);
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
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRedefinitionNotSupported() throws Exception {
        new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withAccessControlContext(accessControlContext)
                .withoutNativeMethodPrefix()
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
    }

    @Test
    public void testTransformationWithError() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        RuntimeException exception = mock(RuntimeException.class);
        when(resolution.resolve()).thenThrow(exception);
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(classFileTransformer.transform(REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX),
                nullValue(byte[].class));
        verify(listener).onError(REDEFINED.getName(), exception);
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test
    public void testIgnored() throws Exception {
        when(dynamicType.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(new TypeDescription.ForLoadedType(REDEFINED));
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenReturn(false);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(classFileTransformer.transform(REDEFINED.getClassLoader(), REDEFINED.getName(), REDEFINED, REDEFINED.getProtectionDomain(), QUX),
                nullValue(byte[].class));
        verify(listener).onIgnored(new TypeDescription.ForLoadedType(REDEFINED));
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(initializationStrategy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyPrefixThrowsException() throws Exception {
        new AgentBuilder.Default(byteBuddy).withNativeMethodPrefix("");
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
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), null, REDEFINED.getProtectionDomain()))
                .thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(classFileTransformer.transform(REDEFINED.getClassLoader(), REDEFINED.getName(), null, REDEFINED.getProtectionDomain(), QUX), is(BAZ));
        verify(listener).onTransformation(new TypeDescription.ForLoadedType(REDEFINED), dynamicType);
        verify(listener).onComplete(REDEFINED.getName());
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
    }

    @Test
    public void testRedefinitionConsiderationException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(listener).onError(REDEFINED.getName(), exception);
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testRetransformationConsiderationException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(listener).onError(REDEFINED.getName(), exception);
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testRedefinitionConsiderationExceptionListenerException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        doThrow(new RuntimeException()).when(listener).onError(REDEFINED.getName(), exception);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verify(listener).onError(REDEFINED.getName(), exception);
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testRetransformationConsiderationExceptionListenerException() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{REDEFINED});
        when(instrumentation.isModifiableClass(REDEFINED)).thenReturn(true);
        when(rawMatcher.matches(new TypeDescription.ForLoadedType(REDEFINED), REDEFINED.getClassLoader(), REDEFINED, REDEFINED.getProtectionDomain()))
                .thenThrow(exception);
        doThrow(new RuntimeException()).when(listener).onError(REDEFINED.getName(), exception);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .withInitializationStrategy(initializationStrategy)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .withBinaryLocator(binaryLocator)
                .withTypeStrategy(typeStrategy)
                .withListener(listener)
                .withoutNativeMethodPrefix()
                .withAccessControlContext(accessControlContext)
                .type(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verify(listener).onError(REDEFINED.getName(), exception);
        verify(listener).onComplete(REDEFINED.getName());
        verifyNoMoreInteractions(listener);
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
    public void testDisableddBootstrapInjection() throws Exception {
        AgentBuilder.Default.BootstrapInjectionStrategy.Disabled.INSTANCE.make(mock(ProtectionDomain.class));
    }

    @Test
    public void testExecutingTransformerHandlesNullValue() throws Exception {
        assertThat(new AgentBuilder.Default.ExecutingTransformer(byteBuddy,
                binaryLocator,
                typeStrategy,
                listener,
                mock(AgentBuilder.Default.NativeMethodStrategy.class),
                accessControlContext,
                initializationStrategy,
                mock(AgentBuilder.Default.BootstrapInjectionStrategy.class),
                mock(AgentBuilder.Default.Transformation.class))
                .transform(null,
                        null,
                        null,
                        null,
                        new byte[0]), nullValue(byte[].class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Default.class).create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
            @Override
            public AccessControlContext create() {
                return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Matched.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Simple.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Simple.Resolution.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Ignored.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Compound.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Resolution.Unresolved.class).apply();
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
    }

    public static class Foo {
        /* empty */
    }

    public static class Bar {
        /* empty */
    }
}
