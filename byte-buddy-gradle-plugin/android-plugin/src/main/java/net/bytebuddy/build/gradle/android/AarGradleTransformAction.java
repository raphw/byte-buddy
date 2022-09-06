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

import org.gradle.api.Action;
import org.gradle.api.artifacts.transform.*;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This {@link TransformAction} takes care of providing JARs embedded in AARs dependencies for Byte Buddy's configuration.
 */
public abstract class AarGradleTransformAction implements TransformAction<TransformParameters.None> {

    /**
     * The AAR file format extension.
     */
    private static final String AAR_FILE_EXTENSION = "aar";

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
        String outputName = input.getName().replaceAll("\\." + AAR_FILE_EXTENSION + "$", ".jar");
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

    /**
     * A configuration action for transforming an AAR file.
     */
    protected static class ConfigurationAction implements Action<TransformSpec<TransformParameters.None>> {

        /**
         * {@inheritDoc}
         */
        public void execute(TransformSpec<TransformParameters.None> spec) {
            spec.getFrom().attribute(ByteBuddyAndroidPlugin.ARTIFACT_TYPE_ATTRIBUTE, AAR_FILE_EXTENSION);
            spec.getTo().attribute(ByteBuddyAndroidPlugin.ARTIFACT_TYPE_ATTRIBUTE, "bytebuddy-jar");
        }
    }
}
