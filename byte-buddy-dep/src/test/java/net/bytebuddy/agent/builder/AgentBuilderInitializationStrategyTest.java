package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.DynamicType;
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
import static org.junit.Assert.assertThat;

public class AgentBuilderInitializationStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private DynamicType.Builder<?> builder;

    @Test
    @SuppressWarnings("unchecked")
    public void testNoOp() throws Exception {
        assertThat(AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE.apply(builder), is((DynamicType.Builder) builder));
    }

    @Test
    public void testNexusIsPublic() throws Exception {
        Class<?> type = AgentBuilder.InitializationStrategy.SelfInjection.Nexus.class;
        while (type != null) {
            assertThat(Modifier.isPublic(type.getModifiers()), is(true));
            type = type.getDeclaringClass();
        }
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.Dispatcher.class).apply();
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.Nexus.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.Nexus.Accessor.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.Nexus.Accessor.InitializationAppender.class).apply();
    }
}
