package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderInitializationStrategyTest {

    private static final String FOO = "foo";

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
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.Dispatcher.class).apply();
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(Nexus.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.InitializationAppender.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.Dispatcher.LazyInitializer.Simple.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.InitializationStrategy.Premature.class).apply();
    }
}
