package net.bytebuddy.maven;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.Plugin;
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

@Mojo(name = "transform",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE) // TODO: separate mojo
public class ByteBuddyMojo extends AbstractMojo {

    private static final String CLASS_FILE_EXTENSION = ".class";

    @Component
    private MavenProject mavenProject;

    @Parameter(name = "rebase", defaultValue = "true", required = true)
    private boolean rebase;

//    @Parameter(name = "includeTest", defaultValue = "false", required = true)
//    private boolean includeTest;

    @Parameter(name = "plugins", required = true)
    private List<String> plugins;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            doExecute();
        } catch (IOException exception) {
            throw new MojoExecutionException("Could not process classes", exception);
        } catch (DependencyResolutionRequiredException exception) {
            throw new MojoExecutionException("Dependencies not resolved", exception);
        }
    }

    private void doExecute() throws DependencyResolutionRequiredException, IOException {
        processOutputDirectory(mavenProject.getBuild().getOutputDirectory(), mavenProject.getCompileClasspathElements());
//        if (includeTest) {
//            processOutputDirectory(mavenProject.getBuild().getTestOutputDirectory(), mavenProject.getTestClasspathElements());
//        }
    }

    private void processOutputDirectory(String target, List<? extends String> dependencies) throws IOException {
        processOutputDirectory(new File(target), dependencies);
    }

    private void processOutputDirectory(File target, List<? extends String> dependencies) throws IOException {
        getLog().info("Processing classes in: " + target);
        if (!target.isDirectory()) {
            throw new IOException("Source location does not exist: " + target);
        }
        List<Plugin> plugins = new ArrayList<Plugin>(this.plugins.size());
        for (String plugin : this.plugins) {
            try {
                plugins.add((Plugin) Class.forName(plugin).getDeclaredConstructor().newInstance());
            } catch (Exception exception) {
                throw new RuntimeException("Cannot create plugin: " + plugin, exception);
            }
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
            processDirectory(target, target, byteBuddy, classFileLocator, typePool, plugins);
        } finally {
            classFileLocator.close();
        }
    }

    private void processDirectory(File root, File directory, ByteBuddy byteBuddy, ClassFileLocator classFileLocator, TypePool typePool, List<Plugin> plugins) throws IOException {
        File[] file = directory.listFiles();
        if (file != null) {
            for (File aFile : file) {
                if (aFile.isDirectory()) {
                    processDirectory(root, aFile, byteBuddy, classFileLocator, typePool, plugins);
                } else if (aFile.isFile() && aFile.getName().endsWith(CLASS_FILE_EXTENSION)) {
                    processClassFile(root, root.toURI().relativize(aFile.toURI()).toString(), byteBuddy, classFileLocator, typePool, plugins);
                } else {
                    getLog().debug("Skipping ignored file: " + aFile);
                }
            }
        }
    }

    private void processClassFile(File root, String classFile, ByteBuddy byteBuddy, ClassFileLocator classFileLocator, TypePool typePool, List<Plugin> plugins) throws IOException {
        String typeName = classFile.replace('/', '.').substring(0, classFile.length() - CLASS_FILE_EXTENSION.length());
        getLog().debug("Processing class file: " + typeName);
        TypeDescription typeDescription = typePool.describe(typeName).resolve();
        DynamicType.Builder<?> builder = rebase
                ? byteBuddy.rebase(typeDescription, classFileLocator)
                : byteBuddy.redefine(typeDescription, classFileLocator);
        boolean transformed = false;
        for (Plugin plugin : plugins) {
            if (plugin.matches(typeDescription)) {
                builder = plugin.apply(builder);
                transformed = true;
            }
        }
        if (transformed) {
            getLog().info("Transformed type: " + typeName);
            builder.make().saveIn(root);
        } else {
            getLog().debug("Skipping non-transformed file: " + typeName);
        }
    }
}