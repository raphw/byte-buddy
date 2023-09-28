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
package net.bytebuddy.build.gradle.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipException;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.utility.nullability.MaybeNull;

/**
 * Byte Buddy compilation target that merges an enhanced android app's runtime classpath into a jar file.
 */
final class TargetForAndroidAppJarFile extends Plugin.Engine.Target.ForJarFile {
    private final File file;

    public TargetForAndroidAppJarFile(File file) {
        super(file);
        this.file = file;
    }

    @Override
    public Sink write(@MaybeNull Manifest manifest) throws IOException {
        return manifest == null
                ? new ForAndroidAppOutputStream(new JarOutputStream(new FileOutputStream(file)))
                : new ForAndroidAppOutputStream(new JarOutputStream(new FileOutputStream(file), manifest));
    }

    static final class ForAndroidAppOutputStream extends Sink.ForJarOutputStream {
        private final JarOutputStream outputStream;

        public ForAndroidAppOutputStream(JarOutputStream outputStream) {
            super(outputStream);
            this.outputStream = outputStream;
        }

        @Override
        public void retain(Plugin.Engine.Source.Element element) throws IOException {
            JarEntry entry = element.resolveAs(JarEntry.class);
            if (entry != null && entry.isDirectory()) {
                // Folders give a lot of "duplicated" errors.
                return;
            }

            try {
                outputStream.putNextEntry(new JarEntry(element.getName()));
                InputStream inputStream = element.getInputStream();
                try {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                } finally {
                    inputStream.close();
                }
                outputStream.closeEntry();
            } catch (ZipException e) {
                // Ignore duplicated files inside "META-INF"

                if (!element.getName().startsWith("META-INF")) {
                    throw e;
                }
            }
        }
    }
}
