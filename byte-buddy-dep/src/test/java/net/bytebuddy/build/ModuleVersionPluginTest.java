package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ModuleVersionPluginTest {

    private static final String FOO = "foo", BAR = "bar";

    private Plugin plugin;

    @Before
    public void setUp() throws Exception {
        plugin = new ModuleVersionPlugin(BAR);
    }

    @Test
    public void testMatches() throws Exception {
        DynamicType.Unloaded<?> module = new ByteBuddy().makeModule(FOO).make();
        assertThat(plugin.matches(module.getTypeDescription()), is(true));
    }

    @Test
    public void testCachedValue() throws Exception {
        DynamicType.Unloaded<?> module = new ByteBuddy().makeModule(FOO).make();
        TypeDescription transformed = plugin.apply(new ByteBuddy().redefine(module.getTypeDescription(), module),
                        module.getTypeDescription(),
                        module)
                .make()
                .getTypeDescription();
        assertThat(transformed.toModuleDescription().getActualName(), is(FOO));
        assertThat(transformed.toModuleDescription().getVersion(), is(BAR));
    }
}
