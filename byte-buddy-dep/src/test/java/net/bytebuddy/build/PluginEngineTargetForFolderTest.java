package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.*;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class PluginEngineTargetForFolderTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private File folder;

    @Before
    public void setUp() throws Exception {
        folder = File.createTempFile("foo", "bar");
        assertThat(folder.delete(), is(true));
        assertThat(folder.mkdir(), is(true));
    }

    @After
    public void tearDown() throws Exception {
        assertThat(folder.delete(), is(true));
    }

    @Test
    public void testWriteType() throws Exception {
        Plugin.Engine.Target target = new Plugin.Engine.Target.ForFolder(folder);
        Plugin.Engine.Target.Sink sink = target.write(Plugin.Engine.Source.Origin.NO_MANIFEST);
        try {
            sink.store(Collections.singletonMap(TypeDescription.OBJECT, new byte[]{1, 2, 3}));
        } finally {
            sink.close();
        }
        File file = new File(folder, TypeDescription.OBJECT.getInternalName() + ".class");
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
        File original = File.createTempFile("qux", "baz");
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
        verify(element, times(Plugin.Engine.Target.ForFolder.DISPATCHER.isAlive() ? 0 : 1)).getInputStream();
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

    @Test
    @JavaVersionRule.Enforce(7)
    public void testCanUseNio2() {
        assertThat(Plugin.Engine.Target.ForFolder.DISPATCHER.isAlive(), is(true));
    }
}
