package net.bytebuddy.build.gradle.transform;

import net.bytebuddy.build.gradle.transform.api.Sample;
import net.bytebuddy.build.gradle.transform.api.Substitution;
import net.bytebuddy.build.gradle.transform.target.Target;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import static junit.framework.TestCase.*;

public class GradleTypeTransformerTest {

    private File file;

    @Before
    public void setUp() throws Exception {
        file = File.createTempFile("gradle-byte-buddy-test", ".jar");
    }

    @After
    public void tearDown() throws Exception {
        assertTrue(file.delete());
    }

    @Test
    public void testJarTransformation() throws Exception {
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file));
        try {
            for (Class<?> type : Arrays.asList(Sample.class, Substitution.class, Target.class)) {
                jarOutputStream.putNextEntry(new JarEntry(type.getName().replace('.', '/') + ".class"));
                InputStream inputStream = type.getResourceAsStream(type.getSimpleName() + ".class");
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    jarOutputStream.write(buffer, 0, length);
                }
                jarOutputStream.closeEntry();
            }
        } finally {
            jarOutputStream.close();
        }
        new GradleTypeTransformer(Sample.class.getPackage().getName().replace('.', '/'), Substitution.class.getName().replace('.', '/')).transform(file);
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(file));
        try {
            JarEntry entry = jarInputStream.getNextJarEntry();
            assertNotNull(entry);
            assertEquals(Target.class.getName().replace('.', '/') + ".class", entry.getName());
            new ClassReader(jarInputStream).accept(new ClassVisitor(Opcodes.ASM8) {
                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    assertEquals("Lfoo/Bar;", descriptor);
                    return null;
                }
            }, ClassReader.SKIP_CODE);
            jarInputStream.closeEntry();
            assertNull(jarInputStream.getNextEntry());
        } finally {
            jarInputStream.close();
        }
    }
}