package net.bytebuddy.agent.builder;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderTransformerForBuildPluginTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Plugin plugin;

    @Mock
    private DynamicType.Builder<?> builder, result;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Test
    @SuppressWarnings("unchecked")
    public void testApplication() throws Exception {
        when(plugin.apply(builder, typeDescription)).thenReturn((DynamicType.Builder) result);
        assertThat(new AgentBuilder.Transformer.ForBuildPlugin(plugin).transform(builder, typeDescription, classLoader, module), is((DynamicType.Builder) result));
        verify(plugin).apply(builder, typeDescription);
        verifyNoMoreInteractions(plugin);
    }
}
