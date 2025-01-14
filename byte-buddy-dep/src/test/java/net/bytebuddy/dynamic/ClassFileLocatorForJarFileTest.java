package net.bytebuddy.dynamic;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassFileLocatorForJarFileTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int VALUE = 42;

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File file;

    @Before
    public void setUp() throws Exception {
        file = temporaryFolder.newFile();
    }

    @Test
    public void testSuccessfulLocation() throws Exception {
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
            JarEntry jarEntry = new JarEntry(FOO + "/" + BAR + ClassFileLocator.CLASS_FILE_EXTENSION);
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(VALUE);
            jarOutputStream.write(VALUE * 2);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        JarFile jarFile = new JarFile(file);
        try {
            ClassFileLocator classFileLocator = new ClassFileLocator.ForJarFile(jarFile);
            ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
            assertThat(resolution.isResolved(), is(true));
            assertThat(resolution.resolve(), is(new byte[]{VALUE, VALUE * 2}));
        } finally {
            jarFile.close();
        }
    }

    @Test
    public void testClassPath() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForJarFile.ofClassPath();
        try {
            assertThat(classFileLocator.locate(ByteBuddy.class.getName()).isResolved(), is(true)); // As file.
            assertThat(classFileLocator.locate(ClassVisitor.class.getName()).isResolved(), is(true)); // On path.
        } finally {
            classFileLocator.close();
        }
    }

    @Test
    @JavaVersionRule.Enforce(atMost = 8)
    public void testRuntimeJar() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForJarFile.ofRuntimeJar();
        try {
            // java.lang.Object is not contained in the rt.jar for some JVMs.
            assertThat(classFileLocator.locate(Void.class.getName()).isResolved(), is(true));
        } finally {
            classFileLocator.close();
        }
    }

    @Test
    public void testNonSuccessfulLocation() throws Exception {
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
            JarEntry jarEntry = new JarEntry("noop.class");
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(VALUE);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        JarFile jarFile = new JarFile(file);
        try {
            ClassFileLocator classFileLocator = new ClassFileLocator.ForJarFile(jarFile);
            ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
            assertThat(resolution.isResolved(), is(false));
        } finally {
            jarFile.close();
        }
    }

    @Test
    public void testNoClose() throws Exception {
        JarFile jarFile = mock(JarFile.class);
        new ClassFileLocator.ForJarFile(jarFile).close();
        verifyNoMoreInteractions(jarFile);
    }

    @Test
    public void testClose() throws Exception {
        JarFile jarFile = mock(JarFile.class);
        new ClassFileLocator.ForJarFile(new int[0], jarFile, true).close();
        verify(jarFile).close();
        verifyNoMoreInteractions(jarFile);
    }

    @Test
    public void testMultiReleaseVersionLocation() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Multi-Release", "true");
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream, manifest);
            jarOutputStream.putNextEntry(new JarEntry("META-INF/versions/11/" + FOO + "/" + BAR + ClassFileLocator.CLASS_FILE_EXTENSION));
            jarOutputStream.write(VALUE);
            jarOutputStream.write(VALUE * 2);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        ClassFileLocator classFileLocator = ClassFileLocator.ForJarFile.of(file, ClassFileVersion.JAVA_V11);
        ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(new byte[]{VALUE, VALUE * 2}));
        ClassFileLocator unresolved = ClassFileLocator.ForJarFile.of(file, ClassFileVersion.JAVA_V9);
        assertThat(unresolved.locate(FOO + "." + BAR).isResolved(), is(false));
        classFileLocator.close();
    }
}
