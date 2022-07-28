package net.bytebuddy.build.gradle.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

public final class PluginNamesFinder {

    public void find(ClassLoader classLoader, Listener listener) throws IOException {
        Enumeration<URL> plugins = classLoader.getResources("META-INF/net.bytebuddy/build.plugins");
        while (plugins.hasMoreElements()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(plugins.nextElement().openStream(), "UTF-8"));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    listener.onClassNameFound(line);
                }
            } finally {
                reader.close();
            }
        }
    }

    public interface Listener {
        void onClassNameFound(String className);
    }
}