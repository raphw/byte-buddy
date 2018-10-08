package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PluginEngineDefaultTest {

    private static final String FOO = "foo";

    @Test
    public void testSimpleTransformation() throws Exception {
        Plugin.Engine.Listener listener = mock(Plugin.Engine.Listener.class);
        Plugin plugin = new SimplePlugin();
        Plugin.Engine.Source source = Plugin.Engine.Source.InMemory.ofTypes(Sample.class);
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(listener)
                .with(ClassFileLocator.ForClassLoader.of(SimplePlugin.class.getClassLoader()))
                .apply(source, target, new Plugin.Factory.Simple(plugin));
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, target.toTypeMap());
        Class<?> type = classLoader.loadClass(Sample.class.getName());
        assertThat(type.getDeclaredField(FOO).getType(), is((Object) Void.class));
        assertThat(summary.getTransformed(), hasItems(TypeDescription.ForLoadedType.of(Sample.class)));
        assertThat(summary.getFailed().size(), is(0));
        assertThat(summary.getUnresolved().size(), is(0));
        verify(listener).onManifest(Plugin.Engine.Source.Origin.NO_MANIFEST);
        verify(listener).onDiscovery(Sample.class.getName());
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(Sample.class), plugin);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(Sample.class), Collections.singletonList(plugin));
        verify(listener).onComplete(TypeDescription.ForLoadedType.of(Sample.class));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testSimpleTransformationIgnoredByPlugin() throws Exception {
        Plugin.Engine.Listener listener = mock(Plugin.Engine.Listener.class);
        Plugin plugin = new IgnoringPlugin();
        Plugin.Engine.Source source = Plugin.Engine.Source.InMemory.ofTypes(Sample.class);
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(listener)
                .with(ClassFileLocator.ForClassLoader.of(IgnoringPlugin.class.getClassLoader()))
                .apply(source, target, new Plugin.Factory.Simple(plugin));
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, target.toTypeMap());
        Class<?> type = classLoader.loadClass(Sample.class.getName());
        assertThat(type.getDeclaredFields().length, is(0));
        assertThat(summary.getTransformed().size(), is(0));
        assertThat(summary.getFailed().size(), is(0));
        assertThat(summary.getUnresolved().size(), is(0));
        verify(listener).onManifest(Plugin.Engine.Source.Origin.NO_MANIFEST);
        verify(listener).onDiscovery(Sample.class.getName());
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(Sample.class), plugin);
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(Sample.class), Collections.singletonList(plugin));
        verify(listener).onComplete(TypeDescription.ForLoadedType.of(Sample.class));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testSimpleTransformationIgnoredByMatcher() throws Exception {
        Plugin.Engine.Listener listener = mock(Plugin.Engine.Listener.class);
        Plugin plugin = new SimplePlugin();
        Plugin.Engine.Source source = Plugin.Engine.Source.InMemory.ofTypes(Sample.class);
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(listener)
                .with(ClassFileLocator.ForClassLoader.of(SimplePlugin.class.getClassLoader()))
                .ignore(ElementMatchers.is(Sample.class))
                .apply(source, target, new Plugin.Factory.Simple(plugin));
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, target.toTypeMap());
        Class<?> type = classLoader.loadClass(Sample.class.getName());
        assertThat(type.getDeclaredFields().length, is(0));
        assertThat(summary.getTransformed().size(), is(0));
        assertThat(summary.getFailed().size(), is(0));
        assertThat(summary.getUnresolved().size(), is(0));
        verify(listener).onManifest(Plugin.Engine.Source.Origin.NO_MANIFEST);
        verify(listener).onDiscovery(Sample.class.getName());
        verify(listener).onIgnored(TypeDescription.ForLoadedType.of(Sample.class), Collections.singletonList(plugin));
        verify(listener).onComplete(TypeDescription.ForLoadedType.of(Sample.class));
        verifyNoMoreInteractions(listener);
    }

    @Test(expected = IllegalStateException.class)
    public void testSimpleTransformationError() throws Exception {
        Plugin.Engine.Source source = Plugin.Engine.Source.InMemory.ofTypes(Sample.class);
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        new Plugin.Engine.Default()
                .with(ClassFileLocator.ForClassLoader.of(FailingPlugin.class.getClassLoader()))
                .apply(source, target, new Plugin.Factory.Simple(new FailingPlugin(new RuntimeException())));
    }

    @Test
    public void testSimpleTransformationErrorIgnored() throws Exception {
        Plugin.Engine.Listener listener = mock(Plugin.Engine.Listener.class);
        RuntimeException exception = new RuntimeException();
        Plugin plugin = new FailingPlugin(exception);
        Plugin.Engine.Source source = Plugin.Engine.Source.InMemory.ofTypes(Sample.class);
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(listener)
                .withoutErrorHandlers()
                .with(ClassFileLocator.ForClassLoader.of(FailingPlugin.class.getClassLoader()))
                .apply(source, target, new Plugin.Factory.Simple(plugin));
        assertThat(summary.getTransformed().size(), is(0));
        assertThat(summary.getFailed().size(), is(1));
        assertThat(summary.getFailed().containsKey(TypeDescription.ForLoadedType.of(Sample.class)), is(true));
        assertThat(summary.getUnresolved().size(), is(0));
        assertThat(target.getStorage().size(), is(1));
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, target.toTypeMap());
        Class<?> type = classLoader.loadClass(Sample.class.getName());
        assertThat(type.getDeclaredFields().length, is(0));
        verify(listener).onManifest(Plugin.Engine.Source.Origin.NO_MANIFEST);
        verify(listener).onDiscovery(Sample.class.getName());
        verify(listener).onError(TypeDescription.ForLoadedType.of(Sample.class), plugin, exception);
        verify(listener).onError(TypeDescription.ForLoadedType.of(Sample.class), Collections.<Throwable>singletonList(exception));
        verify(listener).onComplete(TypeDescription.ForLoadedType.of(Sample.class));
        verify(listener).onError(Collections.singletonMap(TypeDescription.ForLoadedType.of(Sample.class), Collections.<Throwable>singletonList(exception)));
        verify(listener).onError(plugin, exception);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testLiveInitializer() throws Exception {
        Plugin.Engine.Listener listener = mock(Plugin.Engine.Listener.class);
        Plugin plugin = new LiveInitializerPlugin();
        Plugin.Engine.Source source = Plugin.Engine.Source.InMemory.ofTypes(Sample.class);
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(listener)
                .withoutErrorHandlers()
                .with(ClassFileLocator.ForClassLoader.of(SimplePlugin.class.getClassLoader()))
                .apply(source, target, new Plugin.Factory.Simple(plugin));
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, target.toTypeMap());
        Class<?> type = classLoader.loadClass(Sample.class.getName());
        assertThat(type.getDeclaredField(FOO).getType(), is((Object) Void.class));
        assertThat(summary.getTransformed(), hasItems(TypeDescription.ForLoadedType.of(Sample.class)));
        assertThat(summary.getFailed().size(), is(0));
        assertThat(summary.getUnresolved().size(), is(0));
        verify(listener).onManifest(Plugin.Engine.Source.Origin.NO_MANIFEST);
        verify(listener).onDiscovery(Sample.class.getName());
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(Sample.class), plugin);
        verify(listener).onTransformation(TypeDescription.ForLoadedType.of(Sample.class), Collections.singletonList(plugin));
        verify(listener).onLiveInitializer(TypeDescription.ForLoadedType.of(Sample.class), TypeDescription.ForLoadedType.of(Sample.class));
        verify(listener).onComplete(TypeDescription.ForLoadedType.of(Sample.class));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testUnresolved() throws Exception {
        Plugin.Engine.Listener listener = mock(Plugin.Engine.Listener.class);
        Plugin plugin = new SimplePlugin();
        Plugin.Engine.Source source = new Plugin.Engine.Source.InMemory(Collections.singletonMap(
                Sample.class.getName().replace('.', '/') + ".class",
                ClassFileLocator.ForClassLoader.read(Sample.class))) {
            @Override
            public ClassFileLocator getClassFileLocator() {
                return ClassFileLocator.NoOp.INSTANCE;
            }
        };
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(listener)
                .withoutErrorHandlers()
                .apply(source, target, new Plugin.Factory.Simple(plugin));
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, target.toTypeMap());
        Class<?> type = classLoader.loadClass(Sample.class.getName());
        assertThat(type.getDeclaredFields().length, is(0));
        assertThat(summary.getTransformed().size(), is(0));
        assertThat(summary.getFailed().size(), is(0));
        assertThat(summary.getUnresolved().size(), is(1));
        assertThat(summary.getUnresolved().contains(Sample.class.getName()), is(true));
        verify(listener).onManifest(Plugin.Engine.Source.Origin.NO_MANIFEST);
        verify(listener).onDiscovery(Sample.class.getName());
        verify(listener).onUnresolved(Sample.class.getName());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testResource() throws Exception {
        Plugin.Engine.Listener listener = mock(Plugin.Engine.Listener.class);
        Plugin.Engine.Source source = new Plugin.Engine.Source.InMemory(Collections.singletonMap(FOO, new byte[]{1, 2, 3}));
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(listener)
                .with(ClassFileLocator.ForClassLoader.of(SimplePlugin.class.getClassLoader()))
                .apply(source, target, new Plugin.Factory.Simple(new SimplePlugin()));
        assertThat(summary.getTransformed().size(), is(0));
        assertThat(summary.getFailed().size(), is(0));
        assertThat(summary.getUnresolved().size(), is(0));
        verify(listener).onManifest(Plugin.Engine.Source.Origin.NO_MANIFEST);
        verify(listener).onResource(FOO);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testManifest() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        manifest.write(outputStream);
        Plugin.Engine.Listener listener = mock(Plugin.Engine.Listener.class);
        Plugin.Engine.Source source = new Plugin.Engine.Source.InMemory(Collections.singletonMap(JarFile.MANIFEST_NAME, outputStream.toByteArray()));
        Plugin.Engine.Target.InMemory target = new Plugin.Engine.Target.InMemory();
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .with(listener)
                .with(ClassFileLocator.ForClassLoader.of(SimplePlugin.class.getClassLoader()))
                .apply(source, target, new Plugin.Factory.Simple(new SimplePlugin()));
        assertThat(summary.getTransformed().size(), is(0));
        assertThat(summary.getFailed().size(), is(0));
        assertThat(summary.getUnresolved().size(), is(0));
        verify(listener).onManifest(manifest);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testImplicitFileInput() throws Exception {
        File file = File.createTempFile("foo", "bar");
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));
        try {
            outputStream.putNextEntry(new JarEntry("dummy"));
            outputStream.write(new byte[]{1, 2, 3});
        } finally {
            outputStream.close();
        }
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

    @Test
    public void testOfEntryPoint() {
        EntryPoint entryPoint = mock(EntryPoint.class);
        ClassFileVersion classFileVersion = mock(ClassFileVersion.class);
        MethodNameTransformer methodNameTransformer = mock(MethodNameTransformer.class);
        ByteBuddy byteBuddy = mock(ByteBuddy.class);
        when(entryPoint.byteBuddy(classFileVersion)).thenReturn(byteBuddy);
        assertThat(Plugin.Engine.Default.of(entryPoint, classFileVersion, methodNameTransformer), hasPrototype(new Plugin.Engine.Default()
                .with(byteBuddy)
                .with(new Plugin.Engine.TypeStrategy.ForEntryPoint(entryPoint, methodNameTransformer))));
        verify(entryPoint).byteBuddy(classFileVersion);
        verifyNoMoreInteractions(entryPoint);
    }

    @Test
    public void testMain() throws Exception {
        File source = File.createTempFile("foo", "bar"), target = File.createTempFile("qux", "baz");
        assertThat(target.delete(), is(true));
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(source));
        try {
            outputStream.putNextEntry(new JarEntry("dummy"));
            outputStream.write(new byte[]{1, 2, 3});
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Default.main(source.getAbsolutePath(), target.getAbsolutePath(), Plugin.NoOp.class.getName());
        assertThat(target.isFile(), is(true));
        assertThat(target.delete(), is(true));
    }

    @Test
    public void testConfiguration() {
        Plugin.Engine.ErrorHandler errorHandler = mock(Plugin.Engine.ErrorHandler.class);
        Plugin.Engine.TypeStrategy typeStrategy = mock(Plugin.Engine.TypeStrategy.class);
        Plugin.Engine.PoolStrategy poolStrategy = mock(Plugin.Engine.PoolStrategy.class);
        assertThat(new Plugin.Engine.Default()
                .with(poolStrategy)
                .with(typeStrategy)
                .withErrorHandlers(errorHandler), hasPrototype(new Plugin.Engine.Default()
                .with(poolStrategy)
                .with(typeStrategy)
                .withErrorHandlers(errorHandler)));
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

    private static class LiveInitializerPlugin implements Plugin {

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            return builder.defineField(FOO, Void.class).initializer(new LoadedTypeInitializer() {
                public void onLoad(Class<?> type) {
                    /* do nothing */
                }

                public boolean isAlive() {
                    return true;
                }
            });
        }

        public boolean matches(TypeDescription target) {
            return target.represents(Sample.class);
        }

        public void close() {
            /* empty */
        }
    }

    private static class IgnoringPlugin implements Plugin {

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            throw new AssertionError();
        }

        public boolean matches(TypeDescription target) {
            return false;
        }

        public void close() {
            /* empty */
        }
    }

    private static class FailingPlugin implements Plugin {

        private final RuntimeException exception;

        public FailingPlugin(RuntimeException exception) {
            this.exception = exception;
        }

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            throw exception;
        }

        public boolean matches(TypeDescription target) {
            return target.represents(Sample.class);
        }

        public void close() {
            throw exception;
        }
    }
}
