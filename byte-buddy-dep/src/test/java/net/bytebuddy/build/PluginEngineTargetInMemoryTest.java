package net.bytebuddy.build;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class PluginEngineTargetInMemoryTest {

    private static final String FOO = "foo";

    @Test
    public void testWriteType() throws Exception {
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        sink.store(Collections.singletonMap(TypeDescription.ForLoadedType.of(Object.class), new byte[]{1, 2, 3}));
        sink.close();
        assertThat(target.getStorage().size(), is(1));
        assertThat(target.getStorage().get(TypeDescription.ForLoadedType.of(Object.class).getInternalName() + ClassFileLocator.CLASS_FILE_EXTENSION), is(new byte[]{1, 2, 3}));
        assertThat(target.toTypeMap().size(), is(1));
        assertThat(target.toTypeMap().get(TypeDescription.ForLoadedType.of(Object.class).getName()), is(new byte[]{1, 2, 3}));
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

    @Test
    public void testMultiVersion() throws Exception {
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        sink.store(Collections.singletonMap(TypeDescription.ForLoadedType.of(Object.class), new byte[]{1, 2, 3}));
        sink.store(ClassFileVersion.JAVA_V11, Collections.singletonMap(TypeDescription.ForLoadedType.of(Object.class), new byte[]{4, 5, 6}));
        sink.store(ClassFileVersion.JAVA_V17, Collections.singletonMap(TypeDescription.ForLoadedType.of(Object.class), new byte[]{7, 8, 9}));
        sink.close();
        assertThat(target.getStorage().size(), is(3));
        assertThat(target.getStorage().get(TypeDescription.ForLoadedType.of(Object.class).getInternalName() + ClassFileLocator.CLASS_FILE_EXTENSION), is(new byte[]{1, 2, 3}));
        assertThat(target.getStorage().get(ClassFileLocator.META_INF_VERSIONS + "11/" + TypeDescription.ForLoadedType.of(Object.class).getInternalName() + ClassFileLocator.CLASS_FILE_EXTENSION), is(new byte[]{4, 5, 6}));
        assertThat(target.getStorage().get(ClassFileLocator.META_INF_VERSIONS + "17/" + TypeDescription.ForLoadedType.of(Object.class).getInternalName() + ClassFileLocator.CLASS_FILE_EXTENSION), is(new byte[]{7, 8, 9}));
        assertThat(target.toTypeMap().size(), is(1));
        assertThat(target.toTypeMap().get(TypeDescription.ForLoadedType.of(Object.class).getName()), is(new byte[]{1, 2, 3}));
        assertThat(target.toTypeMap(ClassFileVersion.JAVA_V11).size(), is(1));
        assertThat(target.toTypeMap(ClassFileVersion.JAVA_V11).get(TypeDescription.ForLoadedType.of(Object.class).getName()), is(new byte[]{4, 5, 6}));
        assertThat(target.toTypeMap(ClassFileVersion.JAVA_V17).size(), is(1));
        assertThat(target.toTypeMap(ClassFileVersion.JAVA_V17).get(TypeDescription.ForLoadedType.of(Object.class).getName()), is(new byte[]{7, 8, 9}));
    }
}
