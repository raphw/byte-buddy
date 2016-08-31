package net.bytebuddy.build.gradle;

import com.google.common.io.Files;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static junit.framework.TestCase.assertTrue;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.Assert.assertEquals;

public class ByteBuddyPluginFunctionalTest {
    public static final String BYTE_BUDDY_VERSION = System
            .getProperty("byte_buddy_version", "1.4.22-SNAPSHOT");
    @ClassRule
    public static final TemporaryFolder testPluginJarProjectDir = new TemporaryFolder();
    private static File testPluginJarFile;

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private File buildFile;

    @BeforeClass
    public static void createTestPluginJarFile() throws IOException {
        testPluginJarFile = compileTestByteBuddyPluginJar();
    }

    private static File compileTestByteBuddyPluginJar() throws IOException {
        File testByteBuddyPluginDir = testPluginJarProjectDir
                .newFolder("test-byte-buddy-plugin");
        File pluginBuildFile = new File(testByteBuddyPluginDir, "build.gradle");
        Files.write(
                "plugins { id 'java' }\nrepositories { mavenLocal()\nmavenCentral()\n}\ndependencies {\ncompile 'net.bytebuddy:byte-buddy:"
                        + BYTE_BUDDY_VERSION + "'}\n", pluginBuildFile,
                Charset.defaultCharset());
        File testPluginFile = new File(testByteBuddyPluginDir,
                "src/main/java/net/bytebuddy/test/SimplePlugin.java");
        testPluginFile.getParentFile().mkdirs();
        Files.write("package net.bytebuddy.test;\n" + "\n"
                + "import net.bytebuddy.build.Plugin;\n"
                + "import net.bytebuddy.description.type.TypeDescription;\n"
                + "import net.bytebuddy.dynamic.DynamicType;\n"
                + "import net.bytebuddy.implementation.FixedValue;\n" + "\n"
                + "import static net.bytebuddy.matcher.ElementMatchers.named;\n" + "\n"
                + "public class SimplePlugin implements Plugin {\n" + "\n"
                + "    @Override\n"
                + "    public boolean matches(TypeDescription target) {\n"
                + "        return target.getName().equals(\"foo.Bar\");\n" + "    }\n"
                + "\n" + "    @Override\n"
                + "    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription) {\n"
                + "        return builder.method(named(\"foo\")).intercept(FixedValue.value(\"qux\"));\n"
                + "    }\n" + "}\n", testPluginFile, Charset.defaultCharset());
        GradleRunner.create().withProjectDir(testByteBuddyPluginDir).withArguments("jar")
                .build();
        return new File(testByteBuddyPluginDir, "build/libs/test-byte-buddy-plugin.jar");
    }

    @Before
    public void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle");
    }

    @Test
    public void testByteBuddy() throws IOException {
        Files.write("plugins {\n" + "    id 'java'\n" + "    id 'application'\n"
                        + "    id 'net.bytebuddy.byte-buddy'\n" + "}\n" + "configurations {\n"
                        + "    simpleplugin\n" + "}\n" + "dependencies {\n"
                        + "    simpleplugin files(\"" + testPluginJarFile.getAbsolutePath()
                        .replace(File.separatorChar, '/') + "\")\n" + "}\n" + "byteBuddy {\n"
                        + "    transformations {\n" + "        transformation {\n"
                        + "            plugin = \"net.bytebuddy.test.SimplePlugin\"\n"
                        + "            classpath = configurations.simpleplugin\n" + "        }\n"
                        + "    }\n" + "}\n" + "mainClassName = 'foo.Bar'", buildFile,
                Charset.defaultCharset());
        Files.write(
                "package foo;\npublic class Bar { public String foo() { return \"bar\"; } public static void main(String[] args) { System.out.println(\"foo=\" + new Bar().foo()); } }",
                new File(testProjectDir.newFolder("src", "main", "java", "foo"),
                        "Bar.java"), Charset.defaultCharset());

        BuildResult result = GradleRunner.create().withPluginClasspath()
                .withProjectDir(testProjectDir.getRoot()).withArguments("-s", "run")
                .forwardOutput()
                .build();

        assertEquals(result.task(":classes").getOutcome(), SUCCESS);

        assertTrue(result.getOutput().contains("foo=qux"));
    }

}
