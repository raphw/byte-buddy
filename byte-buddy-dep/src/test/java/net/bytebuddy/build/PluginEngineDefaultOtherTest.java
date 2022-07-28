package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
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
}
