package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class PluginNoOpTest {

    private Plugin plugin;

    @Before
    public void setUp() throws Exception {
        plugin = new Plugin.NoOp();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoOpApplication() {
        plugin.apply(mock(DynamicType.Builder.class), mock(TypeDescription.class), mock(ClassFileLocator.class));
    }

    @Test
    public void testMatch() {
        assertThat(plugin.matches(mock(TypeDescription.class)), is(false));
    }
}
