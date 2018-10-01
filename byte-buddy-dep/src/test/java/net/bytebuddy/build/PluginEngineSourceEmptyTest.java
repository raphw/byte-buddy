package net.bytebuddy.build;

import net.bytebuddy.dynamic.ClassFileLocator;
import org.junit.Test;

import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PluginEngineSourceEmptyTest {

    @Test
    public void testNonOperational() throws Exception {
        assertThat(Plugin.Engine.Source.Empty.INSTANCE.getClassFileLocator(), is((ClassFileLocator) ClassFileLocator.NoOp.INSTANCE));
        assertThat(Plugin.Engine.Source.Empty.INSTANCE.getManifest(), nullValue(Manifest.class));
        assertThat(Plugin.Engine.Source.Empty.INSTANCE.iterator().hasNext(), is(false));
    }
}
