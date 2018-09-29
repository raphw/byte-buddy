package net.bytebuddy.build;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class PluginFactorySimpleTest {

    @Test
    public void testFactory() {
        Plugin plugin = mock(Plugin.class);
        assertThat(new Plugin.Factory.Simple(plugin).make(), is(plugin));
    }
}
