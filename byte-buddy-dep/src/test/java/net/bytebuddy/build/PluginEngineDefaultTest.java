package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.jar.JarOutputStream;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class PluginEngineDefaultTest {

    private static final String FOO = "foo";

    @Test
    public void testSimpleTransformation() throws Exception {
        Plugin.Engine.Source source = Plugin.Engine.Source.InMemory.ofTypes(Sample.class);
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(ClassFileLocator.ForClassLoader.of(SimplePlugin.class.getClassLoader()))
                .apply(source, target, new Plugin.Factory.Simple(new SimplePlugin()));
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, target.toTypeMap());
        Class<?> type = classLoader.loadClass(Sample.class.getName());
        assertThat(type.getDeclaredField(FOO).getType(), is((Object) Void.class));
        assertThat(summary.getTransformed(), hasItems(TypeDescription.ForLoadedType.of(Sample.class)));
        assertThat(summary.getFailed().size(), is(0));
        assertThat(summary.getUnresolved().size(), is(0));
    }

    @Test
    public void testSimpleTransformationIgnored() throws Exception {
        Plugin.Engine.Source source = Plugin.Engine.Source.InMemory.ofTypes(Sample.class);
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(ClassFileLocator.ForClassLoader.of(SimplePlugin.class.getClassLoader()))
                .ignore(ElementMatchers.is(Sample.class))
                .apply(source, target, new Plugin.Factory.Simple(new SimplePlugin()));
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, target.toTypeMap());
        Class<?> type = classLoader.loadClass(Sample.class.getName());
        assertThat(type.getDeclaredFields().length, is(0));
        assertThat(summary.getTransformed().size(), is(0));
        assertThat(summary.getFailed().size(), is(0));
        assertThat(summary.getUnresolved().size(), is(0));
    }

    @Test
    public void testImplicitFileInput() throws Exception {
        File file = File.createTempFile("foo", "bar");
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));
        outputStream.close();
        Plugin.Engine engine = spy(new Plugin.Engine.Default());
        doAnswer(new Answer<Plugin.Engine.Summary>() {
            public Plugin.Engine.Summary answer(InvocationOnMock invocationOnMock) {
                if (!(invocationOnMock.getArgument(0) instanceof Plugin.Engine.Source.ForJarFile)) {
                    throw new AssertionError();
                } else if (!(invocationOnMock.getArgument(1) instanceof Plugin.Engine.Target.ForJarFile)) {
                    throw new AssertionError();
                }
                return null;
            }
        }).when(engine).apply(any(Plugin.Engine.Source.class), any(Plugin.Engine.Target.class), ArgumentMatchers.<Plugin.Factory>anyList());
        assertThat(engine.apply(file, file), nullValue(Plugin.Engine.Summary.class));
        assertThat(file.delete(), is(true));
    }

    @Test
    public void testImplicitFolderInput() throws Exception {
        File file = File.createTempFile("foo", "bar");
        assertThat(file.delete(), is(true));
        assertThat(file.mkdir(), is(true));
        Plugin.Engine engine = spy(new Plugin.Engine.Default());
        doAnswer(new Answer<Plugin.Engine.Summary>() {
            public Plugin.Engine.Summary answer(InvocationOnMock invocationOnMock) {
                if (!(invocationOnMock.getArgument(0) instanceof Plugin.Engine.Source.ForFolder)) {
                    throw new AssertionError();
                } else if (!(invocationOnMock.getArgument(1) instanceof Plugin.Engine.Target.ForFolder)) {
                    throw new AssertionError();
                }
                return null;
            }
        }).when(engine).apply(any(Plugin.Engine.Source.class), any(Plugin.Engine.Target.class), ArgumentMatchers.<Plugin.Factory>anyList());
        assertThat(engine.apply(file, file), nullValue(Plugin.Engine.Summary.class));
        assertThat(file.delete(), is(true));
    }

    private static class Sample {
        /* empty */
    }

    private static class SimplePlugin implements Plugin {

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            return builder.defineField(FOO, Void.class);
        }

        public boolean matches(TypeDescription target) {
            return target.represents(Sample.class);
        }

        public void close() {
            /* empty */
        }
    }
}
