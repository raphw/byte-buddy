package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.util.Collections;
import java.util.jar.Manifest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class PluginEngineTargetDiscardingTest {

    @Test
    public void testDiscarding() throws Exception {
        assertThat(Plugin.Engine.Target.Discarding.INSTANCE.write(Plugin.Engine.Source.Origin.NO_MANIFEST), is((Plugin.Engine.Target.Sink) Plugin.Engine.Target.Discarding.INSTANCE));
        assertThat(Plugin.Engine.Target.Discarding.INSTANCE.write(new Manifest()), is((Plugin.Engine.Target.Sink) Plugin.Engine.Target.Discarding.INSTANCE));
        Plugin.Engine.Source.Element eleement = mock(Plugin.Engine.Source.Element.class);
        Plugin.Engine.Target.Discarding.INSTANCE.write(Plugin.Engine.Source.Origin.NO_MANIFEST).retain(eleement);
        verifyZeroInteractions(eleement);
        Plugin.Engine.Target.Discarding.INSTANCE.write(Plugin.Engine.Source.Origin.NO_MANIFEST).store(Collections.singletonMap(TypeDescription.OBJECT, new byte[]{1, 2, 3}));
    }
}
