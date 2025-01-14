package net.bytebuddy.build.gradle;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.utility.OpenedClassReader;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.*;
import org.junit.rules.MethodRule;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyPluginTest {

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private static final String FOO = "foo";

    private File folder;

    private File byteBuddyJar;

    @Before
    public void setUp() throws Exception {
        folder = File.createTempFile("byte-buddy-gradle-plugin", "");
        assertThat(folder.delete(), is(true));
        assertThat(folder.mkdir(), is(true));
        CodeSource source = ByteBuddyPluginTest.class.getProtectionDomain().getCodeSource();
        if (source == null) {
            throw new IllegalStateException("Failed to resolve code source");
        }
        URL location = source.getLocation();
        if (location == null || !location.getProtocol().equals("file")) {
            throw new IllegalStateException("Expected location to be a file location: " + location);
        }
        File file = new File(location.getPath());
        while (!new File(file, "pom.xml").isFile()) {
            file = file.getParentFile();
        }
        while (new File(file.getParentFile(), "pom.xml").isFile()) {
            file = file.getParentFile();
        }
        assertThat(file.isDirectory(), is(true));
        InputStream inputStream = new FileInputStream(new File(file, "pom.xml"));
        Document document;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        } finally {
            inputStream.close();
        }
        String version = (String) XPathFactory.newInstance()
                .newXPath()
                .compile("/project/version")
                .evaluate(document, XPathConstants.STRING);
        assertThat(version, notNullValue(String.class));
        byteBuddyJar = new File(file, "byte-buddy/target/byte-buddy-" + version + ".jar");
        assertThat(byteBuddyJar.isFile(), is(true));
    }

    @After
    public void tearDown() throws Exception {
        delete(folder);
    }

    private static void delete(File folder) {
        File[] file = folder.listFiles();
        if (file != null) {
            for (File aFile : file) {
                if (aFile.isDirectory()) {
                    delete(aFile);
                } else {
                    assertThat(aFile.delete(), is(true));
                }
            }
        }
        assertThat(folder.delete(), is(true));
    }

    @Test
    @IntegrationRule.Enforce
    public void testPluginExecution() throws Exception {
        write("build.gradle",
            "plugins {",
            "  id 'java'",
            "  id 'net.bytebuddy.byte-buddy-gradle-plugin'",
            "}",
            "",
            "byteBuddy {",
            "  transformation {",
            "    plugin = sample.SamplePlugin.class",
            "  }",
            "}");
        write("buildSrc/build.gradle",
                "dependencies {",
                "  if (gradle.gradleVersion.startsWith(\"2.\")) {",
                "    compile files('" + byteBuddyJar.getAbsolutePath().replace("\\", "\\\\") + "')",
                "  } else {",
                "    implementation files('" + byteBuddyJar.getAbsolutePath().replace("\\", "\\\\") + "')",
                "  }",
                "}");
        write("buildSrc/src/main/java/sample/SamplePlugin.java",
            "package sample;",
            "",
            "import net.bytebuddy.build.Plugin;",
            "import net.bytebuddy.description.type.TypeDescription;",
            "import net.bytebuddy.dynamic.ClassFileLocator;",
            "import net.bytebuddy.dynamic.DynamicType;",
            "",
            "public class SamplePlugin implements Plugin {",
            "",
            "  public boolean matches(TypeDescription target) {",
            "    return target.getSimpleName().equals(\"SampleClass\");",
            "  }",
            "",
            "  public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, " +
                    "TypeDescription typeDescription, " +
                    "ClassFileLocator classFileLocator) {",
            "    return builder.defineField(\"" + FOO + "\", Void.class);",
            "  }",
            "",
            "  public void close() { }",
            "}");
        write("src/main/java/sample/SampleClass.java",
            "package sample;",
            "",
            "public class SampleClass { }");
        BuildResult result = GradleRunner.create()
            .withProjectDir(folder)
            .withArguments("build", "-Dorg.gradle.unsafe.configuration-cache=true")
            .withPluginClasspath()
            .build();
        BuildTask task = result.task(":byteBuddy");
        assertThat(task, notNullValue(BuildTask.class));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
        assertResult(FOO, "sample/", "SampleClass.class");
        assertThat(result.task(":byteBuddyTest"), nullValue(BuildTask.class));
    }

    @Test
    @IntegrationRule.Enforce
    public void testPluginWithArgumentsExecution() throws Exception {
        write("build.gradle",
            "plugins {",
            "  id 'java'",
            "  id 'net.bytebuddy.byte-buddy-gradle-plugin'",
            "}",
            "",
            "byteBuddy {",
            "  transformation {",
            "    plugin = sample.SamplePlugin.class",
            "    argument {",
            "      value = '" + FOO + "'",
            "    }",
            "  }",
            "}");
        write("buildSrc/build.gradle",
            "dependencies {",
            "  if (gradle.gradleVersion.startsWith(\"2.\")) {",
            "    compile files('" + byteBuddyJar.getAbsolutePath().replace("\\", "\\\\") + "')",
            "  } else {",
            "    implementation files('" + byteBuddyJar.getAbsolutePath().replace("\\", "\\\\") + "')",
            "  }",
            "}");
        write("buildSrc/src/main/java/sample/SamplePlugin.java",
            "package sample;",
            "",
            "import net.bytebuddy.build.Plugin;",
            "import net.bytebuddy.description.type.TypeDescription;",
            "import net.bytebuddy.dynamic.ClassFileLocator;",
            "import net.bytebuddy.dynamic.DynamicType;",
            "",
            "public class SamplePlugin implements Plugin {",
            "",
            "  private final String value;",
            "",
            "  public SamplePlugin(String value) { this.value = value; }",
            "",
            "  public boolean matches(TypeDescription target) {",
            "    return target.getSimpleName().equals(\"SampleClass\");",
            "  }",
            "",
            "  public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, " +
                    "TypeDescription typeDescription, " +
                    "ClassFileLocator classFileLocator) {",
            "    return builder.defineField(value, Void.class);",
            "  }",
            "",
            "  public void close() { }",
            "}");
        write("src/main/java/sample/SampleClass.java",
            "package sample;",
            "",
            "public class SampleClass { }");
        BuildResult result = GradleRunner.create()
            .withProjectDir(folder)
            .withArguments("build", "-Dorg.gradle.unsafe.configuration-cache=true")
            .withPluginClasspath()
            .build();
        BuildTask task = result.task(":byteBuddy");
        assertThat(task, notNullValue(BuildTask.class));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
        assertResult(FOO, "sample/", "SampleClass.class");
        assertThat(result.task(":byteBuddyTest"), nullValue(BuildTask.class));
    }

    private File create(List<String> segments) {
        File folder = this.folder;
        for (String segment : segments.subList(0, segments.size() - 1)) {
            folder = new File(folder, segment);
            assertThat(folder.mkdir() || folder.isDirectory(), is(true));
        }
        return new File(folder, segments.get(segments.size() - 1));
    }

    private void write(String path, String... line) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(create(Arrays.asList(path.split("/")))));
        try {
            for (String aLine : line) {
                writer.println(aLine);
            }
            writer.println();
        } finally {
            writer.close();
        }
    }

    private void assertResult(final String expectation, String... path) throws IOException {
        File jar = new File(folder, "build/libs/" + folder.getName() + ".jar");
        assertThat(jar.isFile(), is(true));
        InputStream inputStream = new FileInputStream(jar);
        try {
            JarInputStream jarInputStream = new JarInputStream(inputStream);
            String concatenation = "";
            for (int index = 0; index < path.length; index++) {
                JarEntry entry = jarInputStream.getNextJarEntry();
                assertThat(entry, notNullValue(JarEntry.class));
                concatenation += path[index];
                assertThat(entry.getName(), is(concatenation));
            }
            new ClassReader(jarInputStream).accept(new ClassVisitor(OpenedClassReader.ASM_API) {

                private boolean found;

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    assertThat(name, is(expectation));
                    found = true;
                    return null;
                }

                @Override
                public void visitEnd() {
                    assertThat(found, is(true));
                }
            }, ClassReader.SKIP_CODE);
            jarInputStream.closeEntry();
            assertThat(jarInputStream.getNextJarEntry(), nullValue(JarEntry.class));
            jarInputStream.close();
        } finally {
            inputStream.close();
        }
    }
}