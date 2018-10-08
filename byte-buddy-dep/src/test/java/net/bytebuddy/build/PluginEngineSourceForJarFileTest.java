package net.bytebuddy.build;

import net.bytebuddy.utility.StreamDrainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PluginEngineSourceForJarFileTest {

    private File file;

    @Before
    public void setUp() throws Exception {
        file = File.createTempFile("foo", "bar");
    }

    @After
    public void tearDown() throws Exception {
        assertThat(file.delete(), is(true));
    }

    @Test
    public void testFile() throws Exception {
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));
        try {
            outputStream.putNextEntry(new JarEntry("Foo.class"));
            outputStream.write(new byte[]{1, 2, 3});
            outputStream.closeEntry();
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.ForJarFile(this.file).read();
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
            assertThat(element.resolveAs(JarEntry.class), notNullValue(JarEntry.class));
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
    }

    @Test
    public void testFileInSubFolder() throws Exception {
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));
        try {
            outputStream.putNextEntry(new JarEntry("bar/Foo.class"));
            outputStream.write(new byte[]{1, 2, 3});
            outputStream.closeEntry();
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.ForJarFile(this.file).read();
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
            assertThat(element.resolveAs(JarEntry.class), notNullValue(JarEntry.class));
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
    }

    @Test
    public void testManifest() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));
        try {
            outputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
            manifest.write(outputStream);
            outputStream.closeEntry();
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.ForJarFile(this.file).read();
        try {
            Manifest readManifest = origin.getManifest();
            assertThat(readManifest, notNullValue(Manifest.class));
            assertThat(readManifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION), is("1.0"));
        } finally {
            origin.close();
        }
    }
}
