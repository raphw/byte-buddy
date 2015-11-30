package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderInitializationStrategyTest {

    private static final String FOO = "foo";

    private static final int BAR = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private DynamicType.Builder<?> builder;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private AgentBuilder.InitializationStrategy.Dispatcher.LazyInitializer lazyInitializer;

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
        AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE.register(FOO, classLoader, lazyInitializer);
        verifyZeroInteractions(classLoader);
        verifyZeroInteractions(lazyInitializer);
    }

    @Test
    public void testPremature() throws Exception {
        assertThat(AgentBuilder.Default.InitializationStrategy.Premature.INSTANCE.dispatcher(),
                is((AgentBuilder.InitializationStrategy.Dispatcher) AgentBuilder.Default.InitializationStrategy.Premature.INSTANCE));
    }

    @Test
    public void testPrematureApplication() throws Exception {
        assertThat(AgentBuilder.Default.InitializationStrategy.Premature.INSTANCE.apply(builder), is((DynamicType.Builder) builder));
    }

    @Test
    public void testPrematureRegistration() throws Exception {
        AgentBuilder.Default.InitializationStrategy.Premature.INSTANCE.register(FOO, classLoader, lazyInitializer);
        verifyZeroInteractions(classLoader);
        verify(lazyInitializer).loadAuxiliaryTypes();
        verifyNoMoreInteractions(lazyInitializer);
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
    public void testSimpleInitializerReturnsInstance() throws Exception {
        assertThat(new AgentBuilder.InitializationStrategy.Dispatcher.LazyInitializer.Simple(loadedTypeInitializer).resolve(), is(loadedTypeInitializer));
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
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.Dispatcher.class).apply();
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
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.Dispatcher.LazyInitializer.Simple.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.Premature.class).apply();
    }
}
