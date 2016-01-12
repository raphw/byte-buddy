package net.bytebuddy.agent.builder;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderInitializationStrategyTest {

    private static final String FOO = "foo";

    private static final byte[] QUX = new byte[]{1, 2, 3}, BAZ = new byte[]{4, 5, 6};

    private static final int BAR = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private DynamicType.Builder<?> builder;

    @Mock
    private DynamicType dynamicType;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private AgentBuilder.InitializationStrategy.Dispatcher.InjectorFactory injectorFactory;

    @Test
    public void testNoOp() throws Exception {
        assertThat(AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE.dispatcher(),
                is((AgentBuilder.InitializationStrategy.Dispatcher) AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE));
    }

    @Test
    public void testNoOpApplication() throws Exception {
        assertThat(AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE.apply(builder), is((DynamicType.Builder) builder));
    }

    @Test
    public void testNoOpRegistration() throws Exception {
        AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE.register(dynamicType, classLoader, injectorFactory);
        verifyZeroInteractions(dynamicType);
        verifyZeroInteractions(classLoader);
        verifyZeroInteractions(injectorFactory);
    }

    @Test
    public void testPremature() throws Exception {
        assertThat(AgentBuilder.InitializationStrategy.Minimal.INSTANCE.dispatcher(),
                is((AgentBuilder.InitializationStrategy.Dispatcher) AgentBuilder.InitializationStrategy.Minimal.INSTANCE));
    }

    @Test
    public void testPrematureApplication() throws Exception {
        assertThat(AgentBuilder.InitializationStrategy.Minimal.INSTANCE.apply(builder), is((DynamicType.Builder) builder));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMinimalRegistrationIndependentType() throws Exception {
        Annotation eagerAnnotation = mock(AuxiliaryType.SignatureRelevant.class);
        when(eagerAnnotation.annotationType()).thenReturn((Class) AuxiliaryType.SignatureRelevant.class);
        TypeDescription independent = mock(TypeDescription.class), dependent = mock(TypeDescription.class);
        when(independent.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(eagerAnnotation));
        when(dependent.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        Map<TypeDescription, byte[]> map = new HashMap<TypeDescription, byte[]>();
        map.put(independent, QUX);
        map.put(dependent, BAZ);
        when(dynamicType.getAuxiliaryTypes()).thenReturn(map);
        ClassInjector classInjector = mock(ClassInjector.class);
        when(injectorFactory.resolve()).thenReturn(classInjector);
        when(classInjector.inject(Collections.singletonMap(independent, QUX)))
                .thenReturn(Collections.<TypeDescription, Class<?>>singletonMap(independent, Foo.class));
        LoadedTypeInitializer loadedTypeInitializer = mock(LoadedTypeInitializer.class);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(Collections.singletonMap(independent, loadedTypeInitializer));
        AgentBuilder.InitializationStrategy.Minimal.INSTANCE.register(dynamicType, classLoader, injectorFactory);
        verify(classInjector).inject(Collections.singletonMap(independent, QUX));
        verifyNoMoreInteractions(classInjector);
        verify(loadedTypeInitializer).onLoad(Foo.class);
        verifyNoMoreInteractions(loadedTypeInitializer);
    }

    @Test
    public void testMinimalRegistrationDependentType() throws Exception {
        TypeDescription dependent = mock(TypeDescription.class);
        when(dependent.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(dynamicType.getAuxiliaryTypes()).thenReturn(Collections.singletonMap(dependent, BAZ));
        AgentBuilder.InitializationStrategy.Minimal.INSTANCE.register(dynamicType, classLoader, injectorFactory);
        verifyZeroInteractions(injectorFactory);
    }

    @Test
    public void testNexusIsPublic() throws Exception {
        assertThat(Modifier.isPublic(Nexus.class.getModifiers()), is(true));
    }

    @Test
    public void testNexusHasNoDeclaringType() throws Exception {
        assertThat(Nexus.class.getDeclaringClass(), nullValue(Class.class));
    }

    @Test
    public void testNexusHasNoDeclaredTypes() throws Exception {
        assertThat(Nexus.class.getDeclaredClasses().length, is(0));
    }

    @Test
    public void testNexusAccessorClassLoaderBoundary() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Nexus.class,
                        AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.class,
                        AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.class,
                        AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.Available.class,
                        AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.Unavailable.class),
                null,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Field duplicateInitializers = classLoader.loadClass(Nexus.class.getName()).getDeclaredField("TYPE_INITIALIZERS");
        duplicateInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
        Field actualInitializers = Nexus.class.getDeclaredField("TYPE_INITIALIZERS");
        actualInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(0));
        Class<?> accessor = classLoader.loadClass(AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.class.getName());
        ClassLoader qux = mock(ClassLoader.class);
        assertThat(accessor
                .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, LoadedTypeInitializer.class)
                .invoke(accessor.getEnumConstants()[0], FOO, qux, BAR, loadedTypeInitializer), nullValue(Object.class));
        try {
            assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
            assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(1));
        } finally {
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) actualInitializers.get(null)).remove(constructor.newInstance(FOO, qux, BAR));
            assertThat(value, is((Object) loadedTypeInitializer));
        }
    }

    @Test
    public void testNexusAccessorClassLoaderNoResource() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Nexus.class,
                        AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.class,
                        AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.class,
                        AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.Available.class,
                        AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.Unavailable.class),
                null,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Field duplicateInitializers = classLoader.loadClass(Nexus.class.getName()).getDeclaredField("TYPE_INITIALIZERS");
        duplicateInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
        Field actualInitializers = Nexus.class.getDeclaredField("TYPE_INITIALIZERS");
        actualInitializers.setAccessible(true);
        assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(0));
        Class<?> accessor = classLoader.loadClass(AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.class.getName());
        ClassLoader qux = mock(ClassLoader.class);
        assertThat(accessor
                .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, LoadedTypeInitializer.class)
                .invoke(accessor.getEnumConstants()[0], FOO, qux, BAR, loadedTypeInitializer), nullValue(Object.class));
        try {
            assertThat(((Map<?, ?>) duplicateInitializers.get(null)).size(), is(0));
            assertThat(((Map<?, ?>) actualInitializers.get(null)).size(), is(1));
        } finally {
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) actualInitializers.get(null)).remove(constructor.newInstance(FOO, qux, BAR));
            assertThat(value, is((Object) loadedTypeInitializer));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testUnavailableDispatcherThrowsException() throws Exception {
        new AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.Unavailable(new Exception())
                .register(FOO, classLoader, BAR, loadedTypeInitializer);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.class).apply();
        final Iterator<Class<?>> types = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(Nexus.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return types.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.class).apply();
        final Iterator<Method> methods = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.Available.class)
                .create(new ObjectPropertyAssertion.Creator<Method>() {
                    @Override
                    public Method create() {
                        return methods.next();
                    }
                }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.Unavailable.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.InitializationAppender.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.Minimal.class).apply();
    }

    private static class Foo {
        /* empty */
    }
}
