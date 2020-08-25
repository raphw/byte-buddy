package net.bytebuddy.build.gradle;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.utility.OpenedClassReader;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static junit.framework.TestCase.*;

public class ByteBuddyPluginTest {

    private static final String FOO = "foo";

    private File folder;

    @Before
    public void setUp() throws Exception {
        folder = File.createTempFile("byte-buddy-gradle-plugin", "");
        assertTrue(folder.delete());
        assertTrue(folder.mkdir());
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
                    assertTrue(aFile.delete());
                }
            }
        }
        assertTrue(folder.delete());
    }

    @Test
    public void testPluginExecution() throws Exception {
        write("build.gradle",
                "plugins {",
                "  id 'java'",
                "  id 'net.bytebuddy.byte-buddy-gradle-plugin'",
                "}",
                "",
                "import net.bytebuddy.build.Plugin;",
                "import net.bytebuddy.description.type.TypeDescription;",
                "import net.bytebuddy.dynamic.ClassFileLocator;",
                "import net.bytebuddy.dynamic.DynamicType;",
                "",
                "class SamplePlugin implements Plugin {",
                "  @Override public boolean matches(TypeDescription target) {",
                "    return target.getSimpleName().equals(\"SampleClass\");",
                "  }",
                "  @Override public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, " +
                        "TypeDescription typeDescription, " +
                        "ClassFileLocator classFileLocator) {",
                "    return builder.defineField(\"" + FOO + "\", Void.class);",
                "  }",
                "  @Override public void close() { }",
                "}",
                "",
                "byteBuddy {",
                "  transformation {",
                "    plugin = SamplePlugin.class",
                "  }",
                "}");
        write("src/main/java/sample/SampleClass.java", "public class SampleClass { }");
        BuildResult result = GradleRunner.create()
                .withProjectDir(folder)
                .withArguments("build", "-D" + ByteBuddyPlugin.LEGACY + "=true")
                .withPluginClasspath()
                .build();
        BuildTask task = result.task(":byteBuddy");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());
        assertResult(FOO);
    }

    private File create(List<String> segments) {
        File folder = this.folder;
        for (String segment : segments.subList(0, segments.size() - 1)) {
            folder = new File(folder, segment);
            assertTrue(folder.mkdir() || folder.isDirectory());
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

    private void assertResult(final String expectation) throws IOException {
        File jar = new File(folder, "build/libs/" + folder.getName() + ".jar");
        assertTrue(jar.isFile());
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jar));
        try {
            JarEntry entry = jarInputStream.getNextJarEntry();
            assertNotNull(entry);
            new ClassReader(jarInputStream).accept(new ClassVisitor(OpenedClassReader.ASM_API) {

                private boolean found;

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    assertEquals(expectation, name);
                    found  = true;
                    return null;
                }

                @Override
                public void visitEnd() {
                    if (!found) {
                        throw new AssertionError("Field missing");
                    }
                }
            }, ClassReader.SKIP_CODE);
            jarInputStream.closeEntry();
            assertNull(jarInputStream.getNextEntry());
        } finally {
            jarInputStream.close();
        }
    }
}