package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.util.Collections;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class PluginEngineTargetDiscardingTest {

    @Test
    public void testDiscarding() throws Exception {
        assertThat(Plugin.Engine.Target.Discarding.INSTANCE.write(Plugin.Engine.Source.Origin.NO_MANIFEST), is((Plugin.Engine.Target.Sink) Plugin.Engine.Target.Discarding.INSTANCE));
        assertThat(Plugin.Engine.Target.Discarding.INSTANCE.write(new Manifest()), is((Plugin.Engine.Target.Sink) Plugin.Engine.Target.Discarding.INSTANCE));
        Plugin.Engine.Source.Element eleement = mock(Plugin.Engine.Source.Element.class);
        Plugin.Engine.Target.Discarding.INSTANCE.write(Plugin.Engine.Source.Origin.NO_MANIFEST).retain(eleement);
        verifyNoMoreInteractions(eleement);
        Plugin.Engine.Target.Discarding.INSTANCE.write(Plugin.Engine.Source.Origin.NO_MANIFEST).store(Collections.singletonMap(TypeDescription.ForLoadedType.of(Object.class), new byte[]{1, 2, 3}));
    }
}
