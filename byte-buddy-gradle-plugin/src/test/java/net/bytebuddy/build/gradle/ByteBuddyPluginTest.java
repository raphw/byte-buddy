package net.bytebuddy.build.gradle;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.utility.OpenedClassReader;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

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

    @Test
    @IntegrationRule.Enforce
    public void testClassPathFingerprintTracksMethodBodies() throws Exception {
        // Verifies the task's classPath input uses @Classpath (full bytecode hashing) rather
        // than @CompileClasspath (ABI-only), since plugins may inspect arbitrary bytecode.
        // The dependency jar is kept outside of the project folder that is deleted on tear-down as
        // the Gradle daemon may retain a lock on a class path entry on Windows which would otherwise
        // fail the recursive folder cleanup.
        File dependencyJar = File.createTempFile("byte-buddy-gradle-plugin-dependency", ".jar");
        dependencyJar.deleteOnExit();
        writeClassPathJar(dependencyJar, 1);
        write("build.gradle",
            "plugins {",
            "  id 'java'",
            "  id 'net.bytebuddy.byte-buddy-gradle-plugin'",
            "}",
            "dependencies {",
            // The 'implementation' configuration does not exist on the legacy Gradle 2.x distribution.
            "  if (gradle.gradleVersion.startsWith(\"2.\")) {",
            "    compile files('" + dependencyJar.getAbsolutePath().replace('\\', '/') + "')",
            "  } else {",
            "    implementation files('" + dependencyJar.getAbsolutePath().replace('\\', '/') + "')",
            "  }",
            "}",
            "byteBuddy {",
            "  transformation {",
            "    plugin = net.bytebuddy.build.Plugin.NoOp.class",
            "  }",
            "}");
        write("src/main/java/sample/SampleClass.java",
            "package sample;",
            "public class SampleClass { }");
        // Prime the cache. GradleRunner.build() throws on failure, so no outcome assertion
        // is needed here.
        GradleRunner.create()
            .withProjectDir(folder)
            .withArguments("byteBuddy")
            .withPluginClasspath()
            .build();

        // Re-run with no input changes: the task must be UP_TO_DATE. This rules out the
        // possibility that a third-run SUCCESS is caused by some unrelated volatile input
        // rather than by the classpath jar swap performed below.
        TaskOutcome unchangedOutcome = GradleRunner.create()
            .withProjectDir(folder)
            .withArguments("byteBuddy")
            .withPluginClasspath()
            .build()
            .task(":byteBuddy")
            .getOutcome();
        assertThat(unchangedOutcome, is(TaskOutcome.UP_TO_DATE));

        // Replace the dependency jar with a variant that has the SAME ABI but a different
        // method body. With @CompileClasspath this change is invisible to the cache key
        // and the task is incorrectly reported as UP_TO_DATE. With @Classpath the
        // byte-level change busts the fingerprint and the task re-executes (SUCCESS).
        writeClassPathJar(dependencyJar, 2);
        TaskOutcome swappedOutcome = GradleRunner.create()
            .withProjectDir(folder)
            .withArguments("byteBuddy")
            .withPluginClasspath()
            .build()
            .task(":byteBuddy")
            .getOutcome();
        assertThat(swappedOutcome, is(TaskOutcome.SUCCESS));
    }

    private static void writeClassPathJar(File jar, int constant) throws IOException {
        // Two invocations with different `constant` values share the same public ABI but
        // have different method bodies, so they must produce different bytecode hashes.
        byte[] bytes = new ByteBuddy()
            .subclass(Object.class)
            .name("sample.ClassPathClass")
            .defineMethod("value", int.class, Visibility.PUBLIC)
            .intercept(FixedValue.value(constant))
            .make()
            .getBytes();
        JarOutputStream out = new JarOutputStream(new FileOutputStream(jar));
        try {
            out.putNextEntry(new JarEntry("sample/ClassPathClass.class"));
            out.write(bytes);
            out.closeEntry();
        } finally {
            out.close();
        }
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