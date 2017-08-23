package net.bytebuddy.dynamic;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.ClassVisitor;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassFileLocatorForJarFileTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int VALUE = 42;

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private File file;

    @Before
    public void setUp() throws Exception {
        file = File.createTempFile(FOO, BAR);
    }

    @After
    public void tearDown() throws Exception {
        assertThat(file.delete(), is(true));
    }

    @Test
    public void testSuccessfulLocation() throws Exception {
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file));
        try {
            JarEntry jarEntry = new JarEntry(FOO + "/" + BAR + ".class");
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(VALUE);
            jarOutputStream.write(VALUE * 2);
            jarOutputStream.closeEntry();
        } finally {
            jarOutputStream.close();
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
    public void testJarFileClosable() throws Exception {
        JarFile jarFile = mock(JarFile.class);
        Closeable classFileLocator = new ClassFileLocator.ForJarFile(jarFile);
        classFileLocator.close();
        verify(jarFile).close();
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
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file));
        try {
            JarEntry jarEntry = new JarEntry("noop.class");
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(VALUE);
            jarOutputStream.closeEntry();
        } finally {
            jarOutputStream.close();
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
    public void testClose() throws Exception {
        JarFile jarFile = mock(JarFile.class);
        new ClassFileLocator.ForJarFile(jarFile).close();
        verify(jarFile).close();
        verifyNoMoreInteractions(jarFile);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileLocator.ForJarFile.class).apply();
    }
}
