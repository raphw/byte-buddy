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
package net.bytebuddy.build.gradle.android.connector.adapter.legacy.transform;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.SecondaryFile;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;
import net.bytebuddy.build.gradle.android.utils.Many;
import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class LegacyAndroidAppTransform extends Transform {

    private final AndroidTransformation transformation;
    private final AppExtension appExtension;
    private final Configuration byteBuddyDependenciesConfiguration;

    private static final String OUTPUT_DIR_NAME = "byteBuddyOutput";

    public LegacyAndroidAppTransform(AndroidTransformation transformation, AppExtension appExtension, Configuration byteBuddyDependenciesConfiguration) {
        this.transformation = transformation;
        this.appExtension = appExtension;
        this.byteBuddyDependenciesConfiguration = byteBuddyDependenciesConfiguration;
    }

    @Override
    public String getName() {
        return "byteBuddy";
    }

    public Set<QualifiedContent.ContentType> getInputTypes() {
        return Many.setOf(QualifiedContent.DefaultContentType.CLASSES);
    }

    @Override
    public Collection<SecondaryFile> getSecondaryFiles() {
        return Many.listOf(SecondaryFile.nonIncremental(byteBuddyDependenciesConfiguration));
    }

    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return Many.setOf(
                QualifiedContent.Scope.PROJECT,
                QualifiedContent.Scope.SUB_PROJECTS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES
        );
    }

    @Override
    public Set<QualifiedContent.Scope> getReferencedScopes() {
        return Many.setOf(
                QualifiedContent.Scope.SUB_PROJECTS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES
        );
    }

    @Override
    public void transform(TransformInvocation transformInvocation) {
        ApplicationVariant variant = getVariantByName(transformInvocation.getContext().getVariantName());
        AndroidTransformation.Input input = new AndroidTransformation.Input(
                getClasspathFromTransformInput(transformInvocation.getInputs()),
                getClasspathFromTransformInput(transformInvocation.getReferencedInputs()),
                Many.toSet(appExtension.getBootClasspath()),
                byteBuddyDependenciesConfiguration.getFiles(),
                getJavaTargetCompatibilityVersion(variant)
        );
        transformation.transform(input, getOutputFolder(transformInvocation));
    }

    private Set<File> getClasspathFromTransformInput(Collection<TransformInput> inputs) {
        Set<File> classpath = new HashSet<>();

        inputs.forEach(input -> {
            input.getDirectoryInputs().forEach(it -> classpath.add(it.getFile()));
            input.getJarInputs().forEach(it -> classpath.add(it.getFile()));
        });

        return classpath;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    private ApplicationVariant getVariantByName(String name) {
        return Many.find(appExtension.getApplicationVariants(), applicationVariant -> applicationVariant.getName().equals(name));
    }

    private int getJavaTargetCompatibilityVersion(ApplicationVariant variant) {
        String targetCompatibilityStr = variant.getJavaCompileProvider().get().getTargetCompatibility();
        return javaVersionToInt(JavaVersion.toVersion(targetCompatibilityStr));
    }

    private int javaVersionToInt(JavaVersion javaVersion) {
        return Integer.parseInt(javaVersion.getMajorVersion());
    }

    private File getOutputFolder(TransformInvocation transformInvocation) {
        return transformInvocation.getOutputProvider().getContentLocation(
                OUTPUT_DIR_NAME,
                getInputTypes(),
                getScopes(),
                Format.DIRECTORY
        );
    }
}