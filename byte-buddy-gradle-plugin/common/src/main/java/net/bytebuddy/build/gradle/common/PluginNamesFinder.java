/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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