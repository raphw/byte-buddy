package net.bytebuddy.test.utility;

import net.lingala.zip4j.ZipFile;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.build.gradle.android.utils.Many.arrayOf;
import static net.bytebuddy.build.gradle.android.utils.Many.listOf;
import static net.bytebuddy.build.gradle.android.utils.Many.map;

public abstract class BaseAndroidGradleTest {

    private static final String BUILD_GRADLE_FILE_NAME = "build.gradle";

    @ClassRule
    public static final TemporaryFolder classTempDir = new TemporaryFolder();

    private static File dexToolsDir;

    protected static File getAssetFile(String name) {
        File assetsDir = Paths.get("src", "test", "assets").toFile();
        if (!assetsDir.exists()) {
            throw new IllegalStateException("Assets dir not found, should be in src/{testsDirName}/assets");
        }
        File dir = new File(assetsDir, name);
        if (!dir.exists()) {
            throw new IllegalStateException(String.format("Dir not found in assets '%s'", name));
        }
        return dir;
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        File dexToolsZip = getAssetFile("tools/dex-tools.zip");
        File dexToolsExtractDir = classTempDir.newFolder("dex-tools-extracted");

        new ZipFile(dexToolsZip).extractAll(dexToolsExtractDir.getAbsolutePath());

        dexToolsDir = dexToolsExtractDir.listFiles()[0];
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File rootProjectDir;
    private File settingsGradle;

    @Before
    public void setUp() {
        rootProjectDir = temporaryFolder.getRoot();
        settingsGradle = new File(rootProjectDir, "settings.gradle");
        File rootBuildGradle = new File(rootProjectDir, BUILD_GRADLE_FILE_NAME);

        addText(rootBuildGradle,
                "subprojects {",
                "     repositories {",
                "       mavenCentral()",
                "       google()",
                "     }",
                "}");


        addText(settingsGradle,
                "rootProject.name = 'dummy-project'"
        );
    }

    protected ProjectInfo initAppProject(String projectName, List<String> plugins, List<String> dependencies) {
        List<String> finalPlugins = listOf("id 'net.bytebuddy.bytebuddy-android'");
        finalPlugins.addAll(plugins);
        return initAndroidProject(
                projectName,
                "com.android.application",
                finalPlugins,
                dependencies
        );
    }

    protected ProjectInfo initJarProject(String projectName, List<String> plugins, List<String> dependencies) {
        ArrayList<String> finalPlugins = listOf("id 'java-library'");
        finalPlugins.addAll(plugins);
        return initSubproject(projectName, finalPlugins, dependencies);
    }

    protected ProjectInfo initAarProject(String projectName, List<String> plugins, List<String> dependencies) {
        return initAndroidProject(projectName, "com.android.library", plugins, dependencies);
    }

    private ProjectInfo initAndroidProject(
            String projectName,
            String androidProjectPluginId,
            List<String> plugins,
            List<String> dependencies
    ) {
        ArrayList<String> finalPlugins = listOf(String.format("id '%s'", androidProjectPluginId));
        finalPlugins.addAll(plugins);
        ProjectInfo androidProject = initSubproject(projectName, finalPlugins, dependencies);
        File projectManifest = new File(androidProject.mainDir, "AndroidManifest.xml");
        addText(androidProject.buildGradeFile,
                "android {",
                "   compileSdkVersion 30",
                "}"
        );

        addText(projectManifest,
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"",
                String.format("package=\"some.name.%s\" />", projectName)
        );

        return androidProject;
    }

    private ProjectInfo initSubproject(String projectName, List<String> plugins, List<String> dependencies) {
        File dummyProject = getDummyProjectDir(projectName);
        File projectDir = new File(rootProjectDir, projectName);
        File projectMainDir = new File(projectDir, "src/main");
        File subprojectGradle = new File(projectDir, BUILD_GRADLE_FILE_NAME);

        moveFilesToDir(dummyProject.listFiles(), projectMainDir);

        addText(subprojectGradle,
                " plugins {",
                join(plugins, "\n"),
                "}",

                (dependencies.isEmpty()) ? "" : createDependenciesBlock(dependencies)
        );

        addText(settingsGradle,
                "\n",
                String.format("include ':%s'", projectName)
        );

        return new ProjectInfo(projectName, projectDir, projectMainDir, subprojectGradle);
    }

    private String createDependenciesBlock(List<String> dependencies) {
        StringBuilder builder = new StringBuilder();
        builder.append("dependencies {");
        builder.append(join(dependencies, "\n"));
        builder.append("}");

        return builder.toString();
    }

    private String join(List<String> lines, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(separator);
            builder.append(line);
        }
        return builder.toString();
    }

