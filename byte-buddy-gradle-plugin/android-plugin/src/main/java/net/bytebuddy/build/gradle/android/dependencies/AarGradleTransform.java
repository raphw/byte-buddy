package net.bytebuddy.build.gradle.android.dependencies;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class AarGradleTransform implements TransformAction<TransformParameters.None> {

    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs transformOutputs) {
        File input = getInputArtifact().get().getAsFile();
        String outputName = input.getName().replaceAll("\\.aar$", ".jar");
        try (ZipFile zipFile = new ZipFile(input)) {
            ZipEntry entry = zipFile.getEntry("classes.jar");
            InputStream inputStream = zipFile.getInputStream(entry);
            Files.copy(inputStream, transformOutputs.file(outputName).toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}