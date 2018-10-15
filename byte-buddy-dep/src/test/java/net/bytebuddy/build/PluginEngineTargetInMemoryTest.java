package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class PluginEngineTargetInMemoryTest {

    private static final String FOO = "foo";

    @Test
    public void testWriteType() throws Exception {
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        sink.store(Collections.singletonMap(TypeDescription.OBJECT, new byte[]{1, 2, 3}));
        sink.close();
        assertThat(target.getStorage().size(), is(1));
        assertThat(target.getStorage().get(TypeDescription.OBJECT.getInternalName() + ".class"), is(new byte[]{1, 2, 3}));
        assertThat(target.toTypeMap().size(), is(1));
        assertThat(target.toTypeMap().get(TypeDescription.OBJECT.getName()), is(new byte[]{1, 2, 3}));
    }

    @Test
    public void testWriteResource() throws Exception {
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn(FOO);
        when(element.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        sink.retain(element);
        sink.close();
        assertThat(target.getStorage().size(), is(1));
        assertThat(target.getStorage().get(FOO), is(new byte[]{1, 2, 3}));
        assertThat(target.toTypeMap().size(), is(0));
    }

    @Test
    public void testManifest() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        target.write(manifest).close();
        assertThat(target.getStorage().size(), is(1));
        Manifest readManifest = new Manifest(new ByteArrayInputStream(target.getStorage().get(JarFile.MANIFEST_NAME)));
        assertThat(readManifest.getMainAttributes().get(Attributes.Name.MANIFEST_VERSION), is((Object) "1.0"));
    }

    @Test
    public void testIgnoreFolderElement() throws Exception {
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn(FOO + "/");
        Plugin.Engine.Target.Sink sink = new Plugin.Engine.Target.InMemory().write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        try {
            sink.retain(element);
        } finally {
            sink.close();
        }
        verify(element).getName();
        verifyNoMoreInteractions(element);
    }
}
