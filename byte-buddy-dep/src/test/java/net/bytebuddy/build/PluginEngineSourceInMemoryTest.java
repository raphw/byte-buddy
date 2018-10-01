package net.bytebuddy.build;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PluginEngineSourceInMemoryTest {

    @Test
    public void testNoManifest() throws Exception {
        Plugin.Engine.Source source = new Plugin.Engine.Source.InMemory(Collections.singletonMap("foo/Bar.class", new byte[]{1, 2, 3}));
        assertThat(source.getClassFileLocator().locate("foo.Bar").isResolved(), is(true));
        assertThat(source.getClassFileLocator().locate("foo.Bar").resolve(), is(new byte[]{1, 2, 3}));
        assertThat(source.getClassFileLocator().locate("qux.Baz").isResolved(), is(false));
        assertThat(source.getManifest(), nullValue(Manifest.class));
        Iterator<Plugin.Engine.Source.Element> iterator = source.iterator();
        assertThat(iterator.hasNext(), is(true));
        Plugin.Engine.Source.Element element = iterator.next();
        assertThat(element.getName(), is("foo/Bar.class"));
        assertThat(element.resolveAs(Object.class), nullValue(Object.class));
        assertThat(StreamDrainer.DEFAULT.drain(element.getInputStream()), is(new byte[]{1, 2, 3}));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testOfTypes() throws Exception {
        Plugin.Engine.Source source = Plugin.Engine.Source.InMemory.ofTypes(Foo.class);
        assertThat(source.getClassFileLocator().locate(Foo.class.getName()).isResolved(), is(true));
        assertThat(source.getClassFileLocator().locate(Foo.class.getName()).resolve(), is(ClassFileLocator.ForClassLoader.read(Foo.class)));
        assertThat(source.getClassFileLocator().locate("qux.Baz").isResolved(), is(false));
        assertThat(source.getManifest(), nullValue(Manifest.class));
        Iterator<Plugin.Engine.Source.Element> iterator = source.iterator();
        assertThat(iterator.hasNext(), is(true));
        Plugin.Engine.Source.Element element = iterator.next();
        assertThat(element.getName(), is(Foo.class.getName().replace('.', '/') + ".class"));
        assertThat(element.resolveAs(Object.class), nullValue(Object.class));
        assertThat(StreamDrainer.DEFAULT.drain(element.getInputStream()), is(ClassFileLocator.ForClassLoader.read(Foo.class)));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testManifest() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        manifest.write(outputStream);
        Plugin.Engine.Source source = new Plugin.Engine.Source.InMemory(Collections.singletonMap(Plugin.Engine.MANIFEST_LOCATION, outputStream.toByteArray()));
        assertThat(source.getClassFileLocator().locate("foo.Bar").isResolved(), is(false));
        assertThat(source.getManifest(), notNullValue(Manifest.class));
        assertThat(source.getManifest().getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION), is((Object) "1.0"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorNoRemoval() {
        Plugin.Engine.Source source = new Plugin.Engine.Source.InMemory(Collections.singletonMap("foo/Bar.class", new byte[]{1, 2, 3}));
        Iterator<Plugin.Engine.Source.Element> iterator = source.iterator();
        assertThat(iterator.next(), notNullValue(Plugin.Engine.Source.Element.class));
        iterator.remove();
    }

    static class Foo {
        /* empty */
    }
}
