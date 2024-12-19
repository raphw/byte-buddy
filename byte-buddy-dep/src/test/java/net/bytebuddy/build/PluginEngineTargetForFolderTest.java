package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class PluginEngineTargetForFolderTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File folder;

    @Before
    public void setUp() throws Exception {
        folder = temporaryFolder.newFolder();
    }

    @Test
    public void testWriteType() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForFolder(folder);
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        try {
            sink.store(Collections.singletonMap(TypeDescription.ForLoadedType.of(Object.class), new byte[]{1, 2, 3}));
        } finally {
            sink.close();
        }
        File file = new File(folder, TypeDescription.ForLoadedType.of(Object.class).getInternalName() + ClassFileLocator.CLASS_FILE_EXTENSION);
        assertThat(file.isFile(), is(true));
        InputStream inputStream = new FileInputStream(file);
        try {
            assertThat(StreamDrainer.DEFAULT.drain(inputStream), is(new byte[]{1, 2, 3}));
        } finally {
            inputStream.close();
        }
        assertThat(file.delete(), is(true));
        assertThat(file.getParentFile().delete(), is(true));
        assertThat(file.getParentFile().getParentFile().delete(), is(true));
    }

    @Test
    public void testWriteResource() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForFolder(folder);
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn(FOO + "/" + BAR);
        when(element.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        try {
            sink.retain(element);
        } finally {
            sink.close();
        }
        File file = new File(folder, FOO + "/" + BAR);
        assertThat(file.isFile(), is(true));
        InputStream inputStream = new FileInputStream(file);
        try {
            assertThat(StreamDrainer.DEFAULT.drain(inputStream), is(new byte[]{1, 2, 3}));
        } finally {
            inputStream.close();
        }
        assertThat(file.delete(), is(true));
        assertThat(file.getParentFile().delete(), is(true));
    }

    @Test
    public void testWriteResourceFromFile() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForFolder(folder);
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn(FOO + "/" + BAR);
        when(element.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        File original = temporaryFolder.newFile();
        try {
            FileOutputStream outputStream = new FileOutputStream(original);
            try {
                outputStream.write(new byte[]{1, 2, 3});
            } finally {
                outputStream.close();
            }
            when(element.resolveAs(File.class)).thenReturn(original);
            Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
            try {
                sink.retain(element);
            } finally {
                sink.close();
            }
            File file = new File(folder, FOO + "/" + BAR);
            assertThat(file.isFile(), is(true));
            InputStream inputStream = new FileInputStream(file);
            try {
                assertThat(StreamDrainer.DEFAULT.drain(inputStream), is(new byte[]{1, 2, 3}));
            } finally {
                inputStream.close();
            }
            assertThat(file.delete(), is(true));
            assertThat(file.getParentFile().delete(), is(true));
        } finally {
            assertThat(original.delete(), is(true));
        }
    }

    @Test
    public void testWriteFolder() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForFolder(folder);
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn(FOO + "/" + BAR + "/");
        when(element.getInputStream()).thenThrow(new AssertionError());
        File original = temporaryFolder.newFile();
        try {
            Plugin.Engine.Target.Sink sink = target.write(null);
            sink.retain(element);
            assertThat(new File(folder, FOO + "/" + BAR).isDirectory(), is(true));
            assertThat(new File(folder, FOO + "/" + BAR).delete(), is(true));
            assertThat(new File(folder, FOO).isDirectory(), is(true));
            assertThat(new File(folder, FOO).delete(), is(true));
        } finally {
            assertThat(original.delete(), is(true));
        }
    }

    @Test
    public void testWriteFolderCannotReplaceFile() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForFolder(folder);
        assertThat(new File(folder, FOO).createNewFile(), is(true));
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn(FOO + "/");
        when(element.getInputStream()).thenThrow(new AssertionError());
        File original = temporaryFolder.newFile();
        try {
            Plugin.Engine.Target.Sink sink = target.write(null);
            try {
                sink.retain(element);
                fail("Expected error on overwritten file");
            } catch (IllegalStateException exception) {
                assertThat(exception.getMessage(), is("Cannot create requested directory: " + new File(folder, FOO)));
            }
            assertThat(new File(folder, FOO).isFile(), is(true));
            assertThat(new File(folder, FOO).delete(), is(true));
        } finally {
            assertThat(original.delete(), is(true));
        }
    }

    @Test
    public void testManifest() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForFolder(folder);
        target.write(manifest).close();
        File file = new File(folder, JarFile.MANIFEST_NAME);
        assertThat(file.isFile(), is(true));
        InputStream inputStream = new FileInputStream(file);
        try {
            Manifest readManifest = new Manifest(inputStream);
            assertThat(readManifest.getMainAttributes().get(Attributes.Name.MANIFEST_VERSION), is((Object) "1.0"));
        } finally {
            inputStream.close();
        }
        assertThat(file.delete(), is(true));
        assertThat(file.getParentFile().delete(), is(true));
    }

    @Test
    public void testIgnoreFolderElement() throws Exception {
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn(FOO + "/");
        Plugin.Engine.Target.Sink sink = new Plugin.Engine.Target.ForFolder(folder).write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        try {
            sink.retain(element);
        } finally {
            sink.close();
        }
        verify(element).getName();
        verifyNoMoreInteractions(element);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotWriteRelativeLocation() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForFolder(folder);
        Plugin.Engine.Source.Element element = mock(Plugin.Engine.Source.Element.class);
        when(element.getName()).thenReturn("../illegal");
        target.write(Plugin.Engine.Source.Origin.NO_MANIFEST).retain(element);
    }
}
