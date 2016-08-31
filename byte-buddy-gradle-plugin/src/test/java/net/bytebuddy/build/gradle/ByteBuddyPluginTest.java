package net.bytebuddy.build.gradle;

import net.bytebuddy.test.utility.IntegrationRule;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ByteBuddyPluginTest {

    private static final String BYTE_BUDDY_VERSION = System.getProperty("net.bytebuddy.test.version", "1.4.22");

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    private static File pluginJar;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    @BeforeClass
    public static void createTestPluginJarFile() throws IOException {
        pluginJar = makePluginJar();
    }

    private static File makePluginJar() throws IOException {
        File pluginFolder = TEMPORARY_FOLDER.newFolder("test-byte-buddy-plugin");
        store("plugins { id 'java' }\n" +
                "repositories {\n" +
                "  mavenLocal()\n" +
                "  mavenCentral()\n" +
                "}\n" +
                "dependencies {\n" +
                "  compile 'net.bytebuddy:byte-buddy:" + BYTE_BUDDY_VERSION + "'\n" +
                "}\n", new File(pluginFolder, "build.gradle"));
        File pluginRoot = new File(pluginFolder, "src/main/java/net/bytebuddy/test");
        assertThat(pluginRoot.mkdirs(), is(true));
        store("package net.bytebuddy.test;\n" +
                "\n" +
                "import net.bytebuddy.build.Plugin;\n" +
                "import net.bytebuddy.description.type.TypeDescription;\n" +
                "import net.bytebuddy.dynamic.DynamicType;\n" +
                "import net.bytebuddy.implementation.FixedValue;\n" +
                "\n" +
                "import static net.bytebuddy.matcher.ElementMatchers.named;\n" +
                "\n" +
                "public class SimplePlugin implements Plugin {\n" +
                "    @Override\n" +
                "    public boolean matches(TypeDescription target) {\n" +
                "        return target.getName().equals(\"net.bytebuddy.test.Sample\");\n" +
                "    }\n" +
                "    @Override\n" +
                "    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription) {\n" +
                "        return builder.method(named(\"foo\")).intercept(FixedValue.value(\"qux\"));\n" +
                "    }\n" +
                "}\n", new File(pluginRoot, "SimplePlugin.java"));
        BuildResult result = GradleRunner.create()
                .withProjectDir(pluginFolder)
                .withArguments("jar")
                .build();
        assertThat(result.task(":jar").getOutcome(), is(TaskOutcome.SUCCESS));
        return new File(pluginFolder, "build/libs/test-byte-buddy-plugin.jar");
    }

    @Test
    @IntegrationRule.Enforce
    public void testGradlePlugin() throws IOException {
        createSampleBuildFiles();
        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(temporaryFolder.getRoot()).withArguments("-s", "run")
                .forwardOutput()
                .build();
        assertThat(result.task(":classes").getOutcome(), is(TaskOutcome.SUCCESS));
        assertThat(result.getOutput(), containsString("foo=qux"));
    }

    @Test
    @IntegrationRule.Enforce
    public void testIncrementalCompilationFails() throws IOException {
        createSampleBuildFiles();
        append("compileJava.options.incremental = true\n", new File(temporaryFolder.getRoot(), "build.gradle"));
        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(temporaryFolder.getRoot()).withArguments("classes")
                .forwardOutput()
                .buildAndFail();
        assertThat(result.getOutput(), containsString("Transformations aren't supported when incremental compilation is enabled."));
        throw new AssertionError("Property!");
    }

    private void createSampleBuildFiles() throws IOException {
        store("plugins {\n" +
                "    id 'java'\n" +
                "    id 'application'\n" +
                "    id 'net.bytebuddy.byte-buddy'\n" +
                "}\n" +
                "configurations {\n" +
                "    sample\n" +
                "}\n" +
                "dependencies {\n" +
                "    sample files(\"" + pluginJar.getAbsolutePath() + "\")\n" +
                "}\n" +
                "byteBuddy {\n" +
                "    transformation {\n" +
                "        plugin = \"net.bytebuddy.test.SimplePlugin\"\n" +
                "        classPath = configurations.sample\n" +
                "    }\n" +
                "}\n" +
                "mainClassName = 'net.bytebuddy.test.Sample'\n", temporaryFolder.newFile("build.gradle"));
        store("package net.bytebuddy.test;\n" +
                "public class Sample {\n" +
                "    public String foo() {\n" +
                "        return \"bar\";\n" +
                "    }\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"foo=\" + new Sample().foo());\n" +
                "    }\n" +
                "}\n", new File(temporaryFolder.newFolder("src", "main", "java", "net", "bytebuddy", "test"), "Sample.java"));
    }

    private static void store(String source, File target) throws IOException {
        store(source, target, false);
    }

    private static void append(String source, File target) throws IOException {
        store(source, target, true);
    }

    private static void store(String source, File target, boolean append) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(source.getBytes("UTF-8"));
        try {
            OutputStream outputStream = new FileOutputStream(target, append);
            try {
                byte[] buffer = new byte[1024];
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
    }
}
