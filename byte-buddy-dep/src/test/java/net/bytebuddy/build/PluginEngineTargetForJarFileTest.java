package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginEngineTargetForJarFileTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File file;

    @Before
    public void setUp() throws Exception {
        file = temporaryFolder.newFile();
        assertThat(file.delete(), is(true));
    }

    @Test
    public void testWriteType() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForJarFile(file);
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        try {
            sink.store(Collections.singletonMap(TypeDescription.ForLoadedType.of(Object.class), new byte[]{1, 2, 3}));
        } finally {
            sink.close();
        }
        InputStream inputStream = new FileInputStream(file);
        try {
            JarInputStream jarInputStream = new JarInputStream(inputStream);
            assertThat(jarInputStream.getManifest(), nullValue(Manifest.class));
            JarEntry entry = jarInputStream.getNextJarEntry();
            assertThat(entry.getName(), is(TypeDescription.ForLoadedType.of(Object.class).getInternalName() + ClassFileLocator.CLASS_FILE_EXTENSION));
            assertThat(StreamDrainer.DEFAULT.drain(jarInputStream), is(new byte[]{1, 2, 3}));
            assertThat(jarInputStream.getNextJarEntry(), nullValue(JarEntry.class));
            jarInputStream.close();
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testWriteResource() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForJarFile(file);
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn(FOO + "/" + BAR);
        when(element.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        try {
            sink.retain(element);
        } finally {
            sink.close();
        }
        InputStream inputStream = new FileInputStream(file);
        try {
            JarInputStream jarInputStream = new JarInputStream(inputStream);
            assertThat(jarInputStream.getManifest(), nullValue(Manifest.class));
            JarEntry entry = jarInputStream.getNextJarEntry();
            assertThat(entry.getName(), is(FOO + "/" + BAR));
            assertThat(StreamDrainer.DEFAULT.drain(jarInputStream), is(new byte[]{1, 2, 3}));
            assertThat(jarInputStream.getNextJarEntry(), nullValue(JarEntry.class));
            jarInputStream.close();
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testWriteResourceOriginal() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForJarFile(file);
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn(FOO + "/" + BAR);
        when(element.resolveAs(JarEntry.class)).thenReturn(new JarEntry(FOO + "/" + BAR));
        when(element.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        try {
            sink.retain(element);
        } finally {
            sink.close();
        }
        assertThat(file.isFile(), is(true));
        InputStream inputStream = new FileInputStream(file);
        try {
            JarInputStream jarInputStream = new JarInputStream(inputStream);
            assertThat(jarInputStream.getManifest(), nullValue(Manifest.class));
            JarEntry entry = jarInputStream.getNextJarEntry();
            assertThat(entry.getName(), is(FOO + "/" + BAR));
            assertThat(StreamDrainer.DEFAULT.drain(jarInputStream), is(new byte[]{1, 2, 3}));
            assertThat(jarInputStream.getNextJarEntry(), nullValue(JarEntry.class));
            jarInputStream.close();
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testManifest() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForJarFile(file);
        target.write(manifest).close();
        InputStream inputStream = new FileInputStream(file);
        try {
            JarInputStream jarInputStream = new JarInputStream(inputStream);
            Manifest readManifest = jarInputStream.getManifest();
            assertThat(readManifest.getMainAttributes().get(Attributes.Name.MANIFEST_VERSION), is((Object) "1.0"));
            assertThat(jarInputStream.getNextJarEntry(), nullValue(JarEntry.class));
            jarInputStream.close();
        } finally {
            inputStream.close();
        }
    }
}
