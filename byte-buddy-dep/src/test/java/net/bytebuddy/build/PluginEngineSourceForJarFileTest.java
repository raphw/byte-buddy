package net.bytebuddy.build;

import net.bytebuddy.utility.StreamDrainer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.jar.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class PluginEngineSourceForJarFileTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File file;

    @Before
    public void setUp() throws Exception {
        file = temporaryFolder.newFile();
    }

    @Test
    public void testFile() throws Exception {
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
            jarOutputStream.putNextEntry(new JarEntry("Foo.class"));
            jarOutputStream.write(new byte[]{1, 2, 3});
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.ForJarFile(this.file).read();
        try {
            assertThat(origin.getManifest(), nullValue(Manifest.class));
            assertThat(origin.toClassFileLocator(null).locate("Foo").isResolved(), is(true));
            assertThat(origin.toClassFileLocator(null).locate("Foo").resolve(), is(new byte[]{1, 2, 3}));
            assertThat(origin.toClassFileLocator(null).locate("Bar").isResolved(), is(false));
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
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
            jarOutputStream.putNextEntry(new JarEntry("bar/Foo.class"));
            jarOutputStream.write(new byte[]{1, 2, 3});
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.ForJarFile(this.file).read();
        try {
            assertThat(origin.getManifest(), nullValue(Manifest.class));
            assertThat(origin.toClassFileLocator(null).locate("bar.Foo").isResolved(), is(true));
            assertThat(origin.toClassFileLocator(null).locate("bar.Foo").resolve(), is(new byte[]{1, 2, 3}));
            assertThat(origin.toClassFileLocator(null).locate("Bar").isResolved(), is(false));
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
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
            jarOutputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
            manifest.write(jarOutputStream);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
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
