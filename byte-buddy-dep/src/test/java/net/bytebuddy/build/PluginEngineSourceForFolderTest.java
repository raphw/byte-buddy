package net.bytebuddy.build;

import net.bytebuddy.utility.StreamDrainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PluginEngineSourceForFolderTest {

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
    public void testEmpty() throws Exception {
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.ForFolder(folder).read();
        try {
            assertThat(origin.getManifest(), nullValue(Manifest.class));
            assertThat(origin.getClassFileLocator().locate(Object.class.getName()).isResolved(), is(false));
            assertThat(origin.iterator().hasNext(), is(false));
        } finally {
            origin.close();
        }
    }

    @Test
    public void testFile() throws Exception {
        File file = new File(folder, "Foo.class");
        OutputStream outputStream = new FileOutputStream(file);
        try {
            outputStream.write(new byte[]{1, 2, 3});
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.ForFolder(folder).read();
        try {
            assertThat(origin.getManifest(), nullValue(Manifest.class));
            assertThat(origin.getClassFileLocator().locate("Foo").isResolved(), is(true));
            assertThat(origin.getClassFileLocator().locate("Foo").resolve(), is(new byte[]{1, 2, 3}));
            assertThat(origin.getClassFileLocator().locate("Bar").isResolved(), is(false));
            Iterator<Plugin.Engine.Source.Element> iterator = origin.iterator();
            assertThat(iterator.hasNext(), is(true));
            Plugin.Engine.Source.Element element = iterator.next();
            assertThat(element.getName(), is("Foo.class"));
            assertThat(element.resolveAs(Object.class), nullValue(Object.class));
            assertThat(element.resolveAs(File.class), is(file));
            InputStream inputStream = element.getInputStream();
            try {
                assertThat(StreamDrainer.DEFAULT.drain(inputStream), is(new byte[]{1, 2, 3}));
            } finally {
                inputStream.close();
            }
            assertThat(iterator.hasNext(), is(false));
        } finally {
            origin.close();
        }
        assertThat(file.delete(), is(true));
    }

    @Test
    public void testFileInSubFolder() throws Exception {
        File file = new File(folder, "bar/Foo.class");
        assertThat(file.getParentFile().mkdir(), is(true));
        OutputStream outputStream = new FileOutputStream(file);
        try {
            outputStream.write(new byte[]{1, 2, 3});
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.ForFolder(folder).read();
        try {
            assertThat(origin.getManifest(), nullValue(Manifest.class));
            assertThat(origin.getClassFileLocator().locate("bar.Foo").isResolved(), is(true));
            assertThat(origin.getClassFileLocator().locate("bar.Foo").resolve(), is(new byte[]{1, 2, 3}));
            assertThat(origin.getClassFileLocator().locate("Bar").isResolved(), is(false));
            Iterator<Plugin.Engine.Source.Element> iterator = origin.iterator();
            assertThat(iterator.hasNext(), is(true));
            Plugin.Engine.Source.Element element = iterator.next();
            assertThat(element.getName(), is("bar/Foo.class"));
            assertThat(element.resolveAs(Object.class), nullValue(Object.class));
            assertThat(element.resolveAs(File.class), is(file));
            InputStream inputStream = element.getInputStream();
            try {
                assertThat(StreamDrainer.DEFAULT.drain(inputStream), is(new byte[]{1, 2, 3}));
            } finally {
                inputStream.close();
            }
            assertThat(iterator.hasNext(), is(false));
        } finally {
            origin.close();
        }
        assertThat(file.delete(), is(true));
        assertThat(file.getParentFile().delete(), is(true));
    }

    @Test
    public void testManifest() throws Exception {
        File file = new File(folder, JarFile.MANIFEST_NAME);
        assertThat(file.getParentFile().mkdir(), is(true));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        OutputStream outputStream = new FileOutputStream(file);
        try {
            manifest.write(outputStream);
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.ForFolder(folder).read();
        try {
            Manifest readManifest = origin.getManifest();
            assertThat(readManifest, notNullValue(Manifest.class));
            assertThat(readManifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION), is("1.0"));
        } finally {
            origin.close();
        }
        assertThat(file.delete(), is(true));
        assertThat(file.getParentFile().delete(), is(true));
    }
}