    protected BuildResult buildProject(
            ProjectInfo project,
            List<String> commands
    ) {
        return buildProject(project, commands, false);
    }

    protected BuildResult buildProject(
            ProjectInfo project,
            List<String> commands,
            Boolean withInfo
    ) {
        List<String> extraArgs = listOf("--stacktrace");
        if (withInfo) {
            extraArgs.add("--info");
        }
        ArrayList<String> arguments = map(commands, it -> project.name + ":" + it);
        arguments.addAll(extraArgs);
        return createGradleRunner()
                .withArguments(arguments)
                .build();
    }

    protected abstract String getGradleVersion();

    private GradleRunner createGradleRunner() {
        return GradleRunner.create()
                .withProjectDir(rootProjectDir)
                .withGradleVersion(getGradleVersion())
                .withPluginClasspath();
    }

    private File getDummyProjectDir(String name) {
        File projectDir = getAssetFile("dummyProjects/" + name);
        if (!projectDir.exists()) {
            throw new IllegalStateException(String.format("Project not found: '%s'", name));
        }
        return projectDir;
    }

    private void moveFilesToDir(File[] files, File destinationDir) {
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                moveFilesToDir(file.listFiles(), new File(destinationDir, file.getName()));
            } else {
                File target = new File(destinationDir, file.getName());
                try {
                    if (!destinationDir.exists()) {
                        destinationDir.mkdirs();
                    }
                    Files.copy(file.toPath(), target.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected ClassLoader getAppClassloader(ProjectInfo project) {
        File jarFile = extractJarFromProject(project);
        try {
            return new URLClassLoader(arrayOf(jarFile.toURI().toURL()), this.getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private File extractJarFromProject(ProjectInfo project) {
        File projectDir = project.dir;
        File apkDir = new File(projectDir, "build/outputs/apk/debug");
        File apkPath = new File(apkDir, project.name + "-debug.apk");

        return extractJarFromApk(apkPath);
    }

    private File extractJarFromApk(File apkFile) {
        File destinationDir = apkFile.getParentFile();
        String dexFileName = "classes.dex";
        try {
            new ZipFile(apkFile).extractFile(dexFileName, destinationDir.getAbsolutePath());
            File dexFile = new File(destinationDir, dexFileName);
            return convertDexToJar(dexFile);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private File convertDexToJar(File dexFile) throws IOException, InterruptedException {
        File jarFile = new File(dexFile.getParentFile(), "classes.jar");
        File dex2jarScript = new File(dexToolsDir, "d2j-dex2jar.sh");
        ProcessBuilder processBuilder = new ProcessBuilder(
                "sh",
                dex2jarScript.getAbsolutePath(),
                "-f",
                dexFile.getAbsolutePath(),
                "-o",
                jarFile.getAbsolutePath()
        );

        String javaHomeDir = System.getenv("JAVA_HOME");

        if (javaHomeDir == null || javaHomeDir.isEmpty()) {
            throw new IllegalStateException("JAVA_HOME not set");
        }

        processBuilder.environment().put("PATH",
                String.format("%s%s%s/bin", System.getenv("PATH"), File.pathSeparator, javaHomeDir));

        Process process = processBuilder.start();

        process.waitFor(10, TimeUnit.SECONDS);

        return jarFile;
    }

    protected String getBytebuddyArtifactDependency() {
        String bytebuddyArtifactPath = System.getProperty("bytebuddyArtifactPath");
        return String.format("files('%s')", bytebuddyArtifactPath);
    }

    private void addText(File file, String... lines) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(new FileWriter(file, true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            for (String aLine : lines) {
                writer.println(aLine);
            }
            writer.println();
        } finally {
            writer.close();
        }
    }
}