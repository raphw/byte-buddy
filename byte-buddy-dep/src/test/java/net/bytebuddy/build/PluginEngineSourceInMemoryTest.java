package net.bytebuddy.build;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PluginEngineSourceInMemoryTest {

    @Test
    public void testNoManifest() throws Exception {
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.InMemory(Collections.singletonMap("foo/Bar.class", new byte[]{1, 2, 3})).read();
        try {
            assertThat(origin.getClassFileLocator().locate("foo.Bar").isResolved(), is(true));
            assertThat(origin.getClassFileLocator().locate("foo.Bar").resolve(), is(new byte[]{1, 2, 3}));
            assertThat(origin.getClassFileLocator().locate("qux.Baz").isResolved(), is(false));
            assertThat(origin.getManifest(), nullValue(Manifest.class));
            Iterator<Plugin.Engine.Source.Element> iterator = origin.iterator();
            assertThat(iterator.hasNext(), is(true));
            Plugin.Engine.Source.Element element = iterator.next();
            assertThat(element.getName(), is("foo/Bar.class"));
            assertThat(element.resolveAs(Object.class), nullValue(Object.class));
            assertThat(StreamDrainer.DEFAULT.drain(element.getInputStream()), is(new byte[]{1, 2, 3}));
            assertThat(iterator.hasNext(), is(false));
        } finally {
            origin.close();
        }
    }

    @Test
    public void testOfTypes() throws Exception {
        Plugin.Engine.Source.Origin origin = Plugin.Engine.Source.InMemory.ofTypes(Foo.class).read();
        try {
            assertThat(origin.getClassFileLocator().locate(Foo.class.getName()).isResolved(), is(true));
            assertThat(origin.getClassFileLocator().locate(Foo.class.getName()).resolve(), is(ClassFileLocator.ForClassLoader.read(Foo.class)));
            assertThat(origin.getClassFileLocator().locate("qux.Baz").isResolved(), is(false));
            assertThat(origin.getManifest(), nullValue(Manifest.class));
            Iterator<Plugin.Engine.Source.Element> iterator = origin.iterator();
            assertThat(iterator.hasNext(), is(true));
            Plugin.Engine.Source.Element element = iterator.next();
            assertThat(element.getName(), is(Foo.class.getName().replace('.', '/') + ".class"));
            assertThat(element.resolveAs(Object.class), nullValue(Object.class));
            assertThat(StreamDrainer.DEFAULT.drain(element.getInputStream()), is(ClassFileLocator.ForClassLoader.read(Foo.class)));
            assertThat(iterator.hasNext(), is(false));
        } finally {
            origin.close();
        }
    }

    @Test
    public void testManifest() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        manifest.write(outputStream);
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.InMemory(Collections.singletonMap(JarFile.MANIFEST_NAME, outputStream.toByteArray())).read();
        try {
            assertThat(origin.getClassFileLocator().locate("foo.Bar").isResolved(), is(false));
            assertThat(origin.getManifest(), notNullValue(Manifest.class));
            assertThat(origin.getManifest().getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION), is((Object) "1.0"));
        } finally {
            origin.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorNoRemoval() throws Exception {
        Plugin.Engine.Source.Origin origin = new Plugin.Engine.Source.InMemory(Collections.singletonMap("foo/Bar.class", new byte[]{1, 2, 3})).read();
        try {
            Iterator<Plugin.Engine.Source.Element> iterator = origin.iterator();
            assertThat(iterator.next(), notNullValue(Plugin.Engine.Source.Element.class));
            iterator.remove();
        } finally {
            origin.close();
        }
    }

    static class Foo {
        /* empty */
    }
}
