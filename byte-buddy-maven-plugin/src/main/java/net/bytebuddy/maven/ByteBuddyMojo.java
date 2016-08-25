package net.bytebuddy.maven;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.pool.TypePool;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "byte-buddy",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ByteBuddyMojo extends AbstractMojo {

    private static final String CLASS_FILE_EXTENSION = ".class";

    @Component
    private MavenProject mavenProject;

    @Parameter(name = "rebase", defaultValue = "true", required = true)
    private boolean rebase;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            processClasses(new File(mavenProject.getBuild().getDirectory(), "classes"), mavenProject.getCompileClasspathElements());
        } catch (IOException exception) {
            throw new MojoExecutionException("Could not process classes", exception);
        } catch (DependencyResolutionRequiredException exception) {
            throw new MojoExecutionException("Dependencies not resolved", exception);
        }
    }

    private void processClasses(File target, List<? extends String> dependencies) throws IOException {
        getLog().info("Processing classes in: " + target);
        if (!target.isDirectory()) {
            throw new IOException("Source location does not exist: " + target);
        }
        ByteBuddy byteBuddy = new ByteBuddy();
        List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>(dependencies.size() + 1);
        classFileLocators.add(new ClassFileLocator.ForFolder(target));
        for (String dependency : dependencies) {
            File artifact = new File(dependency);
            classFileLocators.add(artifact.isFile()
                    ? ClassFileLocator.ForJarFile.of(artifact)
                    : new ClassFileLocator.ForFolder(artifact));
        }
        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
        try {
            TypePool typePool = new TypePool.Default.WithLazyResolution(new TypePool.CacheProvider.Simple(),
                    classFileLocator,
                    TypePool.Default.ReaderMode.FAST,
                    TypePool.ClassLoading.of(null));
            processDirectory(target, target, byteBuddy, classFileLocator, typePool);
        } finally {
            classFileLocator.close();
        }
    }

    private void processDirectory(File root, File directory, ByteBuddy byteBuddy, ClassFileLocator classFileLocator, TypePool typePool) throws IOException {
        File[] file = directory.listFiles();
        if (file != null) {
            for (File aFile : file) {
                if (aFile.isFile() && aFile.getName().endsWith(CLASS_FILE_EXTENSION)) {
                    processClassFile(root, root.toURI().relativize(aFile.toURI()).toString(), byteBuddy, classFileLocator, typePool);
                } else if (aFile.isDirectory()) {
                    processDirectory(root, aFile, byteBuddy, classFileLocator, typePool);
                }
            }
        }
    }

    private void processClassFile(File root, String classFile, ByteBuddy byteBuddy, ClassFileLocator classFileLocator, TypePool typePool) throws IOException {
        String typeName = classFile.replace('/', '.').substring(0, classFile.length() - CLASS_FILE_EXTENSION.length());
        getLog().debug("Processing class file: " + typeName);
        TypeDescription typeDescription = typePool.describe(typeName).resolve();
        DynamicType.Builder<?> builder = rebase
                ? byteBuddy.rebase(typeDescription, classFileLocator)
                : byteBuddy.redefine(typeDescription, classFileLocator);
        builder.make().saveIn(root); // TODO: Register processors.
    }
}