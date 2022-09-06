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

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This {@link TransformAction} takes care of providing JARs embedded in AARs dependencies for Byte Buddy's configuration.
 */
public abstract class AarGradleTransformAction implements TransformAction<TransformParameters.None> {

    /**
     * Returns the input artifact.
     *
     * @return The input artifact.
     */
    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    /**
     * {@inheritDoc}
     */
    public void transform(TransformOutputs transformOutputs) {
        File input = getInputArtifact().get().getAsFile();
        String outputName = input.getName().replaceAll("\\.aar$", ".jar");
        try {
            ZipFile zipFile = new ZipFile(input);
            try {
                ZipEntry entry = zipFile.getEntry("classes.jar");
                InputStream inputStream = zipFile.getInputStream(entry);
                try {
                    OutputStream outputStream = new FileOutputStream(transformOutputs.file(outputName));
                    try {
                        byte[] buffer = new byte[1024 * 8];
                        int length;
                        while ((length = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, length);
                        }
                    } finally {
                        outputStream.close();
                    }
                } finally {
                    inputStream.close();
                }
            } finally {
                zipFile.close();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to transform " + getInputArtifact(), exception);
        }
    }
}
