package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PluginEngineDefaultOtherTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String FOO = "foo";

    @Test
    public void testScanFindsPlugin() throws Exception {
        File file = temporaryFolder.newFile();
        OutputStream outputStream = new FileOutputStream(file);
        try {
            outputStream.write(FOO.getBytes("UTF-8"));
        } finally {
            outputStream.close();
        }
        Set<String> plugins = Plugin.Engine.Default.scan(new PluginClassLoader(file));
        assertThat(plugins.size(), is(1));
        assertThat(plugins.contains(FOO), is(true));
    }

    @Test
    public void testScanFindsNoPluginForByteBuddy() throws Exception {
        Set<String> plugins = Plugin.Engine.Default.scan(ByteBuddy.class.getClassLoader());
        assertThat(plugins.size(), is(0));
    }

    @Test
    public void testMissingDependency() throws IOException {
        File jar = temporaryFolder.newFile("source.jar");
        OutputStream outputStream = new FileOutputStream(jar);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
            for (Class<?> type : new Class<?>[] {
                    PluginEngineDefaultOtherTest.class,
                    TypeWithDependency.class,
                    TypeWithoutDependency.class}) {
                jarOutputStream.putNextEntry(new JarEntry(type.getName().replace(".", "/") + ClassFileLocator.CLASS_FILE_EXTENSION));
                jarOutputStream.write(ClassFileLocator.ForClassLoader.read(type));
                jarOutputStream.closeEntry();
            }
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        Plugin.Engine.Summary summary = new Plugin.Engine.Default()
                .withoutErrorHandlers()
                .apply(jar, temporaryFolder.newFile("target.jar"), new Plugin.Factory.Simple(new MissingDependencyPlugin()));
        assertThat(summary.getFailed().size(), is(1));
        assertThat(summary.getFailed().keySet().iterator().next().getName(), is(TypeWithDependency.class.getName()));
        assertThat(summary.getTransformed().size(), is(1));
        assertThat(summary.getTransformed().get(0).getName(), is(TypeWithoutDependency.class.getName()));
    }

    private static class PluginClassLoader extends ClassLoader {

        private final File file;

        private PluginClassLoader(File file) {
            super(null);
            this.file = file;
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            if (name.equals(Plugin.Engine.Default.PLUGIN_FILE)) {
                return Collections.enumeration(Collections.singleton(file.toURI().toURL()));
            }
            return super.findResources(name);
        }
    }

    private static class MissingDependencyPlugin implements Plugin {

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            return builder.visit(new AsmVisitorWrapper.ForDeclaredFields().field(ElementMatchers.<FieldDescription.InDefinedShape>any()));
        }

        public boolean matches(TypeDescription target) {
            return target.represents(TypeWithoutDependency.class) || target.represents(TypeWithDependency.class);
        }

        public void close() {
            /* do nothing */
        }
    }

    public static class TypeDependency {
        /* empty */
    }

    public static class TypeWithDependency {

        @SuppressWarnings("unused")
        private static final TypeDependency MISSING_DEPENDENCY = new TypeDependency();
    }

    public static final class TypeWithoutDependency {
        /* empty */
    }
}
